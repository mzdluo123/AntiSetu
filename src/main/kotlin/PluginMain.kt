package win.rainchan.mirai.antisetu

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.getOrFail


object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "win.rainchan.mirai.antisetu",
        name = "AntiStu",
        version = "0.1.0"
    ) {
        author("RainChan")
        info("简单的反Setu插件")

    }
) {

    internal lateinit var session: OrtSession

    override fun onEnable() {
        logger.info("初始化运行环境....")
        val model=getResourceAsStream("model.onnx")?.use { it.readAllBytes() }
        val env = OrtEnvironment.getEnvironment()
        session = env.createSession(model)
        logger.info("初始化成功")
        GlobalEventChannel.subscribeGroupMessages {
            always {
               val img = message[Image] ?: return@always
                val startTime = System.currentTimeMillis()
                val score = Detector.detector(img)
                logger.info("识别完成 ${img.imageId} 分数 $score 耗时:${System.currentTimeMillis() - startTime}ms")

            }
        }
    }


}
