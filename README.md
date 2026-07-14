# 文件批量重命名 (FileRenamer)

一个基于 Android 16 的现代化文件批量重命名工具，采用液态玻璃（Glassmorphism）主题设计。

## ✨ 功能特点

- **📂 浏览文件** — 通过系统文件选择器浏览任意目录（内部存储、U盘、SD卡）
- **✅ 批量选择** — 逐项勾选或一键全选/取消全选
- **✏️ 四种重命名操作**：
  - **添加前缀** — 在文件名前添加指定文本
  - **添加后缀** — 在文件名后（扩展名前）添加指定文本
  - **删除前N个字符** — 删除文件名开头的N个字符
  - **删除后N个字符** — 删除文件名末尾的N个字符（保留扩展名）
- **🎨 液态玻璃主题** — 毛玻璃效果、半透明渐变、圆角设计
- **🌓 深浅色主题** — 跟随系统自动切换
- **🖼️ 壁纸取色** — 从桌面壁纸提取主色调，动态调整主题色
- **📱 自适应图标** — 液态玻璃风格的应用图标

## 📸 截图

| 初始页面 | 文件列表 | 重命名对话框 |
|---------|---------|------------|
| 选择文件夹入口 | 浏览并勾选文件 | 选择操作类型 |

## 🛠️ 技术栈

| 技术 | 版本 |
|------|------|
| **Android API** | 36 (Android 16) |
| **Kotlin** | 2.1.0 |
| **Compose BOM** | 2024.12.01 |
| **Material 3** | 最新 |
| **Gradle** | 9.6.1 |
| **AGP** | 8.7.3 |
| **架构** | MVVM (ViewModel + StateFlow) |
| **文件访问** | Storage Access Framework (SAF) |

## 📁 项目结构

```
app/src/main/java/com/filerenamer/
├── MainActivity.kt              # 入口 Activity
├── data/
│   ├── FileItem.kt              # 文件/目录数据模型
│   └── RenameOperation.kt       # 重命名操作类型
├── ui/
│   ├── components/
│   │   └── GlassComponents.kt   # 液态玻璃风格UI组件
│   ├── screens/
│   │   ├── MainScreen.kt        # 主界面
│   │   └── MainViewModel.kt     # 状态管理
│   └── theme/
│       ├── Color.kt             # 颜色定义
│       └── Theme.kt             # 主题引擎（壁纸取色）
└── util/
    └── FileUtils.kt             # 文件操作工具
```

## 🚀 构建与安装

### 环境要求

- Android SDK (API 36)
- JDK 17+
- Gradle 9.6.1+

### 构建步骤

```bash
# 1. 设置 Android SDK 路径
export ANDROID_HOME=/path/to/android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. 构建 Release APK（已签名）
./gradlew assembleRelease

# 4. APK 输出位置
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

### Termux 环境构建

在 Android Termux 中构建时，需要安装 `aapt2` 包：

```bash
pkg install aapt2 gradle
```

项目已配置 `android.aapt2FromMavenOverride` 指向 Termux 本地的 `aapt2`。

## 📖 使用说明

1. **打开应用** → 点击「选择文件夹」按钮
2. **选择目录** → 系统弹出文件选择器，选择要操作的文件夹
3. **勾选文件** → 点击文件/目录前的复选框进行选择
4. **批量操作** → 点击底部「批量重命名」按钮
5. **选择类型** → 添加前缀/后缀 或 删除前N个/后N个字符
6. **确认执行** → 点击「开始重命名」

## 🎨 主题定制

应用主题会根据系统深浅色模式自动切换，同时从当前桌面壁纸提取主色调：

- **浅色模式** → 浅紫色液态玻璃主题
- **深色模式** → 深紫色液态玻璃主题
- **壁纸取色** → 根据壁纸主色调动态调整主题色

## 📄 许可证

本项目仅供学习参考。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
