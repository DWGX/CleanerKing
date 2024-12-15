package org.CleanerKing;

import org.CleanerKing.ModuleManager.ModuleManager;
import org.CleanerKing.ModuleManager.Modules.*;

public class Client {
    public static void main(String[] args) {
        // 初始化工具类
        Utils.clearScreen();
        Utils.display3DASCII();

        // 显示加载动画
        Utils.showLoadingAnimation();

        ModuleManager moduleManager = new ModuleManager();

        // 注册模块
        moduleManager.registerModule("1", new OneClickCleanModule());
        moduleManager.registerModule("2", new AdvancedCleanModule());
        moduleManager.registerModule("3", new WindowsToolsModule());
        moduleManager.registerModule("4", new FileSearchModule());
        moduleManager.registerModule("5", new DNSModule());
        moduleManager.registerModule("6", new NetworkRepairModule()); // 新增网络修复模块
        moduleManager.registerModule("7", new SettingsModule());
        moduleManager.registerModule("8", new ExitModule());

        moduleManager.showMainMenu(); // 调用模块管理器显示主菜单
    }
}
