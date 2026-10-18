# 手机性能监控 APK

一个用于监控手机性能参数的 Android 应用。

## 功能特性

- **CPU 使用率**: 实时显示 CPU 使用百分比
- **内存使用**: 显示应用当前内存占用
- **电池状态**: 显示电量百分比和充电状态
- **存储空间**: 显示可用和已用存储空间
- **设备温度**: 显示平均温度（如果有传感器）
- **进程数量**: 显示当前运行的进程数

## 技术栈

- 目标 SDK: Android 35 (Android 15)
- 最低 SDK: Android 7.0 (API 24)
- 语言: Java
- UI: Material Design 3 (M3)
- 构建工具: Android Gradle Plugin 8.5.2

## 构建要求

- JDK 17+
- Android SDK (Platform 35)
- Android SDK Build-Tools 35.0.0
- Gradle 8.9

## 安装步骤

### 方法一: 使用已生成的 APK

1. 将 `PhonePerformance.apk` 文件传输到 Android 设备
2. 在设备上启用"未知来源"安装
3. 打开 APK 文件进行安装
4. 启动应用并授予所需权限

### 方法二: 从源码构建

```bash
# 安装必要工具
./gradlew assembleDebug

# 生成的 APK 位于: app/build/outputs/apk/debug/PhonePerformance.apk
```

## 权限说明

应用需要以下权限：

- `ACTIVITY_RECOGNITION`: 获取运行中的应用信息
- `FOREGROUND_SERVICE`: 允许前台服务运行
- `BATTERY_STATS`: 读取电池状态
- `PACKAGE_USAGE_STATS`: 读取包使用统计
- `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`: 读取存储信息

## 文件结构

```
PhonePerformance/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/phoneperformance/
│   │   │   ├── MainActivity.java      # 主界面
│   │   │   └── PerformanceService.java # 后台服务
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml  # 主布局
│   │   │   ├── values/
│   │   │   │   ├── colors.xml         # M3 颜色
│   │   │   │   ├── strings.xml        # 字符串
│   │   │   │   └── themes.xml         # M3 主题
│   │   │   └── mipmap-*/              # 应用图标
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 使用注意事项

1. 温度数据取决于设备硬件，不是所有设备都有温度传感器
2. 部分权限需要在设备安装后手动授予
3. 建议在实际设备上测试以获得最佳体验

## APK 下载

生成的 APK 文件位于:
- `/workspace/PhonePerformance/app/build/outputs/apk/debug/PhonePerformance.apk`
- 大小: 18KB

## 许可证

MIT License
