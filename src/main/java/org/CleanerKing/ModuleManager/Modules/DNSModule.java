package org.CleanerKing.ModuleManager.Modules;

import org.CleanerKing.ModuleManager.Module;
import org.CleanerKing.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * DNS 设置模块，允许用户自动选择最快最稳定的 DNS，手动输入 DNS 地址，
 * 或者一次性为所有活动的网络接口设置 DNS。
 */
public class DNSModule implements Module {

    // Path to DNS server list with keywords
    private static final String DNS_SERVER_FILE = "dns_servers.txt";

    // Each DNS server's Ping test count
    private static final int PING_COUNT = 5;

    // List of DNS servers loaded from the file
    private static List<DNSEntry> DNS_SERVERS = new ArrayList<>();

    // Predefined list in case the file is not found
    static {
        // Default DNS servers
        DNS_SERVERS.add(new DNSEntry("1.1.1.1", Arrays.asList("cloudflare", "fast", "primary")));
        DNS_SERVERS.add(new DNSEntry("1.0.0.1", Arrays.asList("cloudflare", "fast", "backup")));
        DNS_SERVERS.add(new DNSEntry("8.8.8.8", Arrays.asList("google", "public", "primary")));
        DNS_SERVERS.add(new DNSEntry("8.8.4.4", Arrays.asList("google", "public", "backup")));
        DNS_SERVERS.add(new DNSEntry("208.67.222.222", Arrays.asList("opendns", "secure", "primary")));
        DNS_SERVERS.add(new DNSEntry("208.67.220.220", Arrays.asList("opendns", "secure", "backup")));
        DNS_SERVERS.add(new DNSEntry("114.114.114.114", Arrays.asList("114dns", "china", "public")));
        DNS_SERVERS.add(new DNSEntry("114.114.115.115", Arrays.asList("114dns", "china", "backup")));
        DNS_SERVERS.add(new DNSEntry("223.5.5.5", Arrays.asList("aliyun", "china", "public")));
        DNS_SERVERS.add(new DNSEntry("180.76.76.76", Arrays.asList("baidu", "china", "public")));
        DNS_SERVERS.add(new DNSEntry("119.29.29.29", Arrays.asList("tencent", "china", "public")));
        DNS_SERVERS.add(new DNSEntry("9.9.9.9", Arrays.asList("quad9", "secure", "public")));
        DNS_SERVERS.add(new DNSEntry("149.112.112.112", Arrays.asList("quad9", "secure", "backup")));
    }

    @Override
    public String getName() {
        return "DNS 设置";
    }

    @Override
    public void execute() {
        // 检查是否以管理员权限运行
        if (!Utils.isAdmin()) {
            Utils.showWarning("需要管理员权限来修改 DNS 设置。请以管理员身份运行程序。");
            Utils.logDetail("用户尝试修改 DNS 设置，但缺少管理员权限。");
            Utils.pause();
            return;
        }

        loadDNSServers(); // 加载DNS服务器列表

        while (true) {
            Utils.clearScreen();
            Utils.display3DASCII();
            System.out.println("====================================================");
            System.out.println("                   DNS 设置模块");
            System.out.println("====================================================");
            System.out.println("[1] 自动选择最快DNS");
            System.out.println("[2] 手动输入DNS");
            System.out.println("[3] 为所有网络接口设置DNS");
            System.out.println("[4] 返回主菜单");
            System.out.println("----------------------------------------------------");
            System.out.print("请选择功能: ");

            String choice = Utils.getUserInput().trim();

            if (choice.equalsIgnoreCase("ESC")) {
                Utils.logDetail("用户在DNS设置模块中按下ESC键。");
                return;
            }

            switch (choice) {
                case "1":
                    autoSelectDNS();
                    break;
                case "2":
                    manualInputDNS();
                    break;
                case "3":
                    setDNSForAllInterfaces();
                    break;
                case "4":
                    return;
                default:
                    Utils.showWarning("无效选择，请重新输入。");
                    Utils.pause();
                    break;
            }
        }
    }

    /**
     * 加载DNS服务器列表及其关键词，如果配置文件存在。
     */
    private void loadDNSServers() {
        File file = new File(DNS_SERVER_FILE);
        if (!file.exists()) {
            Utils.logDetail("DNS服务器文件未找到。使用默认DNS服务器列表。");
            return;
        }

        List<DNSEntry> loadedServers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // 每行格式: DNS_IP,keyword1,keyword2,...
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String ip = parts[0].trim();
                    List<String> keywords = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        keywords.add(parts[i].trim().toLowerCase());
                    }
                    loadedServers.add(new DNSEntry(ip, keywords));
                }
            }
            if (!loadedServers.isEmpty()) {
                DNS_SERVERS = loadedServers;
                Utils.logEvent("已从 " + DNS_SERVER_FILE + " 加载DNS服务器列表。");
            } else {
                Utils.logDetail("DNS服务器文件为空或格式不正确。使用默认DNS服务器列表。");
            }
        } catch (IOException e) {
            Utils.showWarning("读取DNS服务器文件时发生错误: " + e.getMessage());
            Utils.logDetail("读取DNS服务器文件时发生错误: " + e.getMessage());
        }
    }

    /**
     * 自动选择最快且最稳定的 DNS
     */
    private void autoSelectDNS() {
        System.out.print("请输入扫描总时间（秒，推荐5-10秒）: ");
        String input = Utils.getUserInput().trim();
        if (input.equalsIgnoreCase("ESC")) {
            return;
        }
        int scanTime;
        try {
            scanTime = Integer.parseInt(input);
            if (scanTime <= 0) {
                throw new NumberFormatException("扫描时间必须为正整数。");
            }
        } catch (NumberFormatException e) {
            System.out.println("无效的输入，请输入一个正整数。");
            Utils.pause();
            return;
        }

        System.out.println("正在测试各个DNS服务器的响应时间和稳定性...");

        // 存储每个DNS的响应时间和丢包率
        List<DNSResult> results = Collections.synchronizedList(new ArrayList<>());

        // 使用线程池并发测试DNS
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(DNS_SERVERS.size(), 20));
        List<Callable<Void>> tasks = new ArrayList<>();

        for (DNSEntry dns : DNS_SERVERS) {
            tasks.add(() -> {
                DNSResult result = testDNS(dns.getDns(), scanTime);
                results.add(result);
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Utils.showWarning("DNS测试被中断。");
            Utils.logDetail("DNS测试被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            return;
        } finally {
            executor.shutdown();
        }

        if (results.isEmpty()) {
            System.out.println("未能测试任何DNS服务器。请检查网络连接。");
            Utils.pause();
            return;
        }

        // 按丢包率升序，然后按响应时间升序排序
        results.sort(Comparator.comparingDouble(DNSResult::getPacketLoss)
                .thenComparingLong(DNSResult::getLatency));

        // 显示测试结果
        System.out.println("====================================================");
        System.out.println("                 DNS 测试结果");
        System.out.println("====================================================");
        System.out.printf("%-5s %-18s %-15s %-10s%n", "序号", "DNS 地址", "响应时间(ms)", "丢包率(%)");
        System.out.println("----------------------------------------------------");
        int index = 1;
        for (DNSResult result : results) {
            String latencyStr = (result.getLatency() == Long.MAX_VALUE) ? "不可达" : String.valueOf(result.getLatency());
            String packetLossStr = String.format("%.2f", result.getPacketLoss());
            System.out.printf("%-5d %-18s %-15s %-10s%n", index++, result.getDns(), latencyStr, packetLossStr);
        }
        System.out.println("----------------------------------------------------");
        System.out.println("序号对应的DNS服务器按速度和稳定性排序，最快且最稳定的DNS排在最前面。");
        System.out.println("您可以设置主DNS和备用DNS，或同时设置。");
        System.out.println("若不想修改主DNS或备用DNS，请直接按回车键跳过。");

        // 设置主DNS
        System.out.print("请输入主DNS的序号 (例如 1)，或直接按回车键跳过设置主DNS: ");
        String primaryChoice = Utils.getUserInput().trim();
        if (primaryChoice.equalsIgnoreCase("ESC")) {
            return;
        }
        DNSResult primaryDNS = null;

        if (!primaryChoice.isEmpty()) {
            try {
                int primaryIndex = Integer.parseInt(primaryChoice);
                if (primaryIndex < 1 || primaryIndex > results.size()) {
                    throw new NumberFormatException("序号超出范围。");
                }
                primaryDNS = results.get(primaryIndex - 1);
            } catch (NumberFormatException e) {
                System.out.println("无效的序号输入。");
                Utils.pause();
                return;
            }
        }

        // 设置备用DNS
        System.out.print("请输入备用DNS的序号 (例如 2)，或直接按回车键跳过设置备用DNS: ");
        String secondaryChoice = Utils.getUserInput().trim();
        if (secondaryChoice.equalsIgnoreCase("ESC")) {
            return;
        }
        DNSResult secondaryDNS = null;

        if (!secondaryChoice.isEmpty()) {
            try {
                int secondaryIndex = Integer.parseInt(secondaryChoice);
                if (secondaryIndex < 1 || secondaryIndex > results.size()) {
                    throw new NumberFormatException("序号超出范围。");
                }
                secondaryDNS = results.get(secondaryIndex - 1);
            } catch (NumberFormatException e) {
                System.out.println("无效的序号输入。");
                Utils.pause();
                return;
            }
        }

        // 更新系统DNS设置
        if (primaryDNS != null || secondaryDNS != null) {
            updateSystemDNS(primaryDNS != null ? primaryDNS.getDns() : null,
                    secondaryDNS != null ? secondaryDNS.getDns() : null);
        } else {
            System.out.println("保持当前DNS设置不变。");
            Utils.pause();
        }
    }

    /**
     * 手动输入DNS地址并设置
     */
    private void manualInputDNS() {
        System.out.println("您可以单独设置主DNS或备用DNS，或同时设置。");
        System.out.println("若不想修改主DNS或备用DNS，请直接按回车键跳过。");

        // 输入主DNS
        System.out.print("请输入主DNS地址: ");
        String primaryDNS = Utils.getUserInput().trim();
        if (primaryDNS.equalsIgnoreCase("ESC")) {
            return;
        }
        if (primaryDNS.isEmpty()) {
            primaryDNS = null;
        } else if (!isValidIPAddress(primaryDNS)) {
            System.out.println("无效的主DNS地址格式。");
            Utils.pause();
            return;
        }

        // 输入备用DNS
        System.out.print("请输入备用DNS地址: ");
        String secondaryDNS = Utils.getUserInput().trim();
        if (secondaryDNS.equalsIgnoreCase("ESC")) {
            return;
        }
        if (secondaryDNS.isEmpty()) {
            secondaryDNS = null;
        } else if (!isValidIPAddress(secondaryDNS)) {
            System.out.println("无效的备用DNS地址格式。");
            Utils.pause();
            return;
        }

        if (primaryDNS == null && secondaryDNS == null) {
            System.out.println("保持当前DNS设置不变。");
            Utils.pause();
            return;
        }

        // 更新系统DNS设置
        updateSystemDNS(primaryDNS, secondaryDNS);
    }

    /**
     * 为所有活动的网络接口设置DNS
     */
    private void setDNSForAllInterfaces() {
        System.out.println("您将为所有活动的网络接口设置DNS。");
        System.out.println("若不想设置主DNS或备用DNS，请直接按回车键跳过。");

        // 输入主DNS
        System.out.print("请输入主DNS地址: ");
        String primaryDNS = Utils.getUserInput().trim();
        if (primaryDNS.equalsIgnoreCase("ESC")) {
            return;
        }
        if (primaryDNS.isEmpty()) {
            primaryDNS = null;
        } else if (!isValidIPAddress(primaryDNS)) {
            System.out.println("无效的主DNS地址格式。");
            Utils.pause();
            return;
        }

        // 输入备用DNS
        System.out.print("请输入备用DNS地址: ");
        String secondaryDNS = Utils.getUserInput().trim();
        if (secondaryDNS.equalsIgnoreCase("ESC")) {
            return;
        }
        if (secondaryDNS.isEmpty()) {
            secondaryDNS = null;
        } else if (!isValidIPAddress(secondaryDNS)) {
            System.out.println("无效的备用DNS地址格式。");
            Utils.pause();
            return;
        }

        if (primaryDNS == null && secondaryDNS == null) {
            System.out.println("保持当前DNS设置不变。");
            Utils.pause();
            return;
        }

        // 更新系统DNS设置为所有接口
        updateSystemDNSForAllInterfaces(primaryDNS, secondaryDNS);
    }

    /**
     * 测试DNS服务器的响应时间和丢包率
     *
     * @param dns      DNS服务器地址
     * @param scanTime 扫描持续时间（秒）
     * @return DNS测试结果
     */
    private DNSResult testDNS(String dns, int scanTime) {
        int successfulPings = 0;
        long totalLatency = 0;

        for (int i = 0; i < PING_COUNT; i++) {
            try {
                InetAddress inetAddress = InetAddress.getByName(dns);
                long startTime = System.currentTimeMillis();
                boolean reachable = inetAddress.isReachable(scanTime * 1000 / PING_COUNT);
                long latency = System.currentTimeMillis() - startTime;
                if (reachable) {
                    successfulPings++;
                    totalLatency += latency;
                }
            } catch (IOException e) {
                // Ping失败，记录日志
                Utils.logDetail("Ping " + dns + " 失败: " + e.getMessage());
            }
        }

        double packetLoss = ((double) (PING_COUNT - successfulPings) / PING_COUNT) * 100;
        long averageLatency = (successfulPings > 0) ? (totalLatency / successfulPings) : Long.MAX_VALUE;

        return new DNSResult(dns, averageLatency, packetLoss);
    }

    /**
     * 验证是否为有效的 IPv4 地址
     *
     * @param ip IPv4 地址字符串
     * @return 如果有效则返回 true，否则返回 false
     */
    private boolean isValidIPAddress(String ip) {
        String regex = "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$";
        return ip.matches(regex);
    }

    /**
     * 获取当前活动的网络接口名称
     *
     * @return 网络接口名称，若未找到则返回 null
     */
    private String getActiveNetworkInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return null;
            }
            List<NetworkInterface> activeInterfaces = new ArrayList<>();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) {
                    activeInterfaces.add(ni);
                }
            }

            if (activeInterfaces.isEmpty()) {
                return null;
            } else if (activeInterfaces.size() == 1) {
                return activeInterfaces.get(0).getName(); // 返回唯一活跃的接口
            } else {
                // 如果有多个活动接口，让用户选择
                System.out.println("检测到多个活动的网络接口，请选择要设置DNS的接口：");
                int idx = 1;
                for (NetworkInterface ni : activeInterfaces) {
                    System.out.printf("[%d] %s%n", idx++, ni.getDisplayName());
                }
                System.out.print("请输入接口编号: ");
                String choice = Utils.getUserInput().trim();
                if (choice.equalsIgnoreCase("ESC")) {
                    return null;
                }
                try {
                    int selected = Integer.parseInt(choice);
                    if (selected < 1 || selected > activeInterfaces.size()) {
                        throw new NumberFormatException("编号超出范围。");
                    }
                    return activeInterfaces.get(selected - 1).getName();
                } catch (NumberFormatException e) {
                    System.out.println("无效的选择。");
                    Utils.logDetail("用户在选择网络接口时输入无效: " + choice);
                    return null;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            Utils.logDetail("获取网络接口失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取所有活动的网络接口名称
     *
     * @return 活动网络接口名称列表，若未找到则返回空列表
     */
    private List<String> getActiveNetworkInterfaces() {
        List<String> interfaces = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) {
                    interfaces.add(ni.getName());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            Utils.logDetail("获取网络接口失败: " + e.getMessage());
        }
        return interfaces;
    }

    /**
     * 更新系统的DNS设置
     *
     * @param primaryDNS   主DNS地址，如果为null则不设置主DNS
     * @param secondaryDNS 备用DNS地址，如果为null则不设置备用DNS
     */
    private void updateSystemDNS(String primaryDNS, String secondaryDNS) {
        String interfaceName = getActiveNetworkInterface();
        if (interfaceName == null) {
            System.out.println("未能检测到活动的网络接口。请手动设置DNS。");
            Utils.showWarning("未能检测到活动的网络接口。");
            Utils.logDetail("未能检测到活动的网络接口。");
            Utils.pause();
            return;
        }

        if (primaryDNS != null) {
            // 修改主DNS
            String command = String.format("netsh interface ipv4 set dns name=\"%s\" static %s primary", interfaceName, primaryDNS);
            Utils.executeCommand(command);
        }

        if (secondaryDNS != null) {
            // 设置备用DNS
            String secondaryCommand = String.format("netsh interface ipv4 add dns name=\"%s\" %s index=2", interfaceName, secondaryDNS);
            Utils.executeCommand(secondaryCommand);
        }

        String updatedPrimary = (primaryDNS != null) ? primaryDNS : "保持不变";
        String updatedSecondary = (secondaryDNS != null) ? secondaryDNS : "无";

        System.out.println("DNS已更新为: 主DNS: " + updatedPrimary + "，备用DNS: " + updatedSecondary);
        Utils.logEvent("DNS设置已更新，主DNS: " + updatedPrimary + "，备用DNS: " + updatedSecondary);
    }

    /**
     * 更新系统的DNS设置为所有接口
     *
     * @param primaryDNS   主DNS地址，如果为null则不设置主DNS
     * @param secondaryDNS 备用DNS地址，如果为null则不设置备用DNS
     */
    private void updateSystemDNSForAllInterfaces(String primaryDNS, String secondaryDNS) {
        List<String> activeInterfaces = getActiveNetworkInterfaces();
        if (activeInterfaces.isEmpty()) {
            System.out.println("未能检测到活动的网络接口。请手动设置DNS。");
            Utils.showWarning("未能检测到活动的网络接口。");
            Utils.logDetail("未能检测到活动的网络接口。");
            Utils.pause();
            return;
        }

        for (String interfaceName : activeInterfaces) {
            if (primaryDNS != null) {
                String command = String.format("netsh interface ipv4 set dns name=\"%s\" static %s primary", interfaceName, primaryDNS);
                Utils.executeCommand(command);
            }

            if (secondaryDNS != null) {
                String secondaryCommand = String.format("netsh interface ipv4 add dns name=\"%s\" %s index=2", interfaceName, secondaryDNS);
                Utils.executeCommand(secondaryCommand);
            }

            String updatedPrimary = (primaryDNS != null) ? primaryDNS : "保持不变";
            String updatedSecondary = (secondaryDNS != null) ? secondaryDNS : "无";

            System.out.println("接口 \"" + interfaceName + "\" 的DNS设置已更新。");
            Utils.logEvent("接口 \"" + interfaceName + "\" 的DNS设置已更新，主DNS: " + updatedPrimary + "，备用DNS: " + updatedSecondary);
        }

        System.out.println("所有活动的网络接口DNS设置已更新。");
        Utils.pause();
    }

    /**
     * DNS测试结果类
     */
    private static class DNSResult {
        private final String dns;
        private final long latency;
        private final double packetLoss;

        public DNSResult(String dns, long latency, double packetLoss) {
            this.dns = dns;
            this.latency = latency;
            this.packetLoss = packetLoss;
        }

        public String getDns() {
            return dns;
        }

        public long getLatency() {
            return latency;
        }

        public double getPacketLoss() {
            return packetLoss;
        }
    }

    /**
     * DNS条目类，包含DNS地址和相关关键词。
     */
    private static class DNSEntry {
        private final String dns;
        private final List<String> keywords;

        public DNSEntry(String dns, List<String> keywords) {
            this.dns = dns;
            this.keywords = keywords;
        }

        public String getDns() {
            return dns;
        }

        public List<String> getKeywords() {
            return keywords;
        }
    }
}
