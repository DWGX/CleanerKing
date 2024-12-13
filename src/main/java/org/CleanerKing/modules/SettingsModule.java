// File: src/org/CleanerKing/modules/SettingsModule.java
package org.CleanerKing.modules;

import org.CleanerKing.Module;
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

            String choice = Utils.scanner.nextLine().trim();

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

        String colorChoice = Utils.scanner.nextLine().trim();
        switch (colorChoice) {
            case "1":
                Utils.setAsciiArtColor("\033[1;34m"); // 蓝色
                System.out.println("3D ASCII 艺术标题颜色已设置为蓝色。");
                Utils.logEvent("用户更改3D ASCII艺术标题颜色为蓝色。");
                break;
            case "2":
                Utils.setAsciiArtColor("\033[1;32m"); // 绿色
                System.out.println("3D ASCII 艺术标题颜色已设置为绿色。");
                Utils.logEvent("用户更改3D ASCII艺术标题颜色为绿色。");
                break;
            case "3":
                Utils.setAsciiArtColor("\033[1;31m"); // 红色
                System.out.println("3D ASCII 艺术标题颜色已设置为红色。");
                Utils.logEvent("用户更改3D ASCII艺术标题颜色为红色。");
                break;
            case "4":
                Utils.setAsciiArtColor("\033[1;33m"); // 黄色
                System.out.println("3D ASCII 艺术标题颜色已设置为黄色。");
                Utils.logEvent("用户更改3D ASCII艺术标题颜色为黄色。");
                break;
            case "5":
                Utils.setAsciiArtColor("\033[1;35m"); // 紫色
                System.out.println("3D ASCII 艺术标题颜色已设置为紫色。");
                Utils.logEvent("用户更改3D ASCII艺术标题颜色为紫色。");
                break;
            case "6":
                Utils.setAsciiArtColor("\033[1;36m"); // 青色
                System.out.println("3D ASCII 艺术标题颜色已设置为青色。");
                Utils.logEvent("用户更改3D ASCII艺术标题颜色为青色。");
                break;
            case "7":
                Utils.setAsciiArtColor("\033[1;37m"); // 白色
                System.out.println("3D ASCII 艺术标题颜色已设置为白色。");
                Utils.logEvent("用户更改3D ASCII艺术标题颜色为白色。");
                break;
            case "8":
                Utils.setAsciiArtColor("\033[0m"); // 默认
                System.out.println("3D ASCII 艺术标题颜色已重置为默认。");
                Utils.logEvent("用户重置3D ASCII艺术标题颜色为默认。");
                break;
            default:
                System.out.println("无效选择，请重试。");
        }
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

        String colorChoice = Utils.scanner.nextLine().trim();
        switch (colorChoice) {
            case "1":
                Utils.setLoadingAnimationColor("\033[1;32m"); // 绿色
                System.out.println("加载动画颜色已设置为绿色。");
                Utils.logEvent("用户更改加载动画颜色为绿色。");
                break;
            case "2":
                Utils.setLoadingAnimationColor("\033[1;33m"); // 黄色
                System.out.println("加载动画颜色已设置为黄色。");
                Utils.logEvent("用户更改加载动画颜色为黄色。");
                break;
            case "3":
                Utils.setLoadingAnimationColor("\033[1;31m"); // 红色
                System.out.println("加载动画颜色已设置为红色。");
                Utils.logEvent("用户更改加载动画颜色为红色。");
                break;
            case "4":
                Utils.setLoadingAnimationColor("\033[1;34m"); // 蓝色
                System.out.println("加载动画颜色已设置为蓝色。");
                Utils.logEvent("用户更改加载动画颜色为蓝色。");
                break;
            case "5":
                Utils.setLoadingAnimationColor("\033[1;35m"); // 紫色
                System.out.println("加载动画颜色已设置为紫色。");
                Utils.logEvent("用户更改加载动画颜色为紫色。");
                break;
            case "6":
                Utils.setLoadingAnimationColor("\033[1;36m"); // 青色
                System.out.println("加载动画颜色已设置为青色。");
                Utils.logEvent("用户更改加载动画颜色为青色。");
                break;
            case "7":
                Utils.setLoadingAnimationColor("\033[1;37m"); // 白色
                System.out.println("加载动画颜色已设置为白色。");
                Utils.logEvent("用户更改加载动画颜色为白色。");
                break;
            case "8":
                Utils.setLoadingAnimationColor("\033[0m"); // 默认
                System.out.println("加载动画颜色已重置为默认。");
                Utils.logEvent("用户重置加载动画颜色为默认。");
                break;
            default:
                System.out.println("无效选择，请重试。");
        }
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

        String choice = Utils.scanner.nextLine().trim();
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

        String choice = Utils.scanner.nextLine().trim();
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
