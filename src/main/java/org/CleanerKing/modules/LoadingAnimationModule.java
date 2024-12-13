// File: src/org/CleanerKing/modules/LoadingAnimationModule.java
package org.CleanerKing.modules;

import org.CleanerKing.Module;
import org.CleanerKing.Utils;

/**
 * 加载动画模块，显示加载动画。
 */
public class LoadingAnimationModule implements Module {

    @Override
    public String getName() {
        return "显示加载动画";
    }

    @Override
    public void execute() {
        Utils.showLoadingAnimation();
    }
}
