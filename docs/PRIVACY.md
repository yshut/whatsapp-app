# 数据与权限说明

核对日期：2026-10-30。对象：0.3.3-test 的当前源码。此文描述已观察到的实现，不是完整隐私政策或安全审计结论。

## 已观察到的数据流

| 场景 | 数据与接收方 | 本地保存 |
| --- | --- | --- |
| 住宅代理设置 | 地址、端口、用户名、密码、协议由用户输入；连接时向所选代理服务商进行认证 | ResidentialConfig 使用 Android Keystore 和 AES-GCM 保存于 no_backup 目录；不是全部应用数据的统一加密 |
| 出口检测 | 向 api.ipify.org 请求；失败时可能访问 www.cloudflare.com/cdn-cgi/trace；对方可见出口 IP | 检测结果可作为界面状态显示，不保证绝不留在运行时记录中 |
| IP 估算定位 | 向 ipwho.is 请求出口位置；对方可见出口 IP | IpLocation.remember 保存 IP、国家/地区/城市、坐标与查询时间，并写入分身位置配置 |
| 空间运行 | 宿主导入本机第三方应用并承载其进程、权限与数据访问 | 不同空间使用不同目录，但共享宿主 UID；不是系统用户级隔离 |
| 用户反馈 | 用户自行提交到 GitHub 的日志、截图或文字 | 由提交方式与 GitHub 处理规则决定，提交前应删除敏感信息 |

开关关闭不会删除已保存的代理凭据或位置记录。当前界面未提供完整的逐项数据导出、保留期限配置和清除流程；不能将“关闭功能”描述成“删除相关数据”。

## 传输与权限

- 凭据的本地加密只解决保存形式。代理认证必须交给代理服务商；SOCKS5 与 HTTP CONNECT 路径没有自动获得 HTTPS CONNECT 的额外 TLS 保护。应结合 VPN、代理协议及各段链路评估传输风险。
- 清单声明了联系人、相机、麦克风、电话状态、存储等权限。清单声明不等于已经授予或实际使用，但运行第三方应用时的数据访问需要结合系统授权和虚拟化实现评估。
- 本地修改已移除 LogSender 的远程发送实现；这不能推导出整个引擎、依赖或第三方应用完全不联网、不记录数据或不存在其他数据处理。
- 网络检测和位置查询会访问第三方网络服务，不应宣传为完全离线、绝不向外部发送信息、绝对匿名或零泄漏。

## 面向其他用户发布前仍需补齐

1. 明确实际维护/运营主体、联系方式、处理目的与适用地域。
2. 根据真实安装包逐项核对权限、接收方、必要性、保留与删除方式，并形成与实现一致的告知文本。
3. 核对 VPN、代理和 IP 查询服务商的条款、数据处理方式及所需的供应商安排；当前未完成这项评估。
4. 若后续增加通知汇总或验证码功能，应另行评估消息、联系人、验证码、授权交互及传输，不沿用本页作为已完成评估。

以上是待核验事项，不是对任何司法辖区下已经合规的结论，也不能代替实际需要的用户告知与授权。

## 源码依据

- [ResidentialConfig.java](../manager/src/main/java/com/yshut/whatsappspaces/ResidentialConfig.java)
- [ResidentialTransport.java](../manager/src/main/java/com/yshut/whatsappspaces/ResidentialTransport.java)
- [IpLocation.java](../manager/src/main/java/com/yshut/whatsappspaces/IpLocation.java)
- [NetworkOptions.java](../manager/src/main/java/com/yshut/whatsappspaces/NetworkOptions.java)
- [AndroidManifest.xml](../Bcore/src/main/AndroidManifest.xml)
- [LogSender.java](../Bcore/src/main/java/top/niunaijun/blackbox/utils/LogSender.java)
