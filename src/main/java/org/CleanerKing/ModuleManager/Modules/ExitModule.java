package org.CleanerKing.ModuleManager.Modules;

import org.CleanerKing.ModuleManager.Module;
import org.CleanerKing.Utils;

/**
 * 退出模块，允许用户退出程序。
 */
public class ExitModule implements Module {

    @Override
    public String getName() {
        return "退出";
    }

    @Override
    public void execute() {
        Utils.clearScreen();
        System.out.print("确认退出程序？(Y=确认, N=取消, ESC=返回主菜单): ");
        String confirm = Utils.scanner.nextLine().trim().toUpperCase();
        switch (confirm) {
            case "Y":
                Utils.logEvent("用户确认退出程序。");
                System.out.println("已退出程序。感谢使用！");
                Utils.closeScanner();
                System.exit(0);
                break;
            case "N":
                Utils.logEvent("用户取消退出操作。");
                System.out.println("取消退出操作。");
                Utils.pause();
                break;
            case "ESC":
                Utils.logDetail("用户选择返回主菜单。");
                break;
            default:
                Utils.showWarning("无效输入，请重试。");
                Utils.logDetail("用户输入无效的退出确认选项: " + confirm);
                Utils.pause();
        }
    }
}
