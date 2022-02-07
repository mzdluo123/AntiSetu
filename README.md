# AntiSetu V3

论坛这么多Setu插件就来个反setu插件吧

V3全新升级版，在`danbooru`的十万张图片上进行了训练，但仅对卡通绘画风格的图片有效

使用`MobileNetV3`模型并进行了剪枝量化，即使服务器只有单个CPU也能高速推理，并在验证数据集上得到了80%的准确率

# 使用方式

插件不包括任何指令，配置文件含义如下

```yml
# 启用的群
enabled_group: 
  - 群号
# questionable等级的阈值
questionable_threshold: 0.5
# 同上，explicit等级要比上面的等级更加se
explicit_threshold: 0.5
# 是否撤回
questionable_recall: true
explicit_recall: true
# 回复内容
questionable_reply: '好涩哦~~~ %score%'
explicit_reply: '太涩啦~~~~ %score%'
```

