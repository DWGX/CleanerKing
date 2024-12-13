// File: src/org/CleanerKing/modules/FileSearchModule.java
package org.CleanerKing.modules;

import org.CleanerKing.Module;
import org.CleanerKing.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 文件搜索功能模块，包括基本搜索和高级搜索。
 */
public class FileSearchModule implements Module {

    @Override
    public String getName() {
        return "文件搜索功能";
    }

    @Override
    public void execute() {
        while (true) {
            Utils.clearScreen();
            System.out.println("============= 文件搜索功能 =============");
            System.out.println("[1] 基本搜索 (按文件名)");
            System.out.println("[2] 高级搜索 (按文件名、大小、日期等)");
            System.out.println("[ESC] 返回主菜单");
            System.out.println("-----------------------------------------");
            System.out.print("请选择搜索模式 (1/2/ESC): ");

            Character input = Utils.readSingleCharacter();
            if (input != null && input == 27) { // 27 是 ESC 的ASCII码
                Utils.logDetail("用户选择返回主菜单。");
                return;
            }
            String searchChoice = String.valueOf(input);
            switch (searchChoice) {
                case "1":
                    basicFileSearch();
                    break;
                case "2":
                    advancedFileSearch();
                    break;
                default:
                    Utils.showWarning("无效选择，请重试。");
                    Utils.logDetail("用户输入无效选择: " + searchChoice);
                    Utils.pause();
            }
        }
    }

    /**
     * 基本文件搜索：按文件名搜索。
     */
    private void basicFileSearch() {
        Utils.clearScreen();
        System.out.println("========== 基本文件搜索 ==========");
        String targetDir = promptForDirectory();
        if (targetDir == null) return;

        System.out.print("请输入要搜索的文件名或部分名称 (支持通配符，如 *.txt): ");
        String fileName = Utils.scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            Utils.showWarning("未输入搜索关键字。");
            Utils.logDetail("用户未输入搜索关键字进行基本搜索。");
            Utils.pause();
            return;
        }

        Utils.logEvent("用户开始基本文件搜索，目录: " + targetDir + ", 文件名: " + fileName);
        List<Utils.FileDetail> foundFiles = searchFiles(targetDir, fileName, null, null, null, null);
        if (!foundFiles.isEmpty()) {
            handleSearchResults(foundFiles);
        } else {
            System.out.println("未找到符合条件的文件。");
            Utils.logEvent("基本文件搜索完成，未找到符合条件的文件。");
            Utils.pause();
        }
    }

    /**
     * 高级文件搜索：按文件名、大小、修改日期等条件搜索。
     */
    private void advancedFileSearch() {
        Utils.clearScreen();
        System.out.println("========== 高级文件搜索 ==========");
        String targetDir = promptForDirectory();
        if (targetDir == null) return;

        System.out.print("请输入要搜索的文件名或部分名称 (支持通配符，如 *.log，留空表示不按名称筛选): ");
        String fileName = Utils.scanner.nextLine().trim();

        Long minSize = promptForFileSize("请输入最小文件大小 (MB, 留空表示不按大小筛选): ");
        Long maxSize = promptForFileSize("请输入最大文件大小 (MB, 留空表示不按大小筛选): ");

        Long startTime = promptForDate("请输入修改日期起始范围 (yyyy-MM-dd, 留空表示不按日期筛选): ");
        Long endTime = promptForDate("请输入修改日期结束范围 (yyyy-MM-dd, 留空表示不按日期筛选): ");

        Utils.logEvent("用户开始高级文件搜索，目录: " + targetDir + ", 文件名: " + fileName +
                ", 最小大小: " + (minSize != null ? minSize + " bytes" : "不限制") +
                ", 最大大小: " + (maxSize != null ? maxSize + " bytes" : "不限制") +
                ", 开始时间: " + (startTime != null ? new Date(startTime) : "不限制") +
                ", 结束时间: " + (endTime != null ? new Date(endTime) : "不限制"));

        List<Utils.FileDetail> foundFiles = searchFiles(targetDir, fileName, minSize, maxSize, startTime, endTime);
        if (!foundFiles.isEmpty()) {
            handleSearchResults(foundFiles);
        } else {
            System.out.println("未找到符合条件的文件。");
            Utils.logEvent("高级文件搜索完成，未找到符合条件的文件。");
            Utils.pause();
        }
    }

    /**
     * 统一搜索方法，支持基本和高级搜索。
     */
    private List<Utils.FileDetail> searchFiles(String targetDir, String fileName, Long minSize, Long maxSize, Long startTime, Long endTime) {
        List<Utils.FileDetail> foundFiles = new ArrayList<>();
        Path dirPath = Paths.get(targetDir);

        System.out.println("正在搜索符合条件的文件，请稍候...");
        Utils.logDetail("开始遍历目录: " + dirPath.toString());

        try {
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // 检查目录权限
                    if (!Files.isReadable(dir)) {
                        System.out.println("无法读取目录: " + dir);
                        Utils.logDetail("无法读取目录: " + dir.toString());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                    try {
                        File file = filePath.toFile();
                        long size = file.length();
                        long lastModified = file.lastModified();

                        boolean matches = true;

                        if (!fileName.isEmpty()) {
                            // 使用通配符匹配，转换为正则表达式
                            String regex = fileName.replace(".", "\\.").replace("*", ".*");
                            if (!file.getName().matches(regex)) {
                                matches = false;
                            }
                        }
                        if (minSize != null && size < minSize) {
                            matches = false;
                        }
                        if (maxSize != null && size > maxSize) {
                            matches = false;
                        }
                        if (startTime != null && lastModified < startTime) {
                            matches = false;
                        }
                        if (endTime != null && lastModified > endTime) {
                            matches = false;
                        }

                        if (matches) {
                            foundFiles.add(new Utils.FileDetail(file.getAbsolutePath(), size));
                            Utils.logDetail("找到文件: " + file.getAbsolutePath() + " 大小: " + size + " bytes");
                        }

                    } catch (Exception e) {
                        Utils.showWarning("处理文件时发生错误: " + filePath + " 错误: " + e.getMessage());
                        Utils.logDetail("处理文件时发生错误: " + filePath.toString() + " 错误: " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    if (exc instanceof AccessDeniedException) {
                        System.out.println("无法访问文件: " + file);
                        Utils.logDetail("无法访问文件: " + file.toString());
                    } else {
                        System.out.println("访问文件时发生错误: " + file + " 错误: " + exc.getMessage());
                        Utils.logDetail("访问文件时发生错误: " + file.toString() + " 错误: " + exc.getMessage());
                    }
                    return FileVisitResult.CONTINUE; // 继续遍历其他文件
                }

            });
        } catch (IOException e) {
            Utils.showWarning("搜索过程中发生错误: " + e.getMessage());
            Utils.logDetail("搜索过程中发生错误: " + e.getMessage());
            Utils.pause();
        }

        return foundFiles;
    }

    /**
     * 处理搜索结果，包括显示和删除选项。
     */
    private void handleSearchResults(List<Utils.FileDetail> foundFiles) {
        Utils.clearScreen();
        System.out.println("========== 搜索结果 ==========");
        System.out.println();
        int idx = 1;
        for (Utils.FileDetail fd : foundFiles) {
            System.out.printf("[%d] %s (%.2f MB)%n", idx, fd.getPath(), fd.getSize() / 1048576.0);
            idx++;
        }

        System.out.print("是否要删除这些文件？(Y/N): ");
        String delConfirm = Utils.scanner.nextLine().trim().toUpperCase();
        if (delConfirm.equals("Y")) {
            Utils.logEvent("用户选择删除搜索到的文件。");
            for (Utils.FileDetail fd : foundFiles) {
                File file = new File(fd.getPath());
                if (file.delete()) {
                    System.out.println("已删除文件: " + fd.getPath());
                    Utils.logEvent("删除文件: " + fd.getPath());
                } else {
                    Utils.showWarning("无法删除文件: " + fd.getPath());
                    Utils.logDetail("无法删除文件: " + fd.getPath());
                }
            }
            System.out.println("删除完成！");
            Utils.logEvent("用户完成文件删除操作。");
        } else {
            System.out.println("未删除任何文件。");
            Utils.logEvent("用户取消了文件删除操作。");
        }
        Utils.pause();
    }

    /**
     * 提示用户输入目录路径。
     */
    private String promptForDirectory() {
        System.out.print("请输入要搜索的目录 (如 D:\\, 输入 ESC 返回): ");
        String targetDir = Utils.scanner.nextLine().trim();
        if (targetDir.equalsIgnoreCase("ESC")) {
            Utils.logDetail("用户选择返回主菜单。");
            return null;
        }
        File dir = new File(targetDir);
        if (!dir.exists() || !dir.isDirectory()) {
            Utils.showWarning("目录不存在，请检查后重试。");
            Utils.logDetail("用户输入的目录不存在: " + targetDir);
            Utils.pause();
            return null;
        }
        return targetDir;
    }

    /**
     * 提示用户输入文件大小。
     */
    private Long promptForFileSize(String message) {
        System.out.print(message);
        String input = Utils.scanner.nextLine().trim();
        if (!input.isEmpty()) {
            try {
                return Long.parseLong(input) * 1048576;
            } catch (NumberFormatException e) {
                Utils.showWarning("无效的文件大小输入。");
                Utils.logDetail("用户输入的文件大小无效: " + input);
            }
        }
        return null;
    }

    /**
     * 提示用户输入日期。
     */
    private Long promptForDate(String message) {
        System.out.print(message);
        String input = Utils.scanner.nextLine().trim();
        if (!input.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false); // 严格日期格式
                Date date = sdf.parse(input);
                return date.getTime();
            } catch (Exception e) {
                Utils.showWarning("无效的日期格式。");
                Utils.logDetail("用户输入的日期格式无效: " + input);
            }
        }
        return null;
    }
}
