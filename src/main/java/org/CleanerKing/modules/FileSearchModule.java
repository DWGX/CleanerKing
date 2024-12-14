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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 扫描指定目录下的大文件并选择删除模块。
 */
public class FileSearchModule implements Module {

    @Override
    public String getName() {
        return "扫描指定目录下的大文件并选择删除";
    }

    @Override
    public void execute() {
        while (true) {
            Utils.clearScreen();
            System.out.println("========== 大文件扫描与选择删除 ==========");
            System.out.print("请输入要扫描的目录 (如 D:\\Tools，输入 ESC 返回主菜单): ");
            String targetDir = Utils.getUserInput().trim();
            if (targetDir.equalsIgnoreCase("ESC")) {
                Utils.logDetail("用户选择返回主菜单。");
                return;
            }

            // 自动补全目录路径
            if (targetDir.matches("^[A-Za-z]:$")) {
                targetDir += "\\";
            }
            File dir = new File(targetDir);
            if (!dir.exists() || !dir.isDirectory()) {
                Utils.showWarning("目录不存在，请检查后重试。");
                Utils.logDetail("用户输入的目录不存在或不是目录: " + targetDir);
                Utils.pause();
                continue;
            }

            System.out.println("是否扫描子目录？(Y=包括子目录，N=只扫描当前目录，ESC返回主菜单)");
            System.out.print("请选择 (Y/N/ESC): ");
            String subChoice = Utils.getUserInput().trim().toUpperCase();
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
                continue;
            }

            System.out.println();
            System.out.println("请输入文件大小阈值：");
            System.out.println("- 第一个数为最小值（MB），第二个数为最大值（MB）");
            System.out.println("- 如果只输入第一个数，将筛选大于等于该大小的文件。");
            System.out.println("- 如果只输入第二个数，将筛选小于等于该大小的文件。");
            System.out.println("- 如果输入两个数，将筛选介于两个数之间的文件。");
            System.out.println();
            System.out.print("请输入大小阈值（格式：min max，或只输入 min 或 max，输入 ESC 返回主菜单）: ");
            String sizeInput = Utils.getUserInput().trim().toUpperCase();
            if (sizeInput.equalsIgnoreCase("ESC")) {
                Utils.logDetail("用户选择返回主菜单。");
                return;
            }

            String[] sizeTokens = sizeInput.split("\\s+");
            Long minBytes = null;
            Long maxBytes = null;

            try {
                if (sizeTokens.length == 1) {
                    if (sizeTokens[0].matches("\\d+")) {
                        // 让用户选择是最小值还是最大值
                        System.out.print("您输入的是最小值还是最大值？(min/max): ");
                        String choice = Utils.getUserInput().trim().toLowerCase();
                        if (choice.equals("min")) {
                            minBytes = Long.parseLong(sizeTokens[0]) * 1048576;
                            Utils.logDetail("用户设置最小文件大小阈值: " + minBytes + " bytes。");
                        } else if (choice.equals("max")) {
                            maxBytes = Long.parseLong(sizeTokens[0]) * 1048576;
                            Utils.logDetail("用户设置最大文件大小阈值: " + maxBytes + " bytes。");
                        } else {
                            // 默认认为是最小值
                            minBytes = Long.parseLong(sizeTokens[0]) * 1048576;
                            Utils.logDetail("用户未明确指定，默认设置最小文件大小阈值: " + minBytes + " bytes。");
                        }
                    } else {
                        Utils.showWarning("无效的大小输入。");
                        Utils.logDetail("用户输入的文件大小无效: " + sizeInput);
                        Utils.pause();
                        continue;
                    }
                } else if (sizeTokens.length == 2) {
                    if (sizeTokens[0].matches("\\d+") && sizeTokens[1].matches("\\d+")) {
                        minBytes = Long.parseLong(sizeTokens[0]) * 1048576;
                        maxBytes = Long.parseLong(sizeTokens[1]) * 1048576;
                        if (minBytes > maxBytes) {
                            Utils.showWarning("最小值不能大于最大值。");
                            Utils.logDetail("用户输入的最小值大于最大值: min=" + minBytes + ", max=" + maxBytes);
                            Utils.pause();
                            continue;
                        }
                        Utils.logDetail("用户设置文件大小阈值: min=" + minBytes + " bytes, max=" + maxBytes + " bytes。");
                    } else {
                        Utils.showWarning("无效的大小输入。");
                        Utils.logDetail("用户输入的文件大小无效: " + sizeInput);
                        Utils.pause();
                        continue;
                    }
                } else {
                    Utils.showWarning("无效的大小输入。");
                    Utils.logDetail("用户输入的文件大小格式不正确: " + sizeInput);
                    Utils.pause();
                    continue;
                }
            } catch (NumberFormatException e) {
                Utils.showWarning("无效的大小输入。");
                Utils.logDetail("用户输入的文件大小转换异常: " + sizeInput);
                Utils.pause();
                continue;
            }

            // 输入修改日期范围
            Long startTime = null;
            Long endTime = null;
            System.out.println();
            System.out.println("请输入修改日期范围：");
            System.out.println("- 起始日期 (yyyy-MM-dd)，留空表示不筛选起始日期。");
            System.out.println("- 结束日期 (yyyy-MM-dd)，留空表示不筛选结束日期。");
            System.out.print("请输入起始日期: ");
            String startDateInput = Utils.getUserInput().trim();
            if (!startDateInput.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setLenient(false);
                    Date startDate = sdf.parse(startDateInput);
                    startTime = startDate.getTime();
                    Utils.logDetail("用户设置修改日期起始范围: " + startDateInput);
                } catch (Exception e) {
                    Utils.showWarning("无效的起始日期格式。");
                    Utils.logDetail("用户输入的起始日期格式无效: " + startDateInput);
                }
            }

            System.out.print("请输入结束日期: ");
            String endDateInput = Utils.getUserInput().trim();
            if (!endDateInput.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setLenient(false);
                    Date endDate = sdf.parse(endDateInput);
                    endTime = endDate.getTime();
                    Utils.logDetail("用户设置修改日期结束范围: " + endDateInput);
                } catch (Exception e) {
                    Utils.showWarning("无效的结束日期格式。");
                    Utils.logDetail("用户输入的结束日期格式无效: " + endDateInput);
                }
            }

            System.out.println("正在扫描符合条件的文件，请稍候...");
            System.out.println("(按 Ctrl+C 可中断扫描)");

            List<Utils.FileDetail> largeFiles = Collections.synchronizedList(new ArrayList<>());
            final AtomicInteger scannedFilesCount = new AtomicInteger(0);
            final AtomicInteger matchedFilesCount = new AtomicInteger(0);

            // 使用多线程提高性能
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            try {
                Path dirPath = dir.toPath();
                Long finalMinBytes = minBytes;
                Long finalMaxBytes = maxBytes;
                Long finalStartTime = startTime;
                Long finalEndTime = endTime;

                Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                        executor.submit(() -> {
                            try {
                                File file = filePath.toFile();
                                long size = file.length();
                                long lastModified = file.lastModified();

                                boolean matches = true;

                                if (finalMinBytes != null && size < finalMinBytes) {
                                    matches = false;
                                }
                                if (finalMaxBytes != null && size > finalMaxBytes) {
                                    matches = false;
                                }
                                if (finalStartTime != null && lastModified < finalStartTime) {
                                    matches = false;
                                }
                                if (finalEndTime != null && lastModified > finalEndTime) {
                                    matches = false;
                                }

                                if (matches) {
                                    largeFiles.add(new Utils.FileDetail(file.getAbsolutePath(), size));
                                    matchedFilesCount.incrementAndGet();
                                    Utils.logDetail("扫描到符合条件的文件: " + file.getAbsolutePath() + " 大小: " + size + " bytes");
                                    // 使用同步打印
                                    Utils.synchronizedPrint(String.format("找到文件: %s (%.2f MB)%n", file.getAbsolutePath(), size / (1024.0 * 1024.0)));
                                }

                                scannedFilesCount.incrementAndGet();
                                // 每扫描100个文件，显示一次进度
                                if (scannedFilesCount.get() % 100 == 0) {
                                    Utils.synchronizedPrint(String.format("已扫描文件: %d，匹配文件: %d", scannedFilesCount.get(), matchedFilesCount.get()));
                                }

                            } catch (Exception e) {
                                Utils.showWarning("处理文件时发生错误: " + filePath + " 错误: " + e.getMessage());
                                Utils.logDetail("处理文件时发生错误: " + filePath.toString() + " 错误: " + e.getMessage());
                            }
                        });
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        if (exc instanceof AccessDeniedException) {
                            Utils.synchronizedPrint("无法访问文件: " + file);
                            Utils.logDetail("无法访问文件: " + file.toString());
                        } else {
                            Utils.synchronizedPrint("访问文件时发生错误: " + file + " 错误: " + exc.getMessage());
                            Utils.logDetail("访问文件时发生错误: " + file.toString() + " 错误: " + exc.getMessage());
                        }
                        return FileVisitResult.CONTINUE; // 继续遍历其他文件
                    }

                });

                executor.shutdown();
                // 等待所有任务完成，设置超时时间为1小时
                if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                    executor.shutdownNow();
                    Utils.showWarning("扫描超时，部分结果可能未被找到。");
                    Utils.logDetail("扫描超时，部分结果可能未被找到。");
                }

            } catch (IOException e) {
                Utils.showWarning("扫描过程中发生错误: " + e.getMessage());
                Utils.logDetail("扫描过程中发生错误: " + e.getMessage());
                executor.shutdownNow();
                Utils.pause();
                return;
            } catch (InterruptedException e) {
                Utils.showWarning("扫描过程中被中断: " + e.getMessage());
                Utils.logDetail("扫描过程中被中断: " + e.getMessage());
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                Utils.pause();
                return;
            }

            if (largeFiles.isEmpty()) {
                Utils.synchronizedPrint("未找到满足条件的大文件。");
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
                System.out.printf("[%d] %s (%.2f MB)%n", idx, fd.getPath(), fd.getSize() / (1024.0 * 1024.0));
                idx++;
            }
            System.out.printf("共找到 %d 个文件。%n", largeFiles.size());

            System.out.println();
            System.out.println("是否要删除这些文件？");
            System.out.println("[1] 删除全部文件");
            System.out.println("[2] 选择性删除文件");
            System.out.println("[3] 不删除任何文件");
            System.out.print("请选择操作 (1/2/3): ");
            String delChoice = Utils.getUserInput().trim();

            switch (delChoice) {
                case "1":
                    deleteFiles(largeFiles);
                    break;
                case "2":
                    selectiveDeleteFiles(largeFiles);
                    break;
                case "3":
                    Utils.synchronizedPrint("未删除任何文件。");
                    Utils.logEvent("用户选择不删除任何文件。");
                    Utils.pause();
                    break;
                default:
                    Utils.showWarning("无效选择，未删除任何文件。");
                    Utils.logDetail("用户输入无效的删除选择: " + delChoice);
                    Utils.pause();
            }
        }
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
        return true; // 如果没有限制，默认匹配
    }

    /**
     * 删除所有搜索到的文件。
     *
     * @param files 要删除的文件列表
     */
    private void deleteFiles(List<Utils.FileDetail> files) {
        Utils.logEvent("用户选择删除所有搜索到的文件。");
        for (Utils.FileDetail fd : files) {
            File file = new File(fd.getPath());
            if (file.exists()) {
                if (file.delete()) {
                    Utils.synchronizedPrint("已删除文件: " + fd.getPath());
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
        Utils.synchronizedPrint("删除完成！");
        Utils.logEvent("删除操作完成。");
        Utils.pause();
    }

    /**
     * 选择性删除文件，用户可以输入文件编号进行删除。
     *
     * @param files 要删除的文件列表
     */
    private void selectiveDeleteFiles(List<Utils.FileDetail> files) {
        Utils.clearScreen();
        System.out.println("========== 选择性删除文件 ==========");
        System.out.println("输入要删除的文件编号 (如 1 或 1,2,3 或 1-3)。");
        System.out.println("输入 ESC 返回主菜单，不删除任何文件。");
        System.out.print("请输入编号: ");
        String delInput = Utils.getUserInput().trim().toUpperCase();
        if (delInput.equals("ESC") || delInput.isEmpty()) {
            Utils.synchronizedPrint("未选择删除任何文件。");
            Utils.logEvent("用户未选择删除任何文件。");
            Utils.pause();
            return;
        }

        // 解析输入编号
        Set<Integer> delIndices = parseDeletionIndices(delInput, files.size());
        if (delIndices.isEmpty()) {
            Utils.showWarning("未选择有效的文件编号。");
            Utils.logDetail("用户输入的删除编号无效: " + delInput);
            Utils.pause();
            return;
        }

        // 执行删除
        for (Integer i : delIndices) {
            if (i < 1 || i > files.size()) {
                Utils.showWarning("无效编号: " + i);
                Utils.logDetail("用户输入的无效文件编号: " + i);
                continue;
            }
            Utils.FileDetail fd = files.get(i - 1);
            File file = new File(fd.getPath());
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    Utils.synchronizedPrint("已删除文件: " + fd.getPath());
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

        Utils.synchronizedPrint("删除完成！");
        Utils.logEvent("选择性删除操作完成。");
        Utils.pause();
    }

    /**
     * 解析用户输入的删除编号，支持单个编号、逗号分隔和范围编号。
     *
     * @param input 用户输入的编号字符串
     * @param max   最大有效编号
     * @return 有效编号的集合
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
