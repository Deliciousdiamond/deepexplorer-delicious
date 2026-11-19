# 安全策略

## 支持的版本

安全修复只针对当前的 `1.9.x` 线，更早的版本不再维护。

## 报告漏洞

**不要为安全问题开公开 Issue。**

请用 GitHub 的私密漏洞报告：

1. 打开仓库的 **Security** 标签页。
2. 选择 **Report a vulnerability**。
3. 写清楚：
   - 受影响的版本
   - Android / WebView 版本
   - 复现步骤
   - 影响范围
   - 如果安全的话，附一个最小 PoC

不要附带真实的 API Key、密码、私人浏览网址或签名材料。如果密钥泄露，请立刻吊销并
更换。

## 仓库里的敏感信息

- DeepSeek API Key 由用户在运行时输入，只存在本机 `SharedPreferences`，仓库里没有。
- 发布签名密钥被 `.gitignore` 故意排除，必须留在本地。签名密钥泄露会让后续更新
  被人冒名顶替。
- `.env`、`keystore/`、`*.jks`、`*.keystore` 都已被忽略。

## 范围

这是一个本地 Android 浏览器壳。通常算在范围内的：

- 意外的网络请求或数据泄漏
- `addJavascriptInterface` 超出了内置本地页面的使用范围
- 读取了壁纸之外的本地文件 / content URI
- 恶意页面导致 WebView 崩溃的拒绝服务（请附可复现网址，不要未经允许拿第三方网站测试）

不在范围内：用户系统 WebView、DeepSeek API、具体网站自身的漏洞，以及社会工程。
