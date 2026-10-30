# 多开空间

Android 应用多实例实验项目，基于 BlackBox Reborn 的本地修改版，目前适配用户自行安装的 WhatsApp。

**版本：0.3.3-test · versionCode 7。** 包名：`com.yshut.whatsappspaces`。当前为兼容性测试构建。

本项目为独立维护的第三方衍生项目，并非 WhatsApp 或 Meta 的官方产品；当前未提供来自 WhatsApp 或 Meta 的授权、合作或认可证明。文中第三方名称用于说明适配对象与来源，其商标和其他权利归相应权利人所有。

> **许可状态待核实：** 上游 README 与 LICENSE 的许可标注不一致，部分随附组件的来源与许可清单尚未补齐。公开源码、构建成功或“实验用途”说明均不代表已完成授权核验。详见 [许可与发布风险记录](docs/LEGAL_REVIEW.md)。

## 当前功能

| 功能 | 当前实现与边界 |
| --- | --- |
| 空间管理 | 新建、打开、重命名、删除空间，添加桌面快捷方式；删除会清除对应空间本地数据 |
| 启动恢复 | 重启所选空间进程，显示准备阶段和页面打开回执；不保证全部第三方应用行为正常 |
| 更新同步 | 在手机上安装的 WhatsApp 更新后，支持手动同步和后台定期检查并同步到空间；不提供其安装包下载或静默安装服务 |
| 网络配置 | 用户配置的 VPN，或 VPN → 用户配置的住宅代理；目前两种模式均要求 VPN 可用 |
| 配置保存 | 代理凭据使用 Android Keystore 配合 AES-GCM 在设备本地保存；关闭住宅模式仍会保留凭据 |
| IP 估算定位 | 可将出口 IP 的估算坐标应用到分身定位接口；这会改变应用收到的位置，不代表设备真实位置 |

未实现登录验证码自动读取、统一聊天消息中心或批量消息发送功能。分身运行所需的第三方应用由用户自行取得，本仓库不附带 WhatsApp 安装包、账号、聊天记录或可用代理凭据。

## 使用前须知

- 在有权操作的设备、账号与数据范围内使用，并核对第三方软件许可、服务条款及所在地适用要求。技术上能够导入或运行，不代表已经取得相应许可。
- 不得将项目用于未授权访问、冒充他人、未经同意处理消息或位置数据、骚扰或垃圾消息。网络与定位功能不提供规避平台规则或法律要求的授权。
- WhatsApp 的条款对未获允许的使用、代码提取或修改等行为设有限制，并规定账户限制措施；本项目不承诺官方兼容、账号不受限制或长期服务可用。[官方条款及核对范围](docs/LEGAL_REVIEW.md#官方资料)
- “实验项目”等描述不构成责任豁免，也不修改任何适用开源许可证已授予的权利或第三方条款。维护者仍需完成组件来源、许可义务和实际分发方式的核验。

## 数据流与安全边界

- 开启出口检测时会访问 `api.ipify.org`，必要时访问 Cloudflare 的 trace 接口；开启 IP 定位时会访问 `ipwho.is`。这些第三方会收到相应网络请求并可看到请求出口 IP。
- 住宅代理的认证需要向用户选择的代理服务商发送凭据；本地加密保存不代表代理服务商无法获得认证信息，也不等于所有传输环节均额外加密。
- 上次查询的出口 IP、估算位置和查询时间会保存在设备上。关闭住宅代理或定位选项不会自动删除此前保存的配置或位置记录。
- 各空间使用不同的数据目录，但共享宿主的系统 UID，不是 Android 系统用户级隔离。当前 TCP 转发未覆盖全部网络路径，不提供匿名性或“零泄漏”保证。
- 这份说明不是面向所有运营方式的完整隐私政策。向其他用户提供安装包或服务前，还需明确实际运营主体、权限用途、保留与删除规则及适用的数据处理要求。

具体实现与未完成事项见 [数据与权限说明](docs/PRIVACY.md)。请勿在公开 Issue 上传代理密码、验证码、手机号或聊天内容。

## 兼容性与验证

安装清单最低 Android 9（API 28），只构建 arm64-v8a。已在 vivo V2408A / Android 16 上进行有限测试，其他设备尚未验证。

0.3.3 的启动回归测试及五次真机页面打开确认已通过，包括往返切换和整个宿主进程停止后的恢复。长期后台消息、熄屏恢复、语音/视频及全部定位接口未完成验证，见 [测试记录](docs/TESTING.md)。这些结果不构成对其他环境的运行保证。

## 快速构建

环境：JDK 17、Android SDK 36、Build Tools 36.0.0、NDK 27.1.12297006；Gradle wrapper 8.14.5。

Windows：

~~~powershell
$env:JAVA_HOME = '你的 JDK 17 目录'
$env:ANDROID_SDK_ROOT = '你的 Android SDK 目录'
.\build-app.ps1 -WithTests
~~~

Linux / macOS（已配置 JAVA_HOME 与 Android SDK 环境变量）：

~~~sh
bash ./gradlew :manager:assembleDebug :manager:assembleDebugAndroidTest --no-daemon
~~~

主 APK：manager/build/outputs/apk/debug/manager-debug.apk。
测试 APK：manager/build/outputs/apk/androidTest/debug/manager-debug-androidTest.apk。

GitHub Actions 构建这两个 APK 并上传为工作流产物，不自动发布 Release。编译测试包不等于在真实设备上运行测试。

## 项目结构

~~~text
manager/                  APP 界面、空间管理、网络和定位、真机测试
Bcore/                    分身引擎与 native 修改
black-reflection/         反射支持
compiler/                 注解处理器
gradle/                   版本目录与 Gradle wrapper
.github/                  构建工作流与问题模板
build-app.ps1             通用 Windows 构建入口
tools/check-repository.mjs 上传前文件检查
tools/export-source.ps1   无 Git 历史的源码导出
docs/                     使用、开发、测试和上传说明
~~~

## 文档

- [使用说明](docs/USAGE.md)
- [开发与构建](docs/DEVELOPMENT.md)
- [测试记录与边界](docs/TESTING.md)
- [数据与权限说明](docs/PRIVACY.md)
- [许可与发布风险记录](docs/LEGAL_REVIEW.md)
- [上传到 GitHub](docs/GITHUB.md)
- [上游来源](docs/UPSTREAM.md)
- [变更记录](CHANGELOG.md)

## 上游与许可状态

引擎基于 `fdavids77/BlackBoxReborn`，基线提交 `7b86a9d62a974650e0659825758931cd91140827`。保留 [上游 README 原文](docs/upstream/README.original.md) 和 [LICENSE](LICENSE)。上游 README 标为 GPL-3.0，而 LICENSE 正文为 Apache-2.0；现有材料不足以据此确认整个派生项目及所有依赖的统一许可范围。

[来源清单](docs/UPSTREAM.md) 将已观察到的材料和待补齐事项分开记录。本次文档调整没有改写任何已有许可文本，也没有宣称获得新的第三方授权。正式分发、收费或再许可前，应针对实际组件和版本完成许可核验；具体法律结论需结合适用法域与分发方式由合资格专业人士判断。
