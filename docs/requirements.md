# T9 快速启动器 App

## 需求

基于T9键盘交互，快速筛选并启动已安装的Android应用。用户在T9数字键盘上输入数字序列，应用实时匹配已安装App名称中的对应字母，展示候选列表，点击即可启动。

## 核心功能

1. **T9数字键盘** - 标准3x4布局（2-9对应ABC-WXYZ，0空格，*删除）
2. **应用列表查询** - 通过PackageManager获取已安装应用
3. **T9匹配算法** - 数字序列 → 字母组合 → 匹配App名称前缀/包含
4. **实时筛选** - 每次按键立即更新候选应用列表
5. **启动应用** - 点击候选App直接启动

## 技术方案

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **最低API**: 26 (Android 8.0)
- **架构**: 单Activity + Compose

## 文件结构

```
app/src/main/
├── AndroidManifest.xml
├── java/com/t9launcher/
│   ├── MainActivity.kt          # 入口Activity，承载Compose
│   ├── T9LauncherScreen.kt     # 主界面：键盘+列表，整体布局
│   ├── engine/
│   │   └── T9Matcher.kt        # T9匹配核心算法（纯逻辑，不依赖UI）
│   └── model/
│       └── AppInfo.kt          # 应用信息数据类
├── res/
│   └── values/
│       └── strings.xml
```

> UI从简，先不做复杂组件拆分和皮肤系统。后续皮肤可抽取主题/颜色token层。
