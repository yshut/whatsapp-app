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
