// File: src/org/CleanerKing/modules/WindowsToolsModule.java
package org.CleanerKing.modules;

import org.CleanerKing.Module;
import org.CleanerKing.Utils;

/**
 * Windows内置工具菜单模块，集成更多的Windows系统工具。
 */
public class WindowsToolsModule implements Module {

    @Override
    public String getName() {
        return "Windows内置工具菜单";
    }

    @Override
    public void execute() {
        while (true) {
            Utils.clearScreen();
            System.out.println("=========== Windows内置工具菜单 ===========");
            System.out.println("[1] 磁盘清理工具 (cleanmgr)");
            System.out.println("[2] 磁盘管理 (diskmgmt.msc)");
            System.out.println("[3] 系统配置 (msconfig)");
            System.out.println("[4] 系统信息 (msinfo32)");
            System.out.println("[5] 碎片整理 (dfrgui)");
            System.out.println("[6] 检查磁盘 (chkdsk)");
            System.out.println("[7] 任务管理器 (taskmgr)");
            System.out.println("[8] 资源监视器 (resmon)");
            System.out.println("[ESC] 返回主菜单");
            System.out.println("---------------------------------------------");
            System.out.print("请选择工具 (1-8 或 ESC): ");
            String toolSel = Utils.getUserInput().trim().toUpperCase();
            if (toolSel.equals("ESC")) {
                Utils.logDetail("用户选择返回主菜单。");
                return;
            }

            switch (toolSel) {
                case "1":
                    Utils.executeCommand("cleanmgr");
                    Utils.logEvent("用户启动磁盘清理工具 (cleanmgr)。");
                    break;
                case "2":
                    Utils.executeCommand("cmd.exe /c start diskmgmt.msc");
                    Utils.logEvent("用户启动磁盘管理 (diskmgmt.msc)。");
                    break;
                case "3":
                    Utils.executeCommand("cmd.exe /c start msconfig");
                    Utils.logEvent("用户启动系统配置 (msconfig)。");
                    break;
                case "4":
                    Utils.executeCommand("cmd.exe /c start msinfo32");
                    Utils.logEvent("用户启动系统信息 (msinfo32)。");
                    break;
                case "5":
                    Utils.executeCommand("cmd.exe /c start dfrgui");
                    Utils.logEvent("用户启动碎片整理 (dfrgui)。");
                    break;
                case "6":
                    Utils.chkdskMenu();
                    break;
                case "7":
                    Utils.executeCommand("cmd.exe /c start taskmgr");
                    Utils.logEvent("用户启动任务管理器 (taskmgr)。");
                    break;
                case "8":
                    Utils.executeCommand("cmd.exe /c start resmon");
                    Utils.logEvent("用户启动资源监视器 (resmon)。");
                    break;
                default:
                    Utils.showWarning("无效选择: " + toolSel);
                    Utils.logDetail("用户输入无效的Windows工具选择: " + toolSel);
            }
        }
    }
}
