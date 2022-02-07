package win.rainchan.mirai.antisetu

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.internal.data.builtins.AutoLoginConfig

object Config:AutoSavePluginConfig("config") {
    val enabled_group:MutableList<Long> by value(mutableListOf())
    var questionable_threshold:Float by value(0.5f)
    var explicit_threshold:Float by value(0.5f)

    var questionable_recall:Boolean by value(false)
    var explicit_recall:Boolean by value(false)

    var questionable_reply:String by value("好涩哦~~~ %score%")
    var explicit_reply:String by value("太涩啦~~~~ %score%")
}
