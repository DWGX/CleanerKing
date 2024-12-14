
package org.CleanerKing.ModuleManager.Modules;

import org.CleanerKing.ModuleManager.Module;
import org.CleanerKing.Utils;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;

/**
 * DNS 设置模块，允许用户自动选择最快最稳定的 DNS，手动输入 DNS 地址，
 * 或者一次性为所有活动的网络接口设置 DNS。
 */
public class DNSModule implements Module {

    // 扩展后的预定义 DNS 服务器列表
    private static final List<String> DNS_SERVERS = Arrays.asList(
            "1.1.1.1", // Cloudflare DNS
            "1.0.0.1", // Cloudflare DNS
            "8.8.8.8", // Google DNS
            "8.8.4.4", // Google DNS
            "208.67.222.222", // OpenDNS
            "208.67.220.220", // OpenDNS
            "114.114.114.114", // 114 DNS
            "114.114.115.115", // 114 DNS
            "223.5.5.5", // 阿里云 DNS
            "180.76.76.76", // 百度 DNS
            "119.29.29.29", // 腾讯 DNS
            "9.9.9.9", // Quad9 DNS
            "149.112.112.112" // Quad9 DNS
    );

    // 每个 DNS 服务器的 Ping 测试次数
    private static final int PING_COUNT = 5;

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
                System.out.println("无效选择，请重新输入。");
                Utils.pause();
                execute();
                break;
        }
    }

    /**
     * 自动选择最快且最稳定的 DNS
     */
    private void autoSelectDNS() {
        System.out.print("请输入每个 DNS 服务器的扫描时间（秒，推荐5-10秒）: ");
        String input = Utils.getUserInput().trim();
        int scanTime;
        try {
            scanTime = Integer.parseInt(input);
            if (scanTime <= 0) {
                throw new NumberFormatException("扫描时间必须为正整数。");
            }
        } catch (NumberFormatException e) {
            System.out.println("无效的输入，请输入一个正整数。");
            Utils.pause();
            execute();
            return;
        }

        System.out.println("正在测试各个 DNS 服务器的响应时间和稳定性...");

        // 存储每个 DNS 的响应时间和丢包率
        List<DNSResult> results = Collections.synchronizedList(new ArrayList<>());

        // 使用线程池并发测试 DNS
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(DNS_SERVERS.size(), 20));
        List<Callable<Void>> tasks = new ArrayList<>();

        for (String dns : DNS_SERVERS) {
            tasks.add(() -> {
                DNSResult result = testDNS(dns, scanTime);
                results.add(result);
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Utils.showWarning("DNS 测试被中断。");
            Utils.logDetail("DNS 测试被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            return;
        } finally {
            executor.shutdown();
        }

        if (results.isEmpty()) {
            System.out.println("未能测试任何 DNS 服务器。请检查网络连接。");
            Utils.pause();
            execute();
            return;
        }

        // 排序结果：首先按丢包率升序，其次按响应时间升序
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
        System.out.println("序号对应的 DNS 服务器按速度和稳定性排序，最快且最稳定的 DNS 排在最前面。");
        System.out.println("您可以单独设置主 DNS 或备用 DNS，或同时设置。");
        System.out.println("若不想修改主 DNS 或备用 DNS，请直接按回车键跳过。");

        // 设置主 DNS
        System.out.print("请输入主 DNS 的序号 (例如 1)，或直接按回车键跳过设置主 DNS: ");
        String primaryChoice = Utils.getUserInput().trim();
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
                execute();
                return;
            }
        }

        // 设置备用 DNS
        System.out.print("请输入备用 DNS 的序号 (例如 2)，或直接按回车键跳过设置备用 DNS: ");
        String secondaryChoice = Utils.getUserInput().trim();
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
                execute();
                return;
            }
        }

        // 更新系统 DNS 设置
        if (primaryDNS != null || secondaryDNS != null) {
            updateSystemDNS(primaryDNS != null ? primaryDNS.getDns() : null,
                    secondaryDNS != null ? secondaryDNS.getDns() : null);
        } else {
            System.out.println("保持当前 DNS 设置不变。");
            Utils.pause();
        }
    }

    /**
     * 手动输入 DNS 地址并设置
     */
    private void manualInputDNS() {
        System.out.println("您可以单独设置主 DNS 或备用 DNS，或同时设置。");
        System.out.println("若不想修改主 DNS 或备用 DNS，请直接按回车键跳过。");

        // 输入主 DNS
        System.out.print("请输入主 DNS 地址: ");
        String primaryDNS = Utils.getUserInput().trim();
        if (primaryDNS.isEmpty()) {
            primaryDNS = null;
        } else if (!isValidIPAddress(primaryDNS)) {
            System.out.println("无效的主 DNS 地址格式。");
            Utils.pause();
            execute();
            return;
        }

        // 输入备用 DNS
        System.out.print("请输入备用 DNS 地址: ");
        String secondaryDNS = Utils.getUserInput().trim();
        if (secondaryDNS.isEmpty()) {
            secondaryDNS = null;
        } else if (!isValidIPAddress(secondaryDNS)) {
            System.out.println("无效的备用 DNS 地址格式。");
            Utils.pause();
            execute();
            return;
        }

        if (primaryDNS == null && secondaryDNS == null) {
            System.out.println("保持当前 DNS 设置不变。");
            Utils.pause();
            return;
        }

        // 更新系统 DNS 设置
        updateSystemDNS(primaryDNS, secondaryDNS);
    }

    /**
     * 为所有活动的网络接口设置 DNS
     */
    private void setDNSForAllInterfaces() {
        System.out.println("您将为所有活动的网络接口设置 DNS。");
        System.out.println("若不想设置主 DNS 或备用 DNS，请直接按回车键跳过。");

        // 输入主 DNS
        System.out.print("请输入主 DNS 地址: ");
        String primaryDNS = Utils.getUserInput().trim();
        if (primaryDNS.isEmpty()) {
            primaryDNS = null;
        } else if (!isValidIPAddress(primaryDNS)) {
            System.out.println("无效的主 DNS 地址格式。");
            Utils.pause();
            execute();
            return;
        }

        // 输入备用 DNS
        System.out.print("请输入备用 DNS 地址: ");
        String secondaryDNS = Utils.getUserInput().trim();
        if (secondaryDNS.isEmpty()) {
            secondaryDNS = null;
        } else if (!isValidIPAddress(secondaryDNS)) {
            System.out.println("无效的备用 DNS 地址格式。");
            Utils.pause();
            execute();
            return;
        }

        if (primaryDNS == null && secondaryDNS == null) {
            System.out.println("保持当前 DNS 设置不变。");
            Utils.pause();
            return;
        }

        // 更新系统 DNS 设置为所有接口
        updateSystemDNSForAllInterfaces(primaryDNS, secondaryDNS);
    }

    /**
     * 测试 DNS 服务器的响应时间和丢包率
     *
     * @param dns      DNS 服务器地址
     * @param scanTime 扫描持续时间（秒）
     * @return DNS 测试结果
     */
    private DNSResult testDNS(String dns, int scanTime) {
        int totalPings = PING_COUNT;
        int successfulPings = 0;
        long totalLatency = 0;

        for (int i = 0; i < PING_COUNT; i++) {
            try {
                InetAddress inetAddress = InetAddress.getByName(dns);
                long startTime = System.currentTimeMillis();
                boolean reachable = inetAddress.isReachable(scanTime * 1000 / PING_COUNT); // 分配时间
                long latency = System.currentTimeMillis() - startTime;
                if (reachable) {
                    successfulPings++;
                    totalLatency += latency;
                }
            } catch (IOException e) {
                // Ping 失败，记录日志
                Utils.logDetail("Ping " + dns + " 失败: " + e.getMessage());
            }
        }

        double packetLoss = ((double) (PING_COUNT - successfulPings) / PING_COUNT) * 100;
        long averageLatency = (successfulPings > 0) ? (totalLatency / successfulPings) : Long.MAX_VALUE;

        return new DNSResult(dns, averageLatency, packetLoss);
    }

    /**
     * 更新系统的 DNS 设置
     *
     * @param primaryDNS   主 DNS 地址，如果为 null 则不设置主 DNS
     * @param secondaryDNS 备用 DNS 地址，如果为 null 则不设置备用 DNS
     */
    private void updateSystemDNS(String primaryDNS, String secondaryDNS) {
        String interfaceName = getActiveNetworkInterface();
        if (interfaceName == null) {
            System.out.println("未能检测到活动的网络接口。请手动设置 DNS。");
            Utils.showWarning("未能检测到活动的网络接口。");
            Utils.logDetail("未能检测到活动的网络接口。");
            Utils.pause();
            return;
        }

        try {
            if (primaryDNS != null) {
                // 修改主 DNS
                String command = String.format("netsh interface ipv4 set dns name=\"%s\" static %s primary", interfaceName, primaryDNS);
                executeCommand(command);
            }

            if (secondaryDNS != null) {
                // 如果提供了备用 DNS，设置备用 DNS
                String secondaryCommand = String.format("netsh interface ipv4 add dns name=\"%s\" %s index=2", interfaceName, secondaryDNS);
                executeCommand(secondaryCommand);
            }

            String updatedPrimary = (primaryDNS != null) ? primaryDNS : "保持不变";
            String updatedSecondary = (secondaryDNS != null) ? secondaryDNS : "无";

            System.out.println("DNS 已更新为: 主 DNS: " + updatedPrimary + "，备用 DNS: " + updatedSecondary);
            Utils.logEvent("DNS 设置已更新，主 DNS: " + updatedPrimary + "，备用 DNS: " + updatedSecondary);
        } catch (IOException e) {
            e.printStackTrace();
            Utils.showWarning("更新 DNS 失败，请检查是否具有管理员权限！");
            Utils.logDetail("更新 DNS 失败: " + e.getMessage());
        }
    }

    /**
     * 为所有活动的网络接口设置 DNS
     *
     * @param primaryDNS   主 DNS 地址，如果为 null 则不设置主 DNS
     * @param secondaryDNS 备用 DNS 地址，如果为 null 则不设置备用 DNS
     */
    private void updateSystemDNSForAllInterfaces(String primaryDNS, String secondaryDNS) {
        List<String> activeInterfaces = getActiveNetworkInterfaces();
        if (activeInterfaces.isEmpty()) {
            System.out.println("未能检测到活动的网络接口。请手动设置 DNS。");
            Utils.showWarning("未能检测到活动的网络接口。");
            Utils.logDetail("未能检测到活动的网络接口。");
            Utils.pause();
            return;
        }

        for (String interfaceName : activeInterfaces) {
            try {
                if (primaryDNS != null) {
                    String command = String.format("netsh interface ipv4 set dns name=\"%s\" static %s primary", interfaceName, primaryDNS);
                    executeCommand(command);
                }

                if (secondaryDNS != null) {
                    String secondaryCommand = String.format("netsh interface ipv4 add dns name=\"%s\" %s index=2", interfaceName, secondaryDNS);
                    executeCommand(secondaryCommand);
                }

                String updatedPrimary = (primaryDNS != null) ? primaryDNS : "保持不变";
                String updatedSecondary = (secondaryDNS != null) ? secondaryDNS : "无";

                System.out.println("接口 \"" + interfaceName + "\" 的 DNS 设置已更新。");
                Utils.logEvent("接口 \"" + interfaceName + "\" 的 DNS 设置已更新，主 DNS: " + updatedPrimary + "，备用 DNS: " + updatedSecondary);
            } catch (IOException e) {
                e.printStackTrace();
                Utils.showWarning("更新 DNS 失败，请检查是否具有管理员权限！");
                Utils.logDetail("更新 DNS 失败: " + e.getMessage());
            }
        }

        System.out.println("所有活动的网络接口 DNS 设置已更新。");
        Utils.pause();
    }

    /**
     * 执行命令行命令
     *
     * @param command 命令字符串
     * @throws IOException 执行命令时的异常
     */
    private void executeCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) { // 使用UTF-8编码
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        try {
            int exitCode = process.waitFor(); // 捕获 InterruptedException
            if (exitCode != 0) {
                Utils.showWarning("执行命令失败: " + command);
                Utils.logDetail("执行命令失败: " + command + "，退出代码: " + exitCode);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
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
                return activeInterfaces.get(0).getName(); // 返回接口名称
            } else {
                // 如果有多个活动接口，让用户选择
                System.out.println("检测到多个活动的网络接口，请选择要设置 DNS 的接口：");
                int idx = 1;
                for (NetworkInterface ni : activeInterfaces) {
                    System.out.printf("[%d] %s%n", idx++, ni.getDisplayName());
                }
                System.out.print("请输入接口编号: ");
                String choice = Utils.getUserInput().trim();
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
     * DNS 测试结果类
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
}
