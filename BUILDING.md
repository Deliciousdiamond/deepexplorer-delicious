# 构建深探

深探故意不用 Gradle，APK 直接用 Android SDK 命令行工具拼出来。这样构建体积小、
可复现，也不需要装 Android Studio。

## 需要什么

| 工具 | 用途 |
|---|---|
| JDK 8+（`javac`、`keytool`） | 编译 Java、生成签名密钥 |
| Android SDK Build-Tools | `aapt`、`d8`、`zipalign`、`apksigner` |
| Android Platform 29 的 `android.jar` | 编译/运行类路径（`targetSdk 29`） |
| `zip` | 把 `classes.dex` 塞进 APK |
| `bash` | 运行 `build.sh` |

仓库**不包含** Android SDK 和签名密钥。`build/`、`sdk/`、`*.jks`、`*.keystore`、
`keystore/`、`.env*` 都已被 Git 忽略，请保持这样。

## 1. 安装构建工具

### Linux / macOS

装好 JDK 和 Android 命令行 build-tools，确保这些命令在 `PATH` 里：

```bash
aapt d8 zipalign apksigner javac keytool zip
```

SDK 目录通常长这样：

```text
~/Android/Sdk/
├── build-tools/35.0.0/
└── platforms/android-29/android.jar
```

### Android / Termux

在 Termux 里装 JDK、`zip` 和 Android build-tools，或者直接用现成的 SDK，然后把
`ANDROID_JAR` 指向平台 jar。构建脚本就是普通 bash，手机上也能跑。

## 2. 生成签名密钥（只需一次）

**生成的密钥不要提交。** 它决定了 App 以后能不能覆盖升级。

```bash
export KEYSTORE_PASS='换成你自己的强密码'
./scripts/generate-keystore.sh
```

会生成 `keystore/release.jks`（已被 Git 忽略），别名 `release`。

如果已经有一把密钥，直接用它：

```bash
export KEYSTORE="/path/to/your.jks"
export KEY_ALIAS="your-alias"
export KEYSTORE_PASS="..."
export KEY_PASS="$KEYSTORE_PASS"
```

## 3. 构建

```bash
export ANDROID_JAR="$HOME/Android/Sdk/platforms/android-29/android.jar"
export KEYSTORE="$PWD/keystore/release.jks"
export KEY_ALIAS="release"
export KEYSTORE_PASS="换成你自己的强密码"

./build.sh
```

产物：

```text
build/DeepExplorer.apk
```

构建完想顺手复制到别处：

```bash
COPY_TO="$HOME/DeepExplorer.apk" ./build.sh
```

## 环境变量

| 变量 | 必填 | 默认值 |
|---|---|---|
| `KEYSTORE_PASS` | 是 | — |
| `ANDROID_JAR` | 建议填 | `$ANDROID_HOME/platforms/android-29/android.jar` |
| `KEYSTORE` | 否 | `keystore/release.jks` |
| `KEY_ALIAS` | 否 | `release` |
| `KEY_PASS` | 否 | 与 `KEYSTORE_PASS` 相同 |
| `OUT` | 否 | `build/DeepExplorer.apk` |
| `COPY_TO` | 否 | 不复制 |

## 4. 安装

```bash
adb install -r build/DeepExplorer.apk
```

或者把 APK 拷到手机上打开（给文件管理器/浏览器开「安装未知应用」权限）。

APK 是用你自己的密钥签的。如果手机里已经装了另一个签名版本的深探，必须先卸载旧版，
否则 Android 会拒绝更新。

## 常见报错

### `android.jar not found`

显式指定：

```bash
export ANDROID_JAR="/path/to/Android/Sdk/platforms/android-29/android.jar"
```

### `aapt: command not found`

把 build-tools 目录加进 `PATH`：

```bash
export PATH="$HOME/Android/Sdk/build-tools/35.0.0:$PATH"
```

### `Failed to load signer` / `keystore not found`

检查 `KEYSTORE` 是否指向 `.jks` 文件、`KEYSTORE_PASS` 是否正确。不要把密码写进
`build.sh`，也不要提交。

### `java.lang.UnsupportedClassVersionError`

`javac`/`d8` 和 JDK 版本要兼容。用当前版本的 JDK（17 没问题）和 build-tools。

### 装上了但一打开就闪退

看 logcat：

```bash
adb logcat | grep -E 'AndroidRuntime|DeepExplorer'
```

提 Bug 时带上崩溃堆栈和 Android/WebView 版本，但先把 API Key、私人网址去掉。

## 为什么不用 Gradle？

项目只用 Android 框架 API，没有 AndroidX、没有 Kotlin、没有第三方资源。五步
`aapt`/`javac`/`d8` 流水线就够了，仓库也更好读。如果以后要加 AndroidX 或 Material
Components，应该整体迁到 Gradle，而不是手工塞 AAR。
