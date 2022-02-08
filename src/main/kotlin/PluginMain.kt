package win.rainchan.mirai.antisetu

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.PlainText
import kotlin.math.roundToInt


object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "win.rainchan.mirai.antisetuv3",
        name = "AntiStuV3",
        version = "0.0.1"
    ) {
        author("RainChan")
        info("简单的反Setu插件 V2")

    }
) {

    internal lateinit var session: OrtSession

    override fun onEnable() {
        logger.info("加载数据....")
        Config.reload()
//        val imageFolder = File(configFolder,"images")
//        if (!imageFolder.exists()){
//            imageFolder.mkdir()
//        }
        logger.info("初始化运行环境....")
        val model = getResourceAsStream("v3.onnx")?.use { it.readAllBytes() }
        val env = OrtEnvironment.getEnvironment()
        session = env.createSession(model)
        logger.info("初始化成功")
        this.globalEventChannel().subscribeGroupMessages {
            always {
                if (group.id !in Config.enabled_group) {
                    return@always
                }
                val img = message[Image] ?: return@always
                val startTime = System.currentTimeMillis()
                val image = Detector.downloadImg(img)
                val result = Detector.detector(image)
                logger.info("识别完成 ${img.imageId} 分数 $result 耗时:${System.currentTimeMillis() - startTime}ms")
                // group.sendMessage(message.quote()+PlainText(result.toString()))
                if (result.safe >= Config.safe_threshold) {
                    return@always
                }
                if (result.questionable >= Config.questionable_threshold) {
                    if (result.explicit >= Config.explicit_threshold) {

                        group.sendMessage(
                            message.quote() + PlainText(
                                Config.explicit_reply.replace(
                                    "%score%",
                                    result.explicit.toString()
                                )
                            )
                        )
                        if (Config.explicit_recall) {
                            this.message.recall()
                        }
                        return@always
                    } else {
                        group.sendMessage(
                            message.quote() + PlainText(
                                Config.questionable_reply.replace(
                                    "%score%",
                                    result.questionable.toString()
                                )
                            )
                        )
                        if (Config.questionable_recall) {
                            this.message.recall()
                        }
                    }
                }
            }
        }
    }


}
