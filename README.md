# AntiSetu V3

论坛这么多Setu插件就来个反setu插件吧

V3全新升级版，在`danbooru`的十万张图片上进行了训练，但仅对卡通绘画风格的图片有效

使用`MobileNetV3`模型并进行了剪枝量化，即使服务器只有单个CPU也能高速推理，并在验证数据集上得到了80%的准确率

# 使用方式

请先到release下载模型文件

插件不包括任何指令，配置文件含义如下

```yml
# 模型路径
model_path: 
# 启用的群
enabled_group: 
  - 群号
# explicit等级的阈值
explicit_threshold: 0.5
# 是否撤回
explicit_recall: true
# 回复内容
explicit_reply: '太涩啦~~~~ %score%'
```

