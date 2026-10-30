# 上游来源与待核实清单

核对日期：2026-10-30。本表记录仓库证据和缺口，不是已完成的软件物料清单或许可授权证明。

## 基线与本地修改

- 仓库：fdavids77/BlackBoxReborn。
- 基线提交：7b86a9d62a974650e0659825758931cd91140827。
- [LICENSE](../LICENSE) 沿用上游；[上游 README 原文](upstream/README.original.md) 保留原首页，其能力描述、路径和链接属于原项目。
- 原 README 标注 GPL-3.0，LICENSE 正文为 Apache-2.0。保留差异，不据此推定整个项目的统一许可。详见 [风险记录](LEGAL_REVIEW.md)。

本地修改包括独立 manager APP、空间管理与同步、TCP 转发及设置、IP 估算定位、启动恢复与测试。远程日志发送和原有 Integrity token relay 调用已移除。

未参与构建的上游演示 app、旧发布工作流、研究工具和历史开发文档已移到工作区的本地归档。构建模块、代码注释、第三方代码与链接所需静态库保留。本段概述不能替代适用许可证要求的逐文件修改说明。

## 随附组件

| 对象 | 仓库依据 | 核实状态与后续工作 |
| --- | --- | --- |
| BlackBox Reborn 派生引擎 | Bcore/、上述提交、保留的上游文档 | 需澄清许可差异、版权链与本地修改的分发条件 |
| Dobby 静态库及头文件 | Bcore/src/main/cpp/Dobby/；arm64-v8a 与 armeabi-v7a 下均有 libdobby.a | 库文件存在；当前只构建 ARM64。精确源码版本、构建材料、许可证与适用通知尚未核实，不依据名称猜测许可证 |
| xdl 源码 | Bcore/src/main/cpp/xdl/ | 已观察到随附源码；精确来源版本、版权与许可通知尚未核实 |
| 反射支持与注解处理器 | black-reflection/、compiler/ | 随上游取得；需进一步确认原始来源、版权与许可范围 |
| Gradle wrapper | gradle/wrapper/ | 属于构建工具；需纳入工具与再分发材料清单，不能直接视为全部适用根 LICENSE |

仓库中是否存在许可文件、是否保留源文件头部声明、是否满足分发义务是不同问题。本清单未逐文件完成版权审计；缺少单独许可文件的观察不等于证明侵权或通知被删除。

## 构建声明中的外部依赖

以下来自 [Bcore/build.gradle](../Bcore/build.gradle)、[compiler/build.gradle.kts](../compiler/build.gradle.kts) 和 [版本目录](../gradle/libs.versions.toml)。这里是声明版本，不是完整解析后的依赖图，也不保证每个声明都进入最终 APK。

| 声明 | 声明版本 | 用途范围 |
| --- | --- | --- |
| com.google.android.material:material | Bcore 为 1.3.0；版本目录另列 1.12.0 | UI 依赖；需按实际解析结果核对 |
| androidx.appcompat:appcompat | Bcore 为 1.2.0；版本目录另列 1.7.0 | Android 兼容支持；需按实际解析结果核对 |
| com.moandjiezana.toml:toml4j | 0.7.2 | TOML 处理 |
| com.github.tiann:FreeReflection | 3.2.2 | 反射支持 |
| com.google.auto.service:auto-service | 1.1.1 | 注解处理/编译阶段 |
| com.squareup:javapoet | 1.13.0 | 代码生成 |
| androidx.test.ext:junit、androidx.test:runner、androidx.test:rules | 1.2.1、1.6.2、1.6.1 | Bcore 的测试依赖声明 |

许可证、版权通知、传递依赖、构建插件与最终 APK 内容需根据实际取得的制品另行核实；本表不向这些组件分配未经验证的许可证。正式分发材料应能对应到实际构建的版本与内容，并满足各适用许可要求。
