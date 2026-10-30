# 开发与构建

| 组件 | 配置 |
| --- | --- |
| JDK | 17 |
| Gradle | 8.14.5 |
| Android Gradle Plugin | 8.13.2 |
| compileSdk / Build Tools | 36 / 36.0.0 |
| NDK | 27.1.12297006 |
| APP minSdk / targetSdk | 28 / 28 |
| ABI | arm64-v8a |

APP 版本号以 manager/build.gradle 为准。根 build.gradle 的版本字段属于引擎。首次构建需下载依赖。

Windows 使用 build-app.ps1，读取 JAVA_HOME、ANDROID_SDK_ROOT 或 ANDROID_HOME。也可传 -SdkRoot、-JavaHome 和可选的 -GradleHome。加 -WithTests 同时编译测试包。

Linux / macOS 使用 bash ./gradlew :manager:assembleDebug :manager:assembleDebugAndroidTest。Android Studio 直接打开根目录；本机 SDK 路径仅放在环境变量或 local.properties。

## 签名

当前构建为 debug，使用各构建环境自己的调试签名。CI 或另一电脑生成的 APK 不保证能覆盖手机上的现有包。本仓库未配置正式签名和自动发布，不含 keystore 或密码；稳定发行版需要持续使用同一发行签名。

## 代码入口

manager/src/main/java/com/yshut/whatsappspaces 下的 MainActivity 是首页与启动流程，SpacesController 管理空间与同步，ResidentialService 与 NetworkSettingsActivity 管理转发及设置，IpLocation 处理 IP 位置。SpacesApplication 接入引擎生命周期。native TCP 转发位于 Bcore/src/main/cpp/Hook/SocketGate.cpp。

## 上传前检查与导出

安装 Git 和 Node.js 22+，运行 node tools/check-repository.mjs。检查当前可提交文件中的本机路径、敏感文件名、常见凭据模式和构建产物；只输出文件名、行号和规则名，不输出匹配内容。这不是完整密钥审计，也不检查 Git 历史。

Windows 运行 tools/export-source.ps1 -OutputPath 指定一个不存在的 ZIP 文件，导出检查通过的当前源码，不带 .git 历史。
