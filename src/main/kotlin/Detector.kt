package win.rainchan.mirai.antisetu

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.*
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO


data class DetectResult(
    val safe: Float,
    val explicit: Float,

)
object Detector {
    const val MULTITHREAD_LENGTH = 100 * 1024
    const val DOWNLOAD_PART = 4
    const val INPUT_SIZE = 512

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
                pad(ImageIO.read(it), INPUT_SIZE.toDouble(),INPUT_SIZE.toDouble(), Color.BLACK)
            }
            val inputArray = arrayOf(imageToMatrix(scaled))
            PluginMain.session.run(
                mapOf(
                    "serving_default_input_1:0" to OnnxTensor.createTensor(
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
        return DetectResult(score[0], score[1])
    }

    fun pad(image: BufferedImage, width: Double, height: Double, pad: Color): BufferedImage {
        val ratioW = image.width / width
        val ratioH = image.height / height
        var newWidth = width
        var newHeight = height
        var fitW = 0
        var fitH = 0

        //padding width
        if (ratioW < ratioH) {
            newWidth = image.width / ratioH
            newHeight = image.height / ratioH
            fitW = ((width - newWidth) / 2.0).toInt()
        } //padding height
        else if (ratioH < ratioW) {
            newWidth = image.width / ratioW
            newHeight = image.height / ratioW
            fitH = ((height - newHeight) / 2.0).toInt()
        }
        val resize: java.awt.Image =
            image.getScaledInstance(newWidth.toInt(), newHeight.toInt(), java.awt.Image.SCALE_SMOOTH)
        val resultImage = BufferedImage(width.toInt(), height.toInt(), image.type)
        val g = resultImage.graphics
        g.color = pad
        g.fillRect(0, 0, width.toInt(), height.toInt())
        g.drawImage(resize, fitW, fitH, null)
        g.dispose()
        return resultImage
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
                    return@withContext imgRsp.body?.byteStream()?.use {
                        it.readAllBytes()
                    } ?: throw IllegalArgumentException("无法下载图片${img.imageId}")
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
            return@withContext rsp.body?.byteStream()?.use {
                it.readAllBytes()
            }
        }
        if (rsp == null) {
            delay(500)
            downloadPart(url, start, end, retry + 1)
        }

        return rsp!!
    }
}
