package win.rainchan.mirai.antisetu

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.*
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import java.io.ByteArrayInputStream
import java.lang.IllegalArgumentException
import javax.imageio.ImageIO


data class DetectResult(
    val drawings: Float,
    val hentai: Float,
    val neutral: Float,
    val porn: Float,
    val sexy: Float
) {
    val isSetu
        get() = hentai > Config.threshold || porn > Config.threshold || sexy > Config.threshold
}

object Detector {
    const val MULTITHREAD_LENGTH = 100 * 1024
    const val DOWNLOAD_PART = 4

    private val client by lazy {
        OkHttpClient()
    }

    suspend fun detector(img: Image): DetectResult {
        val imageContent = downloadImg(img)
        return detector(imageContent)
    }

    suspend fun detector(content: ByteArray): DetectResult {
        val output = withContext(Dispatchers.Default) {
            val scaled = ByteArrayInputStream(content).use {
                scaleImg(ImageIO.read(it))
            }
            val inputArray = arrayOf(imageToMatrix(scaled))
            PluginMain.session.run(
                mapOf(
                    "input" to OnnxTensor.createTensor(
                        OrtEnvironment.getEnvironment(),
                        inputArray
                    )
                )
            )
        }
        return processOutput(output)


    }

    private fun processOutput(result: OrtSession.Result): DetectResult {

        val scoreTensor = result.first().value.value as Array<*>
        val score = scoreTensor[0] as FloatArray
        return DetectResult(score[0], score[1], score[2], score[3], score[4])
    }

    private fun scaleImg(image: BufferedImage): BufferedImage {
        val scaledImg = image.getScaledInstance(224, 224, java.awt.Image.SCALE_FAST)
        val img = BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB)
        img.graphics.drawImage(scaledImg, 0, 0, null)
        return img
    }

    private fun imageToMatrix(image: BufferedImage): Array<Array<FloatArray>> {
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        PixelGrabber(image, 0, 0, width, height, pixels, 0, width).apply {
            this.grabPixels()
        }
        val result = Array(height) { Array(width) { FloatArray(3) } }


        var pixel = 0
        var row = 0
        var col = 0
        while (row * width + col < pixels.size) {
            pixel = row * width + col
            result[row][col][0] = (pixels[pixel] and 0xff) / 255f// blue
            result[row][col][1] = (pixels[pixel] shr 8 and 0xff) / 255f // green
            result[row][col][2] = (pixels[pixel] shr 16 and 0xff) / 255f // red
            col++
            if (col == width - 1) {
                col = 0
                row++
            }
        }
        return result
    }


    suspend fun downloadImg(img: Image): ByteArray {
        val url = img.queryUrl()
        return withContext(Dispatchers.IO) {
            val rsp = client.newCall(Request.Builder().head().url(url).build()).execute()
            if (rsp.isSuccessful) {
                val length =
                    rsp.header("content-length")?.toInt() ?: throw IllegalArgumentException("无法下载图片${img.imageId}")
                if (length >= MULTITHREAD_LENGTH) {
                    val everyPart = length / DOWNLOAD_PART
                    val result = ByteArray(length)
                    val remain = length % DOWNLOAD_PART
                    var point = 0
                    (0..DOWNLOAD_PART).map {
                        if (it == DOWNLOAD_PART && remain != 0) {
                            async(Dispatchers.IO) { downloadPart(url, it * everyPart, length) }
                        } else {
                            async(Dispatchers.IO) { downloadPart(url, it * everyPart, (it + 1) * everyPart - 1) }
                        }
                    }.awaitAll().map {
                        System.arraycopy(it, 0, result, point, it.size)
                        point += it.size
                    }
                    return@withContext result
                } else {
                    val imgRsp = client.newCall(Request.Builder().get().url(url).build()).execute()
                    return@withContext imgRsp.body?.byteStream()?.readAllBytes()
                        ?: throw IllegalArgumentException("无法下载图片${img.imageId}")
                }

            }
            throw IllegalArgumentException("无法下载图片${img.imageId}")
        }
    }

    private suspend fun downloadPart(url: String, start: Int, end: Int, retry: Int = 0): ByteArray {
        //  PluginMain.logger.info("download bytes=${start}-${end}")
        if (retry > 3) {
            throw IllegalArgumentException("无法下载图片${url},bytes=${start}-${end}")
        }
        val rsp = withContext(Dispatchers.IO) {
            val req = Request.Builder().get().url(url).header("Range", "bytes=${start}-${end}").build()
            val rsp = client.newCall(req).execute()
            return@withContext rsp.body?.byteStream()?.readAllBytes()
        }
        if (rsp == null) {
            delay(500)
            downloadPart(url, start, end, retry + 1)
        }

        return rsp!!
    }
}