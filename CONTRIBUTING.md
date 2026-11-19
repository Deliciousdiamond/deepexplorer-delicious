# 参与深探

谢谢愿意看一眼。这是一个只用 Android 框架 API 的小项目，流程故意保持简单。

## 开始之前

- Bug 和功能建议都走 GitHub Issues。
- 安全问题：**不要公开发 Issue**，按 [SECURITY.md](SECURITY.md) 里的方式报告。
- 永远不要提交：
  - DeepSeek API Key（`sk-...`）
  - 签名密钥（`.jks`、`.keystore`）或密码
  - 本地 SDK（`sdk/`）、构建产物（`build/`、`dist/`）、`.env`
  - 带隐私信息的截图

## 流程

1. Fork 仓库。
2. 开一个分支：

   ```bash
   git checkout -b fix/short-description
   ```

3. 构建并测试：

   ```bash
   export ANDROID_JAR="/path/to/android-29/android.jar"
   export KEYSTORE_PASS="..."
   ./build.sh
   ```

4. 改动尽量聚焦，不要顺手改无关格式。
5. 提交信息写清楚：

   ```text
   fix: 错误页重试后保留原网址
   feat: 给页内查找加键盘快捷键
   docs: 补充 API 29 构建说明
   ```

6. Push 后向 `main` 开 Pull Request。

## 代码风格

- Java 8 语法，不用 AndroidX、不用 Gradle、不用 Kotlin。
- 优先用项目里已经在用的框架 API。
- 动画只动 `alpha` / `translation` / `scale`，不要每帧改布局。
- `app/assets/` 里的 JavaScript 不要写定时器轮询。
- 新的界面文案放 `app/res/values/strings.xml`，Java 里不要硬编码中英文。
- 保持现有的 Nightly 风格：圆角玻璃、克制的配色、不要夸张的装饰元素。
- 如果引入第三方资源/库，把许可证写进 `THIRD_PARTY_NOTICES.md`。项目目前坚持
  零依赖，确实需要新库时先讨论。

## 测试

目前还没有自动化测试。提 PR 之前：

- 用 `./build.sh` 构建通过。
- 装到手机/模拟器（Android 10+）上。
- 检查：
  - 搜索、标签页、前进后退
  - AI 面板与模型切换（要有可用的 Key）
  - 主题/字体切换
  - 书签、页内查找、桌面版网站
  - 断网横幅与网络诊断
  - 本地错误页与重试
  - 首页组件：开关、编辑、天气刷新、格言换句

## PR 检查清单

- [ ] `./build.sh` 能过
- [ ] 没有把 API Key、密钥、SDK 目录或构建产物带进来
- [ ] 行为变了的话，`README.md` / `CHANGELOG.md` 已同步
- [ ] 界面改动附了截图（不含隐私内容）
