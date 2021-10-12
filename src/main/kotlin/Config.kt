package win.rainchan.mirai.antisetu

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.internal.data.builtins.AutoLoginConfig

object Config:AutoSavePluginConfig("config") {

    public enum class ProcessType{
        DISABLED,
        RECALL,
        DOWNLOAD_RECALL,
        DOWNLOAD,
        MUTE
    }
    val enabledGroup:MutableMap<Long,ProcessType> by value()

    var threshold:Float by value(0.3f)

}