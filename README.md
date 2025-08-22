# McMotd                                                                                                               |

## 独立运行
McMotd支持脱离mirai-console独立运行，可以用于测试或对接其他框架。如需要独立运行McMotd，请下载[release](https://github.com/Under-estimate/McMotd/releases/)
中后缀为`.mirai.jar`的插件版本（其中包含独立运行所需的完整依赖项），并根据需求选择相应的启动命令（见下方）。
独立运行时，会在当前目录下生成配置文件`mcmotd.yml`和历史人数记录数据文件`mcmotd_data.yml`。
- 仅提供HTTP API访问功能
```bash
java -cp mcmotd-x.x.x.mirai.jar org.zrnq.mcmotd.StandaloneMainKt
```
- 仅提供图形化界面访问（用于测试，不启用历史人数记录功能）
```bash
java -cp mcmotd-x.x.x.mirai.jar org.zrnq.mcmotd.GUIMainKt
```
## HTTP API
要开启插件的HTTP API功能，需要将配置文件中的`httpServerPort`设置为非零的可用端口，并配置`httpServerMapping`。  
示例配置：
> httpServerPort: 8092  
> httpServerMapping:   

以上述配置启动McMotd后，访问`http://localhost:8092/info/hypixel` 将会返回与`/mcp hypixel.net`相同的图片结果；访问`http://localhost:8092/info/earthmc` 将会返回与`/mcp org.earthmc.net`相同的图片结果。访问配置文件中未定义的服务器名（如`http://localhost:8092/info/foo` ）将不会返回有效的结果

### Q: 访问Http API有时会返回Too Many Requests
A: 插件自`1.1.17`版本起，默认启用了Http API的访问冷却时间限制，您可以通过修改配置文件来调整冷却时间长度
