# 小来待办（练手版）

安卓待办 App 初版：可安装、可添加桌面小组件。  
后续会把能力迁移到新生儿记录，并接入阿里云 MySQL。

## 功能

- 添加 / 勾选 / 删除待办
- 本地 Room 数据库存储
- 「小来待办」桌面小组件（显示未完成项，可勾选）

## 构建

```bash
export ANDROID_HOME=$HOME/android-sdk
./gradlew :app:assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

## 安装到手机

1. 手机开启「允许安装未知应用」
2. 把 `app-debug.apk` 传到手机并安装
3. 长按桌面 → 小组件 → 添加「小来待办」
