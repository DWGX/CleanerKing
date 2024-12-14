
package org.CleanerKing.ModuleManager.Modules;

import org.CleanerKing.ModuleManager.Module;
import org.CleanerKing.Utils;

/**
 * 设置模块，允许用户更改全局颜色设置和日志选项。
 */
public class SettingsModule implements Module {

    @Override
    public String getName() {
        return "设置";
    }

    @Override
    public void execute() {
        while (true) {
            Utils.clearScreen();
            System.out.println("=========== 设置菜单 ===========");
            System.out.println("[1] 更改 3D ASCII 艺术标题颜色");
            System.out.println("[2] 更改加载动画颜色");
            System.out.println("[3] 启用/禁用日志保存");
            System.out.println("[4] 启用/禁用加载动画");
            System.out.println("[5] 返回主菜单");
            System.out.println("--------------------------------");
            System.out.print("请选择设置选项 (1-5): ");

            String choice = Utils.getUserInput().trim();

            switch (choice) {
                case "1":
                    changeAsciiArtColor();
                    break;
                case "2":
                    changeLoadingAnimationColor();
                    break;
                case "3":
                    toggleLogging();
                    break;
                case "4":
                    toggleLoadingAnimation();
                    break;
                case "5":
                    return;
                default:
                    System.out.println("无效选择，请重试。");
                    Utils.pause();
            }
        }
    }

    /**
     * 更改 3D ASCII 艺术标题颜色。
     */
    private void changeAsciiArtColor() {
        Utils.clearScreen();
        System.out.println("选择 3D ASCII 艺术标题颜色：");
        System.out.println("[1] 蓝色");
        System.out.println("[2] 绿色");
        System.out.println("[3] 红色");
        System.out.println("[4] 黄色");
        System.out.println("[5] 紫色");
        System.out.println("[6] 青色");
        System.out.println("[7] 白色");
        System.out.println("[8] 重置为默认");
        System.out.print("请选择颜色选项 (1-8): ");

        String colorChoice = Utils.getUserInput().trim();
        String selectedColor = null;
        String colorName = "";
        switch (colorChoice) {
            case "1":
                selectedColor = "\033[1;34m"; // 蓝色
                colorName = "蓝色";
                break;
            case "2":
                selectedColor = "\033[1;32m"; // 绿色
                colorName = "绿色";
                break;
            case "3":
                selectedColor = "\033[1;31m"; // 红色
                colorName = "红色";
                break;
            case "4":
                selectedColor = "\033[1;33m"; // 黄色
                colorName = "黄色";
                break;
            case "5":
                selectedColor = "\033[1;35m"; // 紫色
                colorName = "紫色";
                break;
            case "6":
                selectedColor = "\033[1;36m"; // 青色
                colorName = "青色";
                break;
            case "7":
                selectedColor = "\033[1;37m"; // 白色
                colorName = "白色";
                break;
            case "8":
                selectedColor = "\033[0m"; // 默认
                colorName = "默认";
                break;
            default:
                System.out.println("无效选择，请重试。");
                Utils.pause();
                return;
        }

        Utils.setAsciiArtColor(selectedColor);
        System.out.println("3D ASCII 艺术标题颜色已设置为" + colorName + "。");
        Utils.logEvent("用户更改3D ASCII艺术标题颜色为" + colorName + "。");

        // 立即应用颜色更改：重新显示ASCII艺术标题
        Utils.clearScreen();
        Utils.display3DASCII();
        Utils.pause();
    }

    /**
     * 更改加载动画颜色。
     */
    private void changeLoadingAnimationColor() {
        Utils.clearScreen();
        System.out.println("选择加载动画颜色：");
        System.out.println("[1] 绿色");
        System.out.println("[2] 黄色");
        System.out.println("[3] 红色");
        System.out.println("[4] 蓝色");
        System.out.println("[5] 紫色");
        System.out.println("[6] 青色");
        System.out.println("[7] 白色");
        System.out.println("[8] 重置为默认");
        System.out.print("请选择颜色选项 (1-8): ");

        String colorChoice = Utils.getUserInput().trim();
        String selectedColor = null;
        String colorName = "";
        switch (colorChoice) {
            case "1":
                selectedColor = "\033[1;32m"; // 绿色
                colorName = "绿色";
                break;
            case "2":
                selectedColor = "\033[1;33m"; // 黄色
                colorName = "黄色";
                break;
            case "3":
                selectedColor = "\033[1;31m"; // 红色
                colorName = "红色";
                break;
            case "4":
                selectedColor = "\033[1;34m"; // 蓝色
                colorName = "蓝色";
                break;
            case "5":
                selectedColor = "\033[1;35m"; // 紫色
                colorName = "紫色";
                break;
            case "6":
                selectedColor = "\033[1;36m"; // 青色
                colorName = "青色";
                break;
            case "7":
                selectedColor = "\033[1;37m"; // 白色
                colorName = "白色";
                break;
            case "8":
                selectedColor = "\033[0m"; // 默认
                colorName = "默认";
                break;
            default:
                System.out.println("无效选择，请重试。");
                Utils.pause();
                return;
        }

        Utils.setLoadingAnimationColor(selectedColor);
        System.out.println("加载动画颜色已设置为" + colorName + "。");
        Utils.logEvent("用户更改加载动画颜色为" + colorName + "。");

        Utils.pause();
    }

    /**
     * 启用或禁用日志保存。
     */
    private void toggleLogging() {
        Utils.clearScreen();
        System.out.println("当前日志保存状态: " + (Utils.isLoggingEnabled() ? "启用" : "禁用"));
        System.out.println("[1] 启用日志保存");
        System.out.println("[2] 禁用日志保存");
        System.out.println("[3] 返回设置菜单");
        System.out.print("请选择操作 (1-3): ");

        String choice = Utils.getUserInput().trim();
        switch (choice) {
            case "1":
                Utils.enableLogging(true);
                System.out.println("日志保存已启用。");
                Utils.logEvent("用户启用了日志保存。");
                break;
            case "2":
                Utils.enableLogging(false);
                System.out.println("日志保存已禁用。");
                Utils.logEvent("用户禁用了日志保存。");
                break;
            case "3":
                return;
            default:
                System.out.println("无效选择，请重试。");
        }
        Utils.pause();
    }

    /**
     * 启用或禁用加载动画。
     */
    private void toggleLoadingAnimation() {
        Utils.clearScreen();
        System.out.println("当前加载动画状态: " + (Utils.isShowLoadingAnimation() ? "启用" : "禁用"));
        System.out.println("[1] 启用加载动画");
        System.out.println("[2] 禁用加载动画");
        System.out.println("[3] 返回设置菜单");
        System.out.print("请选择操作 (1-3): ");

        String choice = Utils.getUserInput().trim();
        switch (choice) {
            case "1":
                Utils.setShowLoadingAnimation(true);
                System.out.println("加载动画已启用。");
                Utils.logEvent("用户启用了加载动画。");
                break;
            case "2":
                Utils.setShowLoadingAnimation(false);
                System.out.println("加载动画已禁用。");
                Utils.logEvent("用户禁用了加载动画。");
                break;
            case "3":
                return;
            default:
                System.out.println("无效选择，请重试。");
        }
        Utils.pause();
    }
}
