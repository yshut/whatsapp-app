# 上传到 GitHub

## 从源码快照建立新仓库

解压源码 ZIP 到新目录，保留 .github、Gradle wrapper JAR、libdobby.a、LICENSE 和来源说明。不要在原开发目录重复初始化。

~~~sh
git init -b main
git add .
git diff --cached --stat
git commit -m "Initial WhatsApp Multi Space source"
git remote add origin YOUR_GITHUB_REPOSITORY_URL
git push -u origin main
~~~

替换为你新建的空仓库地址。源码快照不含 .git 旧历史；APK 已被忽略，可在需要分发时另附到 Release。

## 沿用现有 Git 历史

原开发目录从上游克隆，整理没有修改远程地址。先执行 git remote -v，确认推送目标为自己的仓库。可将原 origin 改名为 upstream，再添加自己的 origin。

上传前运行 node tools/check-repository.mjs，并检查 git status 和 git diff；检查脚本不扫描旧提交历史。

## 自动构建

.github/workflows/build.yml 支持 push、pull_request 与手动触发，构建 manager 主包和测试包并上传 Actions artifacts。未配置自动 Release 或正式签名。

本机测试脚本、原始测试笔记、构建缓存、APK、local.properties、住宅配置与签名私钥不在源码快照中。

## 发布可直接下载的安装包

当前测试版本：[v0.3.3-test](https://github.com/yshut/whatsapp-app/releases/tag/v0.3.3-test)。APK 存放在 Release 的 Assets 附件中，README 已提供直接下载入口。公开 Release 附件可通过链接下载，无需从 Actions 取构建产物。

后续更新可在仓库的 Releases 页面选择 Draft a new release，创建对应版本标签并选择源码提交，填写版本说明，把 APK 和 SHA256SUMS.txt 拖到附件区域。测试构建勾选 Set as a pre-release，检查附件后再 Publish release。不要上传测试 instrumentation APK、签名私钥、账号或代理配置。

上传前核对包名、versionCode、版本、架构、签名与源码对应关系；同一应用后续升级需保持签名连续性。新版本使用新标签和文件名，避免覆盖已经发布的附件。发布后匿名下载核对 SHA-256，并更新 README 的固定版本下载链接。

当前为人工发布；自动构建仍只生成 Actions artifacts。CI 的调试签名不保证与此 Release 一致，不能直接把每次 CI 输出当作可覆盖升级的发行包。许可核实、数据说明及已知测试限制应随版本说明保留。
