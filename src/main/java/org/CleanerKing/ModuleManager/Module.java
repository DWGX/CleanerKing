package org.CleanerKing.ModuleManager;

/**
 * 模块接口，所有功能模块必须实现此接口。
 */
public interface Module {
    /**
     * 获取模块的名称，用于菜单显示。
     *
     * @return 模块名称。
     */
    String getName();

    /**
     * 执行模块的主要功能。
     */
    void execute();
}
