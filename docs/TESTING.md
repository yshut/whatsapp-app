# 测试记录与边界

## 0.3.3-test / 2026-10-30

- 主包与 AndroidTest 编译成功，手机系统确认 versionCode 7。
- startup-recovery 全部 PASS：旧/空服务意图、失败缓存重置、真实包信息、失效任务记录和已有空间列表保持。
- 两个现有空间共五次页面打开确认：首次打开、往返切换、停止整个宿主进程后分别重新打开。
- 本轮未观察到新的启动崩溃或确认超时，依据为 Activity resumed 回执。

## 在自己的设备复测

以下回归测试不删除现有空间；先确认所选设备以及 APK 签名：

~~~sh
adb -s YOUR_DEVICE_SERIAL install -r manager/build/outputs/apk/debug/manager-debug.apk
adb -s YOUR_DEVICE_SERIAL install -r -t manager/build/outputs/apk/androidTest/debug/manager-debug-androidTest.apk
adb -s YOUR_DEVICE_SERIAL shell am instrument --user 0 -w -r -e action startup-recovery com.yshut.whatsappspaces.test/com.yshut.whatsappspaces.SpacesProbe
~~~

runner 另有改变配置或测试空间的动作，使用前阅读 manager/src/androidTest 下实现，不对真实账号空间执行创建/删除测试。

## 未覆盖

长期后台、熄屏、真实断网恢复、账号登录、聊天、语音/视频、全部定位回调及真实跨版本更新后的登录态保持均未完成验证。此前 SIGKILL 来源未知。本轮未点击新增重启菜单，进程恢复通过停止整个宿主测试。

不宣称任意机型兼容或全部网络路径均受控。GitHub Actions 只检查源码和编译，不运行真机测试。
