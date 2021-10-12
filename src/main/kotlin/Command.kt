package win.rainchan.mirai.antisetu

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.isOperator


object Command : SimpleCommand(
    owner = PluginMain,
    "asetu",
    description = "AntiSetu设置"
) {
    @Handler
    suspend fun CommandSender.onCommand(group: Group?, mode: Int) {
        var groupId = group?.id
        if (group == null && this is MemberCommandSender){
            if (this.user.isOperator()){
                groupId = this.group.id
            }else{
                sendMessage("权限不足")
                return
            }
        }
        if (groupId == null){
            sendMessage("请输入群号")
            return
        }
        val processType = Config.ProcessType.values().getOrNull(mode)
        if (processType == null){
            sendMessage("模式不存在")
            return
        }
        Config.enabledGroup[groupId] = processType
        sendMessage("成功将处理模式设置为${processType.name}")
    }
}