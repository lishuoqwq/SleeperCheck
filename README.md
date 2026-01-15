# "睡了吗" APP

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0+-green.svg" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Kotlin-1.9.20-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

## 📱 项目简介

"睡了吗"是一款极致简洁、本地优先且低功耗的睡眠习惯记录 APP。通过回顾式查询系统使用统计，帮助用户记录和改善睡眠习惯。

### ✨ 核心特性

- 🔒 **隐私至上** - 无网络权限，所有数据本地存储
- ⚡ **低功耗** - 回顾式查询，无常驻后台服务
- 🎯 **智能检测** - 基于 UsageStatsManager 自动判断熬夜
- 📊 **统计分析** - 月度打卡统计、连续打卡记录
- ⏰ **睡前提醒** - 可自定义的睡前通知提醒

## 🏗️ 技术架构

- **语言**: Kotlin
- **最低版本**: Android 8.0 (API 26)
- **架构**: MVVM + Repository
- **数据库**: Room (SQLite)
- **偏好设置**: DataStore
- **后台任务**: WorkManager
- **UI**: ViewBinding + Material Design

## 📦 构建说明

### 前置要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK 34

### 构建步骤

1. 克隆项目
```bash
git clone https://github.com/yourusername/are_you_sleep.git
cd are_you_sleep
```

2. 使用 Android Studio 打开项目

3. 等待 Gradle 同步完成

4. 运行项目或构建 APK
```bash
./gradlew assembleRelease
```

## 🚀 GitHub Actions 自动构建

项目已配置 GitHub Actions，每次推送到 master/main 分支时会自动构建 APK。

构建产物可在 Actions 页面的 Artifacts 中下载。

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**注意**: 首次使用 Android Studio 打开项目时，会自动生成 Gradle Wrapper 文件。
