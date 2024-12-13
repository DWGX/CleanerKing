// File: src/org/CleanerKing/modules/LargeFileScanDeleteModule.java
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
 * 扫描指定目录下的大文件并选择删除模块。
 */
public class LargeFileScanDeleteModule implements Module {

    @Override
    public String getName() {
        return "扫描指定目录下的大文件并选择删除";
    }

    @Override
    public void execute() {
        Utils.clearScreen();
        System.out.println("========== 大文件扫描与选择删除 ==========");
        System.out.print("请输入要扫描的目录 (如 D:\\Tools，输入 ESC 返回主菜单): ");
        String targetDir = Utils.scanner.nextLine().trim();
        if (targetDir.equalsIgnoreCase("ESC")) {
            Utils.logDetail("用户选择返回主菜单。");
            return;
        }
        File dir = new File(targetDir);
        if (!dir.exists() || !dir.isDirectory()) {
            Utils.showWarning("目录不存在，请检查后重试。");
            Utils.logDetail("用户输入的目录不存在: " + targetDir);
            Utils.pause();
            return;
        }

        System.out.println("是否扫描子目录？(Y=包括子目录，N=只扫描当前目录，ESC返回主菜单)");
        System.out.print("请选择 (Y/N/ESC): ");
        String subChoice = Utils.scanner.nextLine().trim().toUpperCase();
        if (subChoice.equals("ESC")) {
            Utils.logDetail("用户选择返回主菜单。");
            return;
        }

        boolean recursive = false;
        if (subChoice.equals("Y")) {
            recursive = true;
        } else if (subChoice.equals("N")) {
            recursive = false;
        } else {
            Utils.showWarning("无效选择，默认不扫描子目录。");
            Utils.logDetail("用户输入无效的子目录选择: " + subChoice + "，默认不扫描子目录。");
            Utils.pause();
        }

        System.out.println();
        System.out.println("请输入文件大小阈值：");
        System.out.println("- 第一个数为最小值（MB），第二个数为最大值（MB）");
        System.out.println("- 如果只输入第一个数，将筛选大于等于该大小的文件。");
        System.out.println("- 如果只输入第二个数，将筛选小于等于该大小的文件。");
        System.out.println("- 如果输入两个数，将筛选介于两个数之间的文件。");
        System.out.println();
        System.out.print("请输入大小阈值（格式：min max，或只输入 min 或 max，输入 ESC 返回主菜单）: ");
        String sizeInput = Utils.scanner.nextLine().trim().toUpperCase();
        if (sizeInput.equalsIgnoreCase("ESC")) {
            Utils.logDetail("用户选择返回主菜单。");
            return;
        }

        String[] sizeTokens = sizeInput.split("\\s+");
        Long minBytes = null;
        Long maxBytes = null;

        try {
            if (sizeTokens.length == 1) {
                if (sizeInput.matches("\\d+")) {
                    // 判断是否用户想输入最小值还是最大值
                    // 这里假设单个输入为最小值
                    minBytes = Long.parseLong(sizeTokens[0]) * 1048576;
                    Utils.logDetail("用户设置最小文件大小阈值: " + minBytes + " bytes。");
                } else {
                    Utils.showWarning("无效的大小输入。");
                    Utils.logDetail("用户输入的文件大小无效: " + sizeInput);
                    Utils.pause();
                    return;
                }
            } else if (sizeTokens.length == 2) {
                if (sizeTokens[0].matches("\\d+") && sizeTokens[1].matches("\\d+")) {
                    minBytes = Long.parseLong(sizeTokens[0]) * 1048576;
                    maxBytes = Long.parseLong(sizeTokens[1]) * 1048576;
                    if (minBytes > maxBytes) {
                        Utils.showWarning("最小值不能大于最大值。");
                        Utils.logDetail("用户输入的最小值大于最大值: min=" + minBytes + ", max=" + maxBytes);
                        Utils.pause();
                        return;
                    }
                    Utils.logDetail("用户设置文件大小阈值: min=" + minBytes + " bytes, max=" + maxBytes + " bytes。");
                } else {
                    Utils.showWarning("无效的大小输入。");
                    Utils.logDetail("用户输入的文件大小无效: " + sizeInput);
                    Utils.pause();
                    return;
                }
            } else {
                Utils.showWarning("无效的大小输入。");
                Utils.logDetail("用户输入的文件大小格式不正确: " + sizeInput);
                Utils.pause();
                return;
            }
        } catch (NumberFormatException e) {
            Utils.showWarning("无效的大小输入。");
            Utils.logDetail("用户输入的文件大小转换异常: " + sizeInput);
            Utils.pause();
            return;
        }

        System.out.println("正在扫描符合条件的文件，请稍候...");
        System.out.println("(按 Ctrl+C 可中断扫描)");

        List<Utils.FileDetail> largeFiles = new ArrayList<>();

        try {
            if (recursive) {
                final Long finalMinBytes = minBytes;
                final Long finalMaxBytes = maxBytes;
                Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                        File file = filePath.toFile();
                        long size = file.length();
                        if (isFileSizeInRange(size, finalMinBytes, finalMaxBytes)) {
                            largeFiles.add(new Utils.FileDetail(file.getAbsolutePath(), size));
                            System.out.println("正在扫描文件: " + file.getAbsolutePath());
                            Utils.logDetail("扫描到符合条件的文件: " + file.getAbsolutePath() + " 大小: " + size + " bytes");
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            long size = file.length();
                            if (isFileSizeInRange(size, minBytes, maxBytes)) {
                                largeFiles.add(new Utils.FileDetail(file.getAbsolutePath(), size));
                                System.out.println("正在扫描文件: " + file.getAbsolutePath());
                                Utils.logDetail("扫描到符合条件的文件: " + file.getAbsolutePath() + " 大小: " + size + " bytes");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Utils.showWarning("扫描过程中发生错误: " + e.getMessage());
            Utils.logDetail("扫描过程中发生错误: " + e.getMessage());
            Utils.pause();
            return;
        }

        if (largeFiles.isEmpty()) {
            System.out.println("未找到满足条件的大文件。");
            Utils.logEvent("大文件扫描完成，未找到符合条件的文件。");
            Utils.pause();
            return;
        }

        // 显示扫描结果
        Utils.clearScreen();
        System.out.println("========== 符合条件的大文件列表 ==========");
        System.out.println();
        int idx = 1;
        for (Utils.FileDetail fd : largeFiles) {
            System.out.printf("[%d] %s (%.2f MB)%n", idx, fd.getPath(), fd.getSize() / 1048576.0);
            idx++;
        }

        System.out.println();
        System.out.println("输入要删除的文件编号 (如 1 或 1,2,3 或 1-3)。");
        System.out.println("留空或输入 ESC 返回主菜单，不删除任何文件。");
        System.out.print("请输入编号: ");
        String delInput = Utils.scanner.nextLine().trim().toUpperCase();
        if (delInput.equalsIgnoreCase("ESC") || delInput.isEmpty()) {
            System.out.println("未选择删除任何文件。");
            Utils.logEvent("用户未选择删除任何文件。");
            Utils.pause();
            return;
        }

        // 解析输入编号
        Set<Integer> delIndices = parseDeletionIndices(delInput, largeFiles.size());
        if (delIndices.isEmpty()) {
            Utils.showWarning("未选择有效的文件编号。");
            Utils.logDetail("用户输入的删除编号无效: " + delInput);
            Utils.pause();
            return;
        }

        // 执行删除
        for (Integer i : delIndices) {
            if (i < 1 || i > largeFiles.size()) {
                Utils.showWarning("无效编号: " + i);
                Utils.logDetail("用户输入的无效文件编号: " + i);
                continue;
            }
            Utils.FileDetail fd = largeFiles.get(i - 1);
            File file = new File(fd.getPath());
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    System.out.println("已删除文件: " + fd.getPath());
                    Utils.logEvent("删除文件: " + fd.getPath());
                } else {
                    Utils.showWarning("无法删除文件: " + fd.getPath());
                    Utils.logDetail("无法删除文件: " + fd.getPath());
                }
            } else {
                Utils.showWarning("文件不存在: " + fd.getPath());
                Utils.logDetail("用户尝试删除不存在的文件: " + fd.getPath());
            }
        }

        System.out.println("删除完成！");
        Utils.logEvent("大文件删除操作已完成。");
        Utils.pause();
    }

    /**
     * 检查文件大小是否在指定范围内。
     */
    private boolean isFileSizeInRange(long size, Long minBytes, Long maxBytes) {
        if (minBytes != null && maxBytes != null) {
            return size >= minBytes && size <= maxBytes;
        } else if (minBytes != null) {
            return size >= minBytes;
        } else if (maxBytes != null) {
            return size <= maxBytes;
        }
        return false;
    }

    /**
     * 解析用户输入的删除编号，支持单个编号、逗号分隔和范围编号。
     */
    private Set<Integer> parseDeletionIndices(String input, int max) {
        Set<Integer> indices = new HashSet<>();
        String[] parts = input.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0]);
                        int end = Integer.parseInt(range[1]);
                        if (start > end) continue;
                        for (int i = start; i <= end; i++) {
                            if (i >= 1 && i <= max) {
                                indices.add(i);
                            }
                        }
                    } catch (NumberFormatException e) {
                        Utils.showWarning("无效的范围输入: " + part);
                        Utils.logDetail("用户输入的无效范围: " + part);
                    }
                }
            } else {
                try {
                    int num = Integer.parseInt(part);
                    if (num >= 1 && num <= max) {
                        indices.add(num);
                    }
                } catch (NumberFormatException e) {
                    Utils.showWarning("无效的编号输入: " + part);
                    Utils.logDetail("用户输入的无效编号: " + part);
                }
            }
        }
        return indices;
    }
}