package win.rainchan.mirai.antisetu

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.at
import net.mamoe.mirai.message.data.getOrFail
import java.io.File


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
        logger.info("加载数据....")
        Config.reload()
        val imageFolder = File(configFolder,"images")
        if (!imageFolder.exists()){
            imageFolder.mkdir()
        }
        Command.register()
        logger.info("初始化运行环境....")
        val model=getResourceAsStream("model.onnx")?.use { it.readAllBytes() }
        val env = OrtEnvironment.getEnvironment()
        session = env.createSession(model)
        logger.info("初始化成功")
        GlobalEventChannel.subscribeGroupMessages {
            always {
                val processType = Config.enabledGroup[group.id] ?: Config.ProcessType.DISABLED
                if (processType == Config.ProcessType.DISABLED){
                    return@always
                }

               val img = message[Image] ?: return@always
                val startTime = System.currentTimeMillis()
                val image = Detector.downloadImg(img)
                val score = Detector.detector(image)
                logger.info("识别完成 ${img.imageId} 分数 $score 耗时:${System.currentTimeMillis() - startTime}ms")
                if (score >= Config.threshold){
                    group.sendMessage(sender.at() + PlainText("hso!😲，分数${score}"))
                     when(processType){
                         Config.ProcessType.RECALL -> {
                             message.recall()
                         }
                         Config.ProcessType.DOWNLOAD->{
                             val file = File(imageFolder,img.imageId)
                            withContext(Dispatchers.IO){
                                file.outputStream().use {
                                    it.write(image)
                                }
                            }
                         }
                         Config.ProcessType.DOWNLOAD_RECALL->{
                             message.recall()
                             val file = File(imageFolder,img.imageId)
                             withContext(Dispatchers.IO){
                                 file.outputStream().use {
                                     it.write(image)
                                 }
                             }
                         }
                         Config.ProcessType.MUTE->{
                             sender.mute(60)
                         }

                         else -> return@always
                     }
                }

            }
        }
    }




}
