// File: src/org/CleanerKing/Utils.java
package org.CleanerKing;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * 工具类，包含常用的静态方法和常量。
 */
public class Utils {
    public static Scanner scanner = new Scanner(System.in, "UTF-8");
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    public static final String USER_TEMP = System.getenv("TEMP");
    public static final String SYSTEM_TEMP = "C:\\Windows\\Temp";
    public static final String RECYCLE_BIN = System.getenv("SystemDrive") + "\\$Recycle.Bin";
    public static final String SOFTWARE_DISTRIBUTION = "C:\\Windows\\SoftwareDistribution\\Download";
    public static final String PREFETCH = "C:\\Windows\\Prefetch";
    public static final String RECENT_FILES = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Recent";
    public static final String EDGE_CACHE = System.getenv("LOCALAPPDATA") + "\\Microsoft\\Edge\\User Data\\Default\\Cache";
    public static final String CHROME_CACHE = System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\User Data\\Default\\Cache";
    public static final String FIREFOX_CACHE = System.getenv("APPDATA") + "\\Mozilla\\Firefox\\Profiles";

    // 实例化Settings对象
    private static final Settings settings = new Settings();

    // Logging
    private static boolean loggingEnabled = settings.isLoggingEnabled();
    private static PrintWriter logWriter = null;
    private static final String LOG_FILE_PATH = "cleanerking.log";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Loading Animation Setting
    private static boolean showLoadingAnimation = settings.isShowLoadingAnimation();

    // Protected Directories to prevent accidental deletion
    private static final List<String> PROTECTED_DIRECTORIES = Arrays.asList(
            "C:\\Windows",
            "C:\\Program Files",
            "C:\\Program Files (x86)",
            "C:\\ProgramData"
    );

    /**
     * 初始化日志记录，如果启用的话。
     */
    static {
        if (loggingEnabled) {
            try {
                logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(LOG_FILE_PATH, true), "UTF-8"), true);
                logEvent("日志记录已启用。");
            } catch (IOException e) {
                System.out.println("无法启用日志记录: " + e.getMessage());
                loggingEnabled = false;
            }
        }
    }

    /**
     * 设置 3D ASCII 艺术标题颜色。
     */
    public static void setAsciiArtColor(String colorCode) {
        settings.setAsciiArtColor(colorCode);
    }

    /**
     * 设置加载动画颜色。
     */
    public static void setLoadingAnimationColor(String colorCode) {
        settings.setLoadingAnimationColor(colorCode);
    }

    /**
     * 启用或禁用日志保存。
     */
    public static void enableLogging(boolean enable) {
        settings.setLoggingEnabled(enable);
        loggingEnabled = enable;
        if (enable) {
            try {
                if (logWriter == null) {
                    logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(LOG_FILE_PATH, true), "UTF-8"), true);
                }
                logEvent("日志记录已启用。");
            } catch (IOException e) {
                System.out.println("无法启用日志记录: " + e.getMessage());
                loggingEnabled = false;
            }
        } else {
            if (logWriter != null) {
                logEvent("日志记录已禁用。");
                logWriter.close();
                logWriter = null;
            }
        }
    }

    /**
     * 检查日志是否启用。
     */
    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * 设置是否显示加载动画。
     */
    public static void setShowLoadingAnimation(boolean show) {
        settings.setShowLoadingAnimation(show);
        showLoadingAnimation = show;
    }

    /**
     * 检查是否显示加载动画。
     */
    public static boolean isShowLoadingAnimation() {
        return showLoadingAnimation;
    }

    /**
     * 记录事件日志，简单描述性日志。
     */
    public static void logEvent(String event) {
        String timestamp = sdf.format(new Date());
        String logEntry = "[" + timestamp + "] 事件: " + event;
        if (loggingEnabled && logWriter != null) {
            logWriter.println(logEntry);
        }
        // 同时输出到控制台
        synchronizedPrint(getAsciiArtColor() + logEntry + "\033[0m");
    }

    /**
     * 记录详细日志，包含更多技术细节。
     */
    public static void logDetail(String detail) {
        String timestamp = sdf.format(new Date());
        String logEntry = "[" + timestamp + "] 详细: " + detail;
        if (loggingEnabled && logWriter != null) {
            logWriter.println(logEntry);
        }
        // 可以选择是否输出到控制台
    }

    /**
     * 获取当前的 ASCII 艺术颜色。
     */
    public static String getAsciiArtColor() {
        return settings.getAsciiArtColor();
    }

    /**
     * 获取当前的加载动画颜色。
     */
    public static String getLoadingAnimationColor() {
        return settings.getLoadingAnimationColor();
    }

    /**
     * 清屏功能，兼容Windows和其他操作系统。
     */
    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception ex) {
            // 忽略异常
        }
    }

    /**
     * 暂停，等待用户按回车键继续。
     */
    public static void pause() {
        System.out.println("按回车键继续...");
        scanner.nextLine();
    }

    /**
     * 递归删除目录及其内容。
     *
     * @param dirPath 目录路径
     */
    public static void deleteDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return;
        }

        // 检查是否为受保护的目录
        for (String protectedDir : PROTECTED_DIRECTORIES) {
            if (dirPath.equalsIgnoreCase(protectedDir) || dirPath.startsWith(protectedDir + "\\")) {
                showWarning("无法删除受保护的目录或其子目录: " + dirPath);
                logDetail("用户尝试删除受保护的目录或其子目录: " + dirPath);
                return;
            }
        }

        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(filePath);
                        logDetail("删除文件: " + filePath.toString());
                    } catch (IOException e) {
                        showWarning("无法删除文件: " + filePath.toString() + " 错误: " + e.getMessage());
                        logDetail("无法删除文件: " + filePath.toString() + " 错误: " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dirPath, IOException exc) throws IOException {
                    if (exc != null) {
                        showWarning("访问目录时发生错误: " + dirPath.toString() + " 错误: " + exc.getMessage());
                        logDetail("访问目录时发生错误: " + dirPath.toString() + " 错误: " + exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        Files.delete(dirPath);
                        logDetail("删除目录: " + dirPath.toString());
                    } catch (IOException e) {
                        showWarning("无法删除目录: " + dirPath.toString() + " 错误: " + e.getMessage());
                        logDetail("无法删除目录: " + dirPath.toString() + " 错误: " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            logEvent("目录 " + dirPath + " 已删除。");
        } catch (IOException e) {
            showWarning("无法删除目录: " + dir.getAbsolutePath() + " 错误: " + e.getMessage());
            logDetail("无法删除目录: " + dir.getAbsolutePath() + " 错误: " + e.getMessage());
        }
    }

    /**
     * 清空回收站，使用PowerShell命令。
     */
    public static void emptyRecycleBin() {
        // 使用 PowerShell 命令清空回收站
        String command = "PowerShell.exe -NoProfile -Command \"Clear-RecycleBin -Force -ErrorAction SilentlyContinue\"";
        try {
            Process process = Runtime.getRuntime().exec(command);
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), Utils::synchronizedPrint);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), Utils::synchronizedPrint);
            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logEvent("回收站已清空。");
            } else {
                showWarning("清空回收站时发生错误。");
                logDetail("清空回收站时发生错误，退出代码: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            showWarning("清空回收站时发生错误: " + e.getMessage());
            logDetail("清空回收站时发生错误: " + e.getMessage());
        }
    }

    /**
     * 清理Firefox浏览器缓存。
     */
    public static void deleteFirefoxCache() {
        try {
            File profilesDir = new File(FIREFOX_CACHE);
            if (profilesDir.exists() && profilesDir.isDirectory()) {
                File[] profiles = profilesDir.listFiles((dir, name) -> name.contains(".default"));
                if (profiles != null) {
                    for (File profile : profiles) {
                        File cacheDir = new File(profile, "cache2\\entries");
                        deleteDirectory(cacheDir.getAbsolutePath());
                        synchronizedPrint("已清理Firefox缓存: " + cacheDir.getAbsolutePath());
                        logEvent("已清理Firefox缓存: " + cacheDir.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            showWarning("无法清理Firefox缓存: " + e.getMessage());
            logDetail("无法清理Firefox缓存: " + e.getMessage());
        }
    }

    /**
     * 刷新DNS缓存。
     */
    public static void flushDNS() {
        String command = "cmd.exe /c ipconfig /flushdns";
        try {
            Process process = Runtime.getRuntime().exec(command);
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), Utils::synchronizedPrint);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), Utils::synchronizedPrint);
            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                synchronizedPrint("DNS缓存已刷新。");
                logEvent("DNS缓存已刷新。");
            } else {
                showWarning("刷新DNS缓存时发生错误。");
                logDetail("刷新DNS缓存时发生错误，退出代码: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            showWarning("刷新DNS缓存时发生错误: " + e.getMessage());
            logDetail("刷新DNS缓存时发生错误: " + e.getMessage());
        }
    }

    /**
     * 执行外部命令。
     *
     * @param command 要执行的命令
     */
    public static void executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), Utils::synchronizedPrint);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), Utils::synchronizedPrint);
            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logEvent("执行命令: " + command);
            } else {
                showWarning("执行命令时发生错误，退出代码: " + exitCode);
                logDetail("执行命令时发生错误: " + command + "，退出代码: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            showWarning("执行命令时发生错误: " + e.getMessage());
            logDetail("执行命令时发生错误: " + e.getMessage());
        }
    }

    /**
     * 检查磁盘功能菜单。
     */
    public static void chkdskMenu() {
        System.out.print("请输入要检查的盘符 (如 C:, 输入 ESC 返回): ");
        String drv = getUserInput().trim().toUpperCase();
        if (drv.equalsIgnoreCase("ESC")) {
            return;
        }
        if (drv.isEmpty()) {
            showWarning("未输入盘符。");
            logDetail("用户未输入盘符进行chkdsk操作。");
            pause();
            return;
        }

        System.out.println("请选择chkdsk模式：");
        System.out.println("[1] 仅扫描");
        System.out.println("[2] 扫描并修复错误 (/f)");
        System.out.println("[3] 扫描、修复错误并恢复坏道上的信息 (/f /r)");
        System.out.print("请选择模式 (1/2/3): ");
        String mode = getUserInput().trim();
        String chkdskCommand = "chkdsk " + drv;
        switch (mode) {
            case "1":
                // 仅扫描
                break;
            case "2":
                chkdskCommand += " /f";
                break;
            case "3":
                chkdskCommand += " /f /r";
                break;
            default:
                showWarning("无效模式选择，默认仅扫描。");
                logDetail("用户选择了无效的chkdsk模式: " + mode + "，默认仅扫描。");
        }
        System.out.println("正在检查 " + drv + " ...");
        executeCommand("cmd.exe /c start " + chkdskCommand);
        logEvent("执行chkdsk命令: " + chkdskCommand);
        pause();
    }

    /**
     * 高级清理功能：允许用户选择具体的清理选项。
     */
    public static void advancedClean() {
        while (true) {
            clearScreen();
            System.out.println("============= 高级清理选项 =============");
            System.out.println("[1] 清理用户临时文件");
            System.out.println("[2] 清理系统临时文件");
            System.out.println("[3] 清空回收站");
            System.out.println("[4] 清理Windows更新缓存");
            System.out.println("[5] 清理预取文件");
            System.out.println("[6] 清理最近打开的文件记录");
            System.out.println("[7] 清理浏览器缓存 (Edge)");
            System.out.println("[8] 清理浏览器缓存 (Chrome)");
            System.out.println("[9] 清理浏览器缓存 (Firefox)");
            System.out.println("[10] 刷新DNS缓存");
            System.out.println("[ESC] 返回主菜单");
            System.out.println("-----------------------------------------");
            System.out.print("请选择要清理的选项 (1-10 或 ESC, 多选用逗号分隔): ");
            String input = getUserInput().trim().toUpperCase();
            if (input.equals("ESC")) {
                return;
            }

            String[] selections = input.split(",");
            boolean anySelected = false;
            for (String sel : selections) {
                sel = sel.trim();
                switch (sel) {
                    case "1":
                        deleteDirectory(USER_TEMP);
                        synchronizedPrint("已清理用户临时文件。");
                        logEvent("清理用户临时文件。");
                        anySelected = true;
                        break;
                    case "2":
                        deleteDirectory(SYSTEM_TEMP);
                        synchronizedPrint("已清理系统临时文件。");
                        logEvent("清理系统临时文件。");
                        anySelected = true;
                        break;
                    case "3":
                        emptyRecycleBin();
                        synchronizedPrint("已清空回收站。");
                        logEvent("清空回收站。");
                        anySelected = true;
                        break;
                    case "4":
                        deleteDirectory(SOFTWARE_DISTRIBUTION);
                        synchronizedPrint("已清理Windows更新缓存。");
                        logEvent("清理Windows更新缓存。");
                        anySelected = true;
                        break;
                    case "5":
                        deleteDirectory(PREFETCH);
                        synchronizedPrint("已清理预取文件。");
                        logEvent("清理预取文件。");
                        anySelected = true;
                        break;
                    case "6":
                        deleteDirectory(RECENT_FILES);
                        synchronizedPrint("已清理最近打开的文件记录。");
                        logEvent("清理最近打开的文件记录。");
                        anySelected = true;
                        break;
                    case "7":
                        deleteDirectory(EDGE_CACHE);
                        synchronizedPrint("已清理Edge浏览器缓存。");
                        logEvent("清理Edge浏览器缓存。");
                        anySelected = true;
                        break;
                    case "8":
                        deleteDirectory(CHROME_CACHE);
                        synchronizedPrint("已清理Chrome浏览器缓存。");
                        logEvent("清理Chrome浏览器缓存。");
                        anySelected = true;
                        break;
                    case "9":
                        deleteFirefoxCache();
                        synchronizedPrint("已清理Firefox浏览器缓存。");
                        logEvent("清理Firefox浏览器缓存。");
                        anySelected = true;
                        break;
                    case "10":
                        flushDNS();
                        synchronizedPrint("已刷新DNS缓存。");
                        logEvent("刷新DNS缓存。");
                        anySelected = true;
                        break;
                    default:
                        showWarning("无效选择: " + sel);
                        logDetail("用户输入无效选择: " + sel);
                }
            }

            if (anySelected) {
                System.out.println("高级清理完成！");
                logEvent("执行高级清理完成。");
            } else {
                System.out.println("未选择任何有效选项。");
                logEvent("用户取消了高级清理操作。");
            }
            pause();
        }
    }

    /**
     * 读取单个字符输入（例如 ESC 键）。
     * 注意：JLine 3.x 不支持 readBinding
     */
    public static Character readSingleCharacter() {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(new DefaultParser())
                    .build();

            String input = reader.readLine("", null, null);

            if (input != null && !input.isEmpty()) {
                char ch = input.charAt(0);
                if (ch == 27) { // ESC键
                    return 27;
                }
                return ch;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 显示 3D ASCII 艺术标题，添加颜色。
     */
    public static void display3DASCII() {
        String asciiArt =
                getAsciiArtColor() + // 使用设置的颜色
                        " ________      ___       __       ________      ___    ___   _____      ________      ________      ________  \n" +
                        "|\\   ___ \\    |\\  \\     |\\  \\    |\\   ____\\    |\\  \\  /  /| / __  \\    |\\_____  \\    |\\_____  \\    |\\_____  \\ \n" +
                        "\\ \\  \\_|\\ \\   \\ \\  \\    \\ \\  \\   \\ \\  \\___|    \\ \\  \\/  / /|\\/|\\  \\   \\|____|\\ /_   \\|____|\\ /_    \\|___/  /|\n" +
                        " \\ \\  \\ \\\\ \\   \\ \\  \\  __\\ \\  \\   \\ \\  \\  ___   \\ \\    / / \\|/ \\ \\  \\        \\|\\  \\        \\|\\  \\       /  / /\n" +
                        "  \\ \\  \\_\\\\ \\   \\ \\  \\|\\__\\_\\  \\   \\ \\  \\|\\  \\   /     \\/       \\ \\  \\      __\\_\\  \\      __\\_\\  \\     /  / / \n" +
                        "   \\ \\_______\\   \\ \\____________\\   \\ \\_______\\ /  /\\   \\        \\ \\__\\    |\\_______\\    |\\_______\\   /__/ /  \n" +
                        "    \\|_______|    \\|____________|    \\|_______|/__/ /\\ __\\        \\|__|    \\|_______|    \\|_______|   |__|/   \n" +
                        "                                               |__|/ \\|__|                                                    \n" +
                        "\033[0m"; // 重置颜色

        System.out.println(asciiArt);
    }

    /**
     * 显示加载动画，添加颜色和改进动画效果。
     */
    public static void showLoadingAnimation() {
        if (!showLoadingAnimation) {
            return;
        }
        String loadingText = getLoadingAnimationColor() + "加载中" + "\033[0m"; // 使用设置的颜色
        String animation = "|/-\\";
        int repeat = 5; // 增加循环次数

        System.out.print(loadingText);
        for (int i = 0; i < repeat; i++) {
            for (char c : animation.toCharArray()) {
                System.out.print("\r" + loadingText + " " + c);
                try {
                    Thread.sleep(100); // 每帧间隔 100 毫秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("\r" + getLoadingAnimationColor() + "加载完成！   \033[0m");
        logEvent("加载动画完成。");
    }

    /**
     * 显示红色警告信息。
     */
    public static void showWarning(String message) {
        synchronizedPrint("\033[1;31m警告: " + message + "\033[0m");
        logDetail("警告: " + message);
    }

    /**
     * 读取用户输入，提供统一的方法
     */
    public static String getUserInput() {
        return scanner.nextLine();
    }

    /**
     * 文件搜索类，用于存储文件详情。
     */
    public static class FileDetail {
        private String path;
        private long size;

        public FileDetail(String path, long size) {
            this.path = path;
            this.size = size;
        }

        public String getPath() {
            return path;
        }

        public long getSize() {
            return size;
        }
    }

    /**
     * 内部类用于处理流输出。
     */
    static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) { // 使用UTF-8编码
                String line;
                while ((line = reader.readLine()) != null) {
                    consumer.accept(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 同步打印方法，避免多线程输出混乱。
     *
     * @param message 要打印的消息
     */
    public static synchronized void synchronizedPrint(String message) {
        System.out.println(message);
    }

    /**
     * 关闭Scanner资源。
     */
    public static void closeScanner() {
        if (scanner != null) {
            scanner.close();
        }
    }

    /**
     * 轮转日志文件，当日志文件超过一定大小时，创建新日志文件。
     */
    private static void rotateLogs() {
        final long MAX_LOG_SIZE = 5 * 1024 * 1024; // 5MB
        File logFile = new File(LOG_FILE_PATH);
        if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
            String newName = LOG_FILE_PATH + "." + System.currentTimeMillis() + ".bak";
            File archive = new File(newName);
            if (logFile.renameTo(archive)) {
                try {
                    logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(LOG_FILE_PATH, true), "UTF-8"), true);
                    logEvent("日志文件已轮转，创建新的日志文件。");
                } catch (IOException e) {
                    System.out.println("无法创建新的日志文件: " + e.getMessage());
                    loggingEnabled = false;
                }
            }
        }
    }

    /**
     * 检查是否以管理员权限运行。
     *
     * @return 如果是管理员，则返回true，否则返回false。
     */
    public static boolean isAdmin() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"net", "session"});
                process.getOutputStream().close();
                int exitCode = process.waitFor();
                return (exitCode == 0);
            } catch (IOException | InterruptedException e) {
                return false;
            }
        } else {
            // 对于非Windows系统，假设是以root权限运行
            return (System.getProperty("user.name").equals("root"));
        }
    }
}
