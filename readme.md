# 😈 CleanerKing - 全能电脑清理工具 😈

---

# DWGX 的项目 💻
### 采用大神 Java 语言 ☕
- 世界上最便捷的控制台页面操作
- 懒得用 GUI 因为用的人是傻逼 😄

---

## 🛠 功能特点

### 💥 一键清理

一步清理运行时生成的垃圾：

- 🗑 用户临时文件
- 📅 系统临时文件
- 📢 回收站
- 📚 预取文件

### 💻 高级清理

按需选择清理对象：

- 📢 回收站清空
- 📂 清理 Windows 更新缓存
- 📇 清理浏览器缓存 (Edge, Chrome, Firefox)
- 📊 刷新 DNS 缓存

### 🌐 文件搜索

- 🔍 **基本搜索**：根据文件名搜索
- 🔍 **高级搜索**：根据大小、日期等条件

### 📄 大文件扫描与删除

- 📂 查找大小超过指定阈值的文件
- 📝 自选文件并一键删除

### 🌟 显示 3D ASCII 艺术标题

- 💄 可自定义艺术标题的颜色

### 📤 进度条动画

- 🌈 一键显示动态动画
- 🔧 可自定义动画颜色

### 🔧 Windows 内置工具

控制 Windows 内置工具：

- 🛠 磁盘清理（cleanmgr）
- 🛂 磁盘管理（diskmgmt.msc）
- 🛡 系统配置（msconfig）
- 📊 系统信息（msinfo32）
- 🛣 碎片整理（dfrgui）

### 📡 设置模块

- 🎨 自定义艺术标题颜色
- 🛢 启用/禁用动画
- 🔒 启用/禁用日志

---

## 🛠️ 安装与使用

1. 从本项目源码采集仓库中下载。
2. 编译项目：
   ```bash
   javac -encoding UTF-8 src/org/CleanerKing/Client.java
   ```
3. 运行清理工具：
   ```bash
   java -Dfile.encoding=UTF-8 org.CleanerKing.Client
   ```

4. 使用批处理文件（BAT）启动：
   ```bat
   @echo off
   chcp 65001 > nul
   java -Dfile.encoding=UTF-8 -jar CleanerKing.jar
   pause

   ```

---

## 📈 更新记录

- **v1.0.0** - 初始发布

---

## 👨‍💻 作者

**CleanerKing 工具**电脑清理工具

📧 如有问题，请联系：[dwgx1337@gmail.com](mailto:dwgx1337@gmail.com)

---

## 🚀 欢迎为我们提供反馈！

进步完善、更加高效，欢迎进行招募或提供意见！ 🎉

