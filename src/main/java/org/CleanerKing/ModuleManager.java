// File: src/org/CleanerKing/ModuleManager.java
package org.CleanerKing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 模块管理器，负责注册和管理所有模块。
 */
public class ModuleManager {
    private Map<String, Module> modules = new LinkedHashMap<>();
    private Scanner scanner = new Scanner(System.in);

    /**
     * 注册一个模块，键为菜单选项
     * @param key 菜单选项
     * @param module 模块实例
     */
    public void registerModule(String key, Module module) {
        modules.put(key, module);
    }

    /**
     * 显示主菜单并处理用户选择
     */
    public void showMainMenu() {
        while (true) {
            Utils.clearScreen();
            System.out.println("====================================================");
            System.out.println("                 全能电脑清理工具");
            System.out.println("====================================================");
            for (Map.Entry<String, Module> entry : modules.entrySet()) {
                System.out.printf("[%s] %s%n", entry.getKey(), entry.getValue().getName());
            }
            System.out.println("----------------------------------------------------");
            System.out.print("请选择功能 (输入对应数字，或输入 ESC 返回/退出): ");

            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("ESC")) {
                System.out.print("确认退出程序？(Y/N): ");
                String confirm = scanner.nextLine().trim().toUpperCase();
                if (confirm.equals("Y")) {
                    System.out.println("已退出程序。感谢使用！");
                    System.exit(0);
                } else {
                    continue;
                }
            }

            Module selectedModule = modules.get(choice);
            if (selectedModule != null) {
                selectedModule.execute();
            } else {
                System.out.println("无效选择，请重新输入。");
                Utils.pause();
            }
        }
    }
}
