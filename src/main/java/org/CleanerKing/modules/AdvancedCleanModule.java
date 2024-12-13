// File: src/org/CleanerKing/modules/AdvancedCleanModule.java
package org.CleanerKing.modules;

import org.CleanerKing.Module;
import org.CleanerKing.Utils;

/**
 * 高级清理模块，允许用户选择具体的清理选项。
 */
public class AdvancedCleanModule implements Module {

    @Override
    public String getName() {
        return "高级清理";
    }

    @Override
    public void execute() {
        Utils.advancedClean();
    }
}
