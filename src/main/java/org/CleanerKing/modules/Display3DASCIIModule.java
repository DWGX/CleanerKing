// File: src/org/CleanerKing/modules/Display3DASCIIModule.java
package org.CleanerKing.modules;

import org.CleanerKing.Module;
import org.CleanerKing.Utils;

/**
 * 显示 3D ASCII 艺术标题模块，添加颜色。
 */
public class Display3DASCIIModule implements Module {

    @Override
    public String getName() {
        return "显示 3D ASCII 艺术标题";
    }

    @Override
    public void execute() {
        Utils.display3DASCII();
        // Utils.pause(); // 保留
    }
}
