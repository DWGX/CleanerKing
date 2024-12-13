// File: src/org/CleanerKing/Client.java
package org.CleanerKing;

import org.CleanerKing.modules.*;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * 主类，启动全能电脑清理工具。
 */
public class Client {
    public static void main(String[] args) {
        // 设置输出编码为UTF-8
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 启用日志（默认禁用，用户可在设置中启用）
        Utils.enableLogging(false);

        // 创建 ModuleManager 并注册模块
        ModuleManager moduleManager = new ModuleManager();

        // 注册各个功能模块
        moduleManager.registerModule("1", new OneClickCleanModule());
        moduleManager.registerModule("2", new AdvancedCleanModule());
        moduleManager.registerModule("3", new WindowsToolsModule());
        moduleManager.registerModule("4", new LargeFileScanDeleteModule());
        moduleManager.registerModule("5", new FileSearchModule());
        moduleManager.registerModule("6", new ExitModule());
        moduleManager.registerModule("7", new SettingsModule());

        // 显示 3D ASCII 艺术标题
        Module displayAsciiModule = new Display3DASCIIModule();
        displayAsciiModule.execute();

        // 显示加载动画，如果设置允许
        if (Utils.isShowLoadingAnimation()) {
            Module loadingAnimationModule = new LoadingAnimationModule();
            loadingAnimationModule.execute();
        }

        // 启动主菜单
        moduleManager.showMainMenu();
    }
}
