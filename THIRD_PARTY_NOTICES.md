# Third-Party Notices

本仓库不包含 GPL / LGPL / AGPL 代码或库。

## Aerial TV（设计参考，MIT）

- 项目: https://github.com/pengchujin/aerial-tv
- 作者: 彭楚金（pengchujin）
- 许可证: MIT
- 关系: 本仓库的 PenDesk 是重新实现的代码，未拷贝上游源码，也未复用其图标、字体、影片或天气数据等素材；布局风格与交互思路参考了该项目。

致谢说明：Aerial TV 面向 Android TV 大屏，本项目面向 258×960 的 Android 6.0 点读笔。我们参考了其 tvOS 风格的视觉语言（时钟栏 + 横向卡片、克制的配色与留白），但布局适配、横竖屏切换、应用枚举、长按菜单、隐藏应用与「学习桌面」入口均为独立实现。上游为 MIT 许可，本项目亦为 MIT，许可证兼容；如上游作者对署名或表述有异议，可通过 Issue 联系调整。

若后续引入上游源码片段，请在上游 MIT 许可下保留其版权声明。

## Kotlin Standard Library（Apache-2.0）

- 项目: https://github.com/JetBrains/kotlin
- 许可证: Apache License 2.0
- 用途: 两个 APK 的运行时依赖，由 Kotlin Gradle 插件自动打包。

Apache-2.0 全文: https://www.apache.org/licenses/LICENSE-2.0

## Android Framework（Apache-2.0）

仅调用系统 API（WindowManager、PackageManager、AudioManager、AccessibilityService 等），未打包 Android 源码。
Android SDK 许可: https://developer.android.com/studio/terms

## 图标与素材

应用图标与界面图形均由代码自绘，或使用系统 `android.R.drawable` 资源，未引入第三方图标包。

