package org.CleanerKing.ModuleManager.Modules;

import org.CleanerKing.ModuleManager.Module;
import org.CleanerKing.Utils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 文件搜索与删除终极版模块：
 *
 * 特点：
 * 1. 多模式选择：
 *    - 已知完整文件路径（直接检查）
 *    - 快速模式（非递归，glob匹配）
 *    - 普通模式（递归 + 有限深度与文件数限制）
 *    - 深度模式（更大深度与文件数限制）
 *
 * 2. 灵活过滤条件：大小、日期、关键词、正则名匹配、文件大小上限等
 *
 * 3. 性能与用户体验优化：
 *    - 跳过系统目录
 *    - 定期进度与询问是否继续
 *    - 强制删除能力（handle.exe + taskkill）
 */
public class FileSearchModule implements Module {

    private static final Set<String> SKIP_DIRECTORIES = new HashSet<>(Arrays.asList(
            "system volume information", "$recycle.bin", "windows", "program files", "program files (x86)", "programdata"
    ));

    private static final int DEFAULT_MAX_DEPTH = 5;
    private static final long DEFAULT_MAX_FILE_SIZE = Long.MAX_VALUE;
    private static final int SCAN_LIMIT_BEFORE_ASK = 10000;
    private static final int MAX_SCAN_FILES_DEFAULT = 100000;

    @Override
    public String getName() {
        return "文件搜索与删除(终极版)";
    }

    @Override
    public void execute() {
        Utils.clearScreen();
        System.out.println("========== 文件搜索与删除(终极版) ==========");

        System.out.println("请选择搜索模式：");
        System.out.println("[1] 已知完整文件路径（直接检查）");
        System.out.println("[2] 快速模式（非递归，仅文件名glob匹配）");
        System.out.println("[3] 普通模式（有限递归深度和文件数）");
        System.out.println("[4] 深度模式（更大深度和文件数）");
        System.out.print("请输入选项(1-4，ESC返回主菜单)：");
        String modeChoice = Utils.getUserInput().trim();
        if (modeChoice.equalsIgnoreCase("ESC") || modeChoice.isEmpty()) {
            System.out.println("返回主菜单。");
            Utils.pause();
            return;
        }

        switch (modeChoice) {
            case "1":
                directPathCheck();
                break;
            case "2":
                quickModeSearch();
                break;
            case "3":
                normalModeSearch();
                break;
            case "4":
                deepModeSearch();
                break;
            default:
                System.out.println("无效选择，返回主菜单。");
                Utils.pause();
        }
    }

    /**
     * 已知完整文件路径模式
     */
    private void directPathCheck() {
        System.out.print("请输入文件完整路径（如 D:\\Code\\Southside.zip），或ESC返回主菜单：");
        String path = Utils.getUserInput().trim();
        if (path.equalsIgnoreCase("ESC") || path.isEmpty()) {
            System.out.println("返回主菜单。");
            Utils.pause();
            return;
        }
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            System.out.println("找到文件：" + f.getAbsolutePath() + " (大小: " + (f.length()/1024.0/1024.0) + "MB)");
            handleDeletion(Collections.singletonList(new Utils.FileDetail(f.getAbsolutePath(), f.length())));
        } else {
            System.out.println("未找到该文件，请检查路径是否正确。");
            Utils.pause();
        }
    }

    /**
     * 快速模式：非递归glob匹配
     */
    private void quickModeSearch() {
        String dir = chooseDirectoryWithDialog();
        if (dir == null) {
            System.out.println("未选择目录，返回主菜单。");
            Utils.pause();
            return;
        }

        System.out.println("请输入文件名匹配模式（glob），例如：*.txt 或 *southside*，ESC返回：");
        String pattern = Utils.getUserInput().trim();
        if (pattern.equalsIgnoreCase("ESC") || pattern.isEmpty()) {
            System.out.println("未输入模式，返回主菜单。");
            Utils.pause();
            return;
        }

        Long[] sizeRange = askSizeFilter();
        Long minBytes = sizeRange[0];
        Long maxBytes = sizeRange[1];
        Long[] dateRange = askDateFilter();
        Long startTime = dateRange[0];
        Long endTime = dateRange[1];

        List<Utils.FileDetail> results = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir), pattern)) {
            for (Path p : stream) {
                File file = p.toFile();
                if (file.isFile()) {
                    if (filterBySizeAndDate(file, minBytes, maxBytes, startTime, endTime)) {
                        results.add(new Utils.FileDetail(file.getAbsolutePath(), file.length()));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("读取目录时出错: " + e.getMessage());
        }

        showResultsAndDelete(results);
    }

    /**
     * 普通模式：递归+有深度与文件数限制
     */
    private void normalModeSearch() {
        String dir = chooseDirectoryWithDialog();
        if (dir == null) {
            System.out.println("未选择目录，返回主菜单。");
            Utils.pause();
            return;
        }

        int maxDepth = askMaxDepth(DEFAULT_MAX_DEPTH);
        int maxScanFiles = askMaxScanFiles(MAX_SCAN_FILES_DEFAULT);
        Long[] sizeRange = askSizeFilter();
        Long minBytes = sizeRange[0];
        Long maxBytes = sizeRange[1];
        Long[] dateRange = askDateFilter();
        Long startTime = dateRange[0];
        Long endTime = dateRange[1];
        List<String> keywords = askKeywords();
        Pattern fileNamePattern = askFileNamePattern();
        long maxFileSize = askMaxFileSize(DEFAULT_MAX_FILE_SIZE);

        List<Utils.FileDetail> results = deepScan(dir, maxDepth, minBytes, maxBytes, startTime, endTime, keywords, fileNamePattern, maxFileSize, maxScanFiles);

        showResultsAndDelete(results);
    }

    /**
     * 深度模式：更大深度与文件数限制
     */
    private void deepModeSearch() {
        String dir = chooseDirectoryWithDialog();
        if (dir == null) {
            System.out.println("未选择目录，返回主菜单。");
            Utils.pause();
            return;
        }

        int maxDepth = askMaxDepth(20);
        int maxScanFiles = askMaxScanFiles(500000);
        Long[] sizeRange = askSizeFilter();
        Long minBytes = sizeRange[0];
        Long maxBytes = sizeRange[1];
        Long[] dateRange = askDateFilter();
        Long startTime = dateRange[0];
        Long endTime = dateRange[1];
        List<String> keywords = askKeywords();
        Pattern fileNamePattern = askFileNamePattern();
        long maxFileSize = askMaxFileSize(DEFAULT_MAX_FILE_SIZE);

        List<Utils.FileDetail> results = deepScan(dir, maxDepth, minBytes, maxBytes, startTime, endTime, keywords, fileNamePattern, maxFileSize, maxScanFiles);

        showResultsAndDelete(results);
    }

    /**
     * 使用文件对话框选择目录
     */
    private String chooseDirectoryWithDialog() {
        System.out.println("将弹出文件夹选择对话框，请选择要扫描的目录...");
        JFileChooser chooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        chooser.setDialogTitle("请选择要扫描的目录");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            if (selectedDir != null && selectedDir.isDirectory()) {
                return selectedDir.getAbsolutePath();
            }
        }
        return null;
    }

    private boolean askYesNo(String prompt, boolean defaultVal) {
        while (true) {
            System.out.println(prompt + " (回车默认：" + (defaultVal ? "是" : "否") + ")");
            String input = Utils.getUserInput().trim().toUpperCase();
            if (input.isEmpty()) return defaultVal;
            if (input.equals("Y")) return true;
            if (input.equals("N")) return false;
            Utils.showWarning("无效输入，请输入Y或N。");
        }
    }

    private int askMaxDepth(int defaultDepth) {
        System.out.print("请输入最大扫描深度(回车默认" + defaultDepth + "): ");
        String input = Utils.getUserInput().trim();
        if (input.isEmpty()) return defaultDepth;
        try {
            int d = Integer.parseInt(input);
            if (d < 1) {
                System.out.println("深度必须≥1，使用默认值: " + defaultDepth);
                return defaultDepth;
            }
            return d;
        } catch (NumberFormatException e) {
            System.out.println("无效数字，使用默认值: " + defaultDepth);
            return defaultDepth;
        }
    }

    private int askMaxScanFiles(int defaultLimit) {
        System.out.print("请输入最大扫描文件数上限(回车默认" + defaultLimit + "): ");
        String input = Utils.getUserInput().trim();
        if (input.isEmpty()) return defaultLimit;
        try {
            int num = Integer.parseInt(input);
            if (num < 1) {
                System.out.println("文件数上限必须≥1，使用默认值: " + defaultLimit);
                return defaultLimit;
            }
            return num;
        } catch (NumberFormatException e) {
            System.out.println("无效数字，使用默认值: " + defaultLimit);
            return defaultLimit;
        }
    }

    private boolean filterBySizeAndDate(File file, Long minBytes, Long maxBytes, Long startTime, Long endTime) {
        long size = file.length();
        if (minBytes != null && size < minBytes) return false;
        if (maxBytes != null && size > maxBytes) return false;
        long lastModified = file.lastModified();
        if (startTime != null && lastModified < startTime) return false;
        if (endTime != null && lastModified > endTime) return false;
        return true;
    }

    private boolean filterFile(File file, Long minBytes, Long maxBytes, Long startTime, Long endTime, List<String> keywords, Pattern fileNamePattern) {
        if (!filterBySizeAndDate(file, minBytes, maxBytes, startTime, endTime)) return false;

        String fileName = file.getName();
        if (fileNamePattern != null && !fileNamePattern.matcher(fileName).matches()) {
            return false;
        }

        if (!keywords.isEmpty()) {
            String fileNameLower = fileName.toLowerCase();
            for (String kw : keywords) {
                String k = kw.toLowerCase();
                if (!fileNameLower.contains(k)) {
                    // 文件名不包含关键词，检查内容
                    if (!checkFileContent(file, k)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean checkFileContent(File file, String kw) {
        if (!file.isFile() || !file.canRead()) return false;
        // 限制行数
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            int lineCount = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains(kw)) return true;
                lineCount++;
                if (lineCount > 50000) {
                    break;
                }
            }
        } catch (IOException e) {
            // 忽略
        }
        return false;
    }

    private List<Utils.FileDetail> deepScan(String dir, int maxDepth, Long minBytes, Long maxBytes, Long startTime, Long endTime, List<String> keywords, Pattern fileNamePattern, long maxFileSize, int maxScanFiles) {
        List<Utils.FileDetail> matched = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger scanned = new AtomicInteger(0);
        AtomicInteger matchedCount = new AtomicInteger(0);
        AtomicInteger inaccessibleCount = new AtomicInteger(0);
        AtomicBoolean askedContinue = new AtomicBoolean(false);

        // 使用数组存储 maxScanFiles，以便在lambda中修改
        final int[] scanLimit = {maxScanFiles};

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            Files.walkFileTree(Paths.get(dir), EnumSet.of(FileVisitOption.FOLLOW_LINKS), maxDepth, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) {
                    String dirName = d.getFileName() != null ? d.getFileName().toString().toLowerCase() : "";
                    if (SKIP_DIRECTORIES.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path fp, BasicFileAttributes attrs) {
                    int count = scanned.get();
                    if (count >= scanLimit[0]) {
                        System.out.print("已扫描超过设定上限(" + scanLimit[0] + "个文件)，继续吗？(Y=继续,N=停止): ");
                        String c = Utils.getUserInput().trim().toUpperCase();
                        if (c.equals("N")) {
                            return FileVisitResult.TERMINATE;
                        } else {
                            scanLimit[0] *= 2; // 增加上限
                        }
                    }

                    if (!askedContinue.get() && matchedCount.get() == 0 && count > SCAN_LIMIT_BEFORE_ASK && count % 2000 == 0) {
                        System.out.print("已扫描 " + count + " 个文件，尚未找到匹配文件，是否继续？(Y=继续，N=停止): ");
                        String c = Utils.getUserInput().trim().toUpperCase();
                        if (c.equals("N")) {
                            return FileVisitResult.TERMINATE;
                        } else {
                            askedContinue.set(true);
                        }
                    }

                    if (attrs.size() > maxFileSize) {
                        scanned.incrementAndGet();
                        return FileVisitResult.CONTINUE;
                    }

                    executor.submit(() -> {
                        try {
                            File file = fp.toFile();
                            if (filterFile(file, minBytes, maxBytes, startTime, endTime, keywords, fileNamePattern)) {
                                matched.add(new Utils.FileDetail(file.getAbsolutePath(), file.length()));
                                matchedCount.incrementAndGet();
                                synchronized (System.out) {
                                    System.out.printf("找到文件: %s (%.2f MB)%n", file.getAbsolutePath(), file.length() / (1024.0 * 1024.0));
                                }
                            }
                            int c = scanned.incrementAndGet();
                            if (c % 2000 == 0) {
                                synchronized (System.out) {
                                    System.out.printf("已扫描文件: %d，匹配文件: %d%n", c, matchedCount.get());
                                }
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    });
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path f, IOException exc) {
                    inaccessibleCount.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
            });

            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                Utils.showWarning("扫描时间过长，中止扫描。");
            }
        } catch (IOException | InterruptedException e) {
            Utils.showWarning("扫描发生错误: " + e.getMessage());
        }

        if (matched.isEmpty()) {
            System.out.println("未找到满足条件的文件。");
            if (inaccessibleCount.get() > 0) {
                System.out.println("有 " + inaccessibleCount.get() + " 个文件/目录无法访问（可能是系统文件或权限不足）。");
            }
            System.out.println("建议尝试放宽筛选条件，或检查目录是否正确。");
        }

        return matched;
    }

    private void showResultsAndDelete(List<Utils.FileDetail> results) {
        if (results.isEmpty()) {
            Utils.pause();
            return;
        }
        Utils.clearScreen();
        System.out.println("========== 符合条件的文件列表 ==========");
        for (int i = 0; i < results.size(); i++) {
            Utils.FileDetail fd = results.get(i);
            System.out.printf("[%d] %s (%.2f MB)%n", i+1, fd.getPath(), fd.getSize()/(1024.0*1024.0));
        }
        System.out.printf("共找到 %d 个文件。%n", results.size());

        handleDeletion(results);
    }

    private void handleDeletion(List<Utils.FileDetail> files) {
        System.out.println("是否要删除这些文件？");
        System.out.println("[1] 删除全部文件");
        System.out.println("[2] 选择性删除文件");
        System.out.println("[3] 不删除任何文件");
        System.out.print("请选择操作(1/2/3): ");
        String choice = Utils.getUserInput().trim();
        switch (choice) {
            case "1":
                deleteFiles(files);
                break;
            case "2":
                selectiveDeleteFiles(files);
                break;
            default:
                System.out.println("未删除任何文件。");
                Utils.pause();
        }
    }

    private void deleteFiles(List<Utils.FileDetail> files) {
        System.out.println("正在删除文件...");
        for (Utils.FileDetail fd : files) {
            deleteFileWithForce(fd.getPath());
        }
        System.out.println("删除完成。");
        Utils.pause();
    }

    private void selectiveDeleteFiles(List<Utils.FileDetail> files) {
        System.out.println("请输入要删除的文件编号（如 1 或 1-3 或 1,2,3），留空跳过：");
        String in = Utils.getUserInput().trim();
        if (in.isEmpty()) {
            System.out.println("未选择删除任何文件。");
            Utils.pause();
            return;
        }

        Set<Integer> indices = parseIndices(in, files.size());
        if (indices.isEmpty()) {
            System.out.println("无效编号，未删除文件。");
            Utils.pause();
            return;
        }

        for (Integer i : indices) {
            deleteFileWithForce(files.get(i-1).getPath());
        }
        System.out.println("选择性删除完成。");
        Utils.pause();
    }

    private Set<Integer> parseIndices(String input, int max) {
        Set<Integer> set = new HashSet<>();
        String[] parts = input.split(",");
        for (String p : parts) {
            p = p.trim();
            if (p.contains("-")) {
                String[] rr = p.split("-");
                if (rr.length == 2) {
                    try {
                        int start = Integer.parseInt(rr[0]);
                        int end = Integer.parseInt(rr[1]);
                        for (int x = start; x <= end; x++) {
                            if (x >= 1 && x <= max) set.add(x);
                        }
                    } catch (NumberFormatException ignore) {}
                }
            } else {
                try {
                    int val = Integer.parseInt(p);
                    if (val >= 1 && val <= max) set.add(val);
                } catch (NumberFormatException ignore) {}
            }
        }
        return set;
    }

    private void deleteFileWithForce(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("文件不存在: " + path);
            return;
        }
        if (file.delete()) {
            System.out.println("已删除: " + path);
            return;
        }

        // 若需强制删除
        if (Utils.isLoggingEnabled()) {
            System.out.println("无法删除: " + path + " 尝试强制删除...");
        }
        try {
            Process handleP = Runtime.getRuntime().exec(new String[]{"handle.exe", "-accepteula", "-nobanner", path});
            BufferedReader br = new BufferedReader(new InputStreamReader(handleP.getInputStream()));
            String line;
            List<String> pids = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains("pid:")) {
                    String[] tokens = line.split("\\s+");
                    for (String tk : tokens) {
                        if (tk.matches("\\d+")) {
                            pids.add(tk);
                        }
                    }
                }
            }

            for (String pid : pids) {
                Runtime.getRuntime().exec("taskkill /F /PID " + pid).waitFor();
            }

            Thread.sleep(500);
            if (file.delete()) {
                System.out.println("已强制删除: " + path);
            } else {
                System.out.println("仍无法删除: " + path);
            }

        } catch (Exception e) {
            System.out.println("强制删除失败: " + e.getMessage());
        }
    }

    // 复用前面已定义的方法askSizeFilter、askDateFilter、askKeywords、askFileNamePattern、askMaxFileSize避免重复代码
    private Long[] askSizeFilter() {
        System.out.println("是否根据文件大小过滤？(Y=是，N=否，回车默认否)");
        String input = Utils.getUserInput().trim().toUpperCase();
        if (input.isEmpty() || input.equals("N")) return new Long[]{null, null};
        if (!input.equals("Y")) {
            System.out.println("无效输入，不应用大小过滤。");
            return new Long[]{null, null};
        }

        while (true) {
            System.out.println("请输入大小阈值（MB）：");
            System.out.println("- 100 表示≥100MB");
            System.out.println("- -50 表示≤50MB");
            System.out.println("- 50-200 表示介于50MB~200MB之间");
            System.out.println("留空跳过大小过滤：");
            String val = Utils.getUserInput().trim().toUpperCase();
            if (val.isEmpty()) return new Long[]{null, null};

            try {
                Long minBytes = null;
                Long maxBytes = null;
                if (val.contains("-")) {
                    String[] parts = val.split("-");
                    if (parts.length == 2) {
                        minBytes = Long.parseLong(parts[0]) * 1048576;
                        maxBytes = Long.parseLong(parts[1]) * 1048576;
                        if (minBytes > maxBytes) {
                            Utils.showWarning("最小值不能大于最大值，请重试。");
                            continue;
                        }
                    } else {
                        Utils.showWarning("格式错误，如50-200。");
                        continue;
                    }
                } else if (val.startsWith("-")) {
                    maxBytes = Long.parseLong(val.substring(1)) * 1048576;
                } else {
                    minBytes = Long.parseLong(val) * 1048576;
                }
                return new Long[]{minBytes, maxBytes};
            } catch (NumberFormatException e) {
                Utils.showWarning("无效数值，请重新输入。");
            }
        }
    }

    private Long[] askDateFilter() {
        System.out.println("是否根据修改日期过滤？(Y=是，N=否，回车默认否)");
        String input = Utils.getUserInput().trim().toUpperCase();
        if (input.isEmpty() || input.equals("N")) return new Long[]{null, null};
        if (!input.equals("Y")) {
            System.out.println("无效输入，不应用日期过滤。");
            return new Long[]{null, null};
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        while (true) {
            System.out.println("输入起始日期(yyyy-MM-dd)，留空不限制：");
            String startStr = Utils.getUserInput().trim();
            Long startTime = null;
            if (!startStr.isEmpty()) {
                try {
                    startTime = sdf.parse(startStr).getTime();
                } catch (Exception e) {
                    Utils.showWarning("起始日期格式无效，请重试。");
                    continue;
                }
            }

            System.out.println("输入结束日期(yyyy-MM-dd)，留空不限制：");
            String endStr = Utils.getUserInput().trim();
            Long endTime = null;
            if (!endStr.isEmpty()) {
                try {
                    endTime = sdf.parse(endStr).getTime();
                } catch (Exception e) {
                    Utils.showWarning("结束日期格式无效，请重试。");
                    continue;
                }
            }

            if (startTime != null && endTime != null && startTime > endTime) {
                Utils.showWarning("起始日期不能晚于结束日期，请重试。");
                continue;
            }

            return new Long[]{startTime, endTime};
        }
    }

    private List<String> askKeywords() {
        System.out.println("是否根据关键词过滤文件名和内容？(Y=是，N=否，回车默认否)");
        String in = Utils.getUserInput().trim().toUpperCase();
        if (in.isEmpty() || in.equals("N")) return Collections.emptyList();
        if (!in.equals("Y")) {
            System.out.println("无效输入，不应用关键词过滤。");
            return Collections.emptyList();
        }

        System.out.println("请输入关键词（空格分隔），留空不过滤：");
        String kw = Utils.getUserInput().trim();
        if (kw.isEmpty()) return Collections.emptyList();
        return Arrays.asList(kw.split("\\s+"));
    }

    private Pattern askFileNamePattern() {
        System.out.println("是否使用正则表达式匹配文件名？(Y=是，N=否，回车默认否)");
        String in = Utils.getUserInput().trim().toUpperCase();
        if (in.isEmpty() || in.equals("N")) return null;
        if (!in.equals("Y")) {
            System.out.println("无效输入，不使用正则匹配。");
            return null;
        }

        System.out.print("请输入文件名正则表达式(如.*southside.*)：");
        String regex = Utils.getUserInput().trim();
        if (regex.isEmpty()) {
            System.out.println("未输入正则，不使用正则匹配。");
            return null;
        }
        try {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            System.out.println("正则表达式无效，不使用正则匹配。");
            return null;
        }
    }

    private long askMaxFileSize(long defaultMax) {
        System.out.println("是否跳过超大文件？(Y=是，N=否，回车默认否)");
        String in = Utils.getUserInput().trim().toUpperCase();
        if (in.isEmpty() || in.equals("N")) return defaultMax;
        if (!in.equals("Y")) {
            System.out.println("无效输入，使用默认值不跳过超大文件。");
            return defaultMax;
        }

        System.out.print("请输入最大文件大小(MB)，超过则跳过(回车默认不限制): ");
        String val = Utils.getUserInput().trim();
        if (val.isEmpty()) return defaultMax;
        try {
            long mb = Long.parseLong(val);
            if (mb <= 0) {
                System.out.println("无效大小，不限制文件大小。");
                return defaultMax;
            }
            return mb * 1024 * 1024;
        } catch (NumberFormatException e) {
            System.out.println("无效数值，不限制文件大小。");
            return defaultMax;
        }
    }
}
