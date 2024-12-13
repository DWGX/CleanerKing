// File: src/org/CleanerKing/modules/OneClickCleanModule.java
package org.CleanerKing.modules;

import org.CleanerKing.Module;
import org.CleanerKing.Utils;

/**
 * 一键清理模块，执行常见的清理任务。
 */
public class OneClickCleanModule implements Module {

    @Override
    public String getName() {
        return "一键清理";
    }

    @Override
    public void execute() {
        Utils.clearScreen();
        System.out.println("正在执行一键清理，请稍候...");

        Utils.logEvent("用户开始一键清理任务。");

        try {
            // 调用清理功能
            Utils.deleteDirectory(Utils.USER_TEMP);
            Utils.deleteDirectory(Utils.SYSTEM_TEMP);
            Utils.emptyRecycleBin();
            Utils.deleteDirectory(Utils.SOFTWARE_DISTRIBUTION);
            Utils.deleteDirectory(Utils.PREFETCH);
            Utils.deleteDirectory(Utils.RECENT_FILES);
            Utils.deleteDirectory(Utils.EDGE_CACHE);
            Utils.deleteDirectory(Utils.CHROME_CACHE);
            Utils.deleteFirefoxCache();
            Utils.flushDNS();

            System.out.println("一键清理完成！");
            Utils.logEvent("一键清理任务完成。");
        } catch (Exception e) {
            Utils.showWarning("一键清理过程中发生错误: " + e.getMessage());
            Utils.logDetail("一键清理过程中发生错误: " + e.getMessage());
        }
        Utils.pause();
    }
}
