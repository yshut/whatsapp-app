# 多开空间 · WhatsApp Multi Space

Android WhatsApp 分身管理 APP，基于 BlackBox Reborn 的本地修改版。

**当前版本：0.3.3-test · versionCode 7。** 包名：com.yshut.whatsappspaces。当前为测试构建，尚未作为应用商店发行版发布。

## 功能

- 新建、打开、重命名和删除空间，添加桌面快捷方式。
- 重启单个空间进程并保留本地数据。
- 手机原版 WhatsApp 更新后同步代码到各空间，支持手动同步和后台定期检查。
- 仅 VPN 或 VPN → 住宅代理；住宅凭据在设备上加密保存。
- 可选 IP 估算定位、启动进度、实际页面打开确认及失败恢复提示。

未实现登录验证码自动读取和统一聊天消息中心。更新同步不包含下载或静默安装手机原版 WhatsApp。

## 使用条件与验证范围

安装清单最低 Android 9（API 28），当前只构建 arm64-v8a。已在 vivo V2408A / Android 16 上完成有限测试，其他机型和系统版本尚未验证。

需要保留手机原版 WhatsApp，并连接用户自行配置的 VPN。目前两种网络模式均要求 VPN 可用；住宅代理是可选的第二跳。源码和安装包没有预设用户的住宅账号或密码。

启动回归测试与五次真机页面打开确认已通过，包括空间间切换及整个宿主进程停止后的重新打开。长期后台消息、熄屏恢复、语音/视频和全部定位接口仍未完成验证，见 [测试记录](docs/TESTING.md)。

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
- [上传到 GitHub](docs/GITHUB.md)
- [上游来源](docs/UPSTREAM.md)
- [变更记录](CHANGELOG.md)

## 上游与许可证

引擎基于 fdavids77/BlackBoxReborn，基线提交 7b86a9d62a974650e0659825758931cd91140827。保留 [上游 README 原文](docs/upstream/README.original.md) 和根目录 [LICENSE](LICENSE)。上游 README 标为 GPL-3.0，而 LICENSE 文件内容为 Apache-2.0；这里如实保留差异，没有重新声明项目为统一的其他许可证。
