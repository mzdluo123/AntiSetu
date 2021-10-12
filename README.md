# AntiSetu

论坛这么多Setu插件就来个反setu插件吧

基于[open_nsfw_android](https://github.com/devzwy/open_nsfw_android) 项目的setu识别器，使用`onnxruntime`在PC平台上运行

支持`amd64`的`Windows` `Linux` `Macos`平台，可快速离线推理

# 使用方式
```
/asetu [群号] [模式]
 0      DISABLED,
 1      RECALL,
 2      DOWNLOAD_RECALL,
 3      DOWNLOAD,
 4      MUTE

禁言默认一分钟，配置文件可调阈值
```

