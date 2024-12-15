package org.CleanerKing;

import java.io.*;
import java.util.Properties;

/**
 * Settings类，用于管理应用程序的配置设置。
 */
public class Settings {
    private static final String CONFIG_FILE = "config.properties";
    private Properties properties;

    // 默认设置
    private String asciiArtColor = "\033[1;34m"; // 蓝色
    private String loadingAnimationColor = "\033[1;32m"; // 绿色
    private boolean loggingEnabled = true;
    private boolean showLoadingAnimation = true;

    public Settings() {
        properties = new Properties();
        loadSettings();
    }

    /**
     * 加载设置从配置文件，如果配置文件不存在，则使用默认设置。
     */
    public void loadSettings() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile);
                 InputStreamReader isr = new InputStreamReader(fis, "UTF-8")) { // 使用UTF-8编码
                properties.load(isr);
                asciiArtColor = properties.getProperty("asciiArtColor", asciiArtColor);
                loadingAnimationColor = properties.getProperty("loadingAnimationColor", loadingAnimationColor);
                loggingEnabled = Boolean.parseBoolean(properties.getProperty("loggingEnabled", String.valueOf(loggingEnabled)));
                showLoadingAnimation = Boolean.parseBoolean(properties.getProperty("showLoadingAnimation", String.valueOf(showLoadingAnimation)));
            } catch (IOException e) {
                System.out.println("无法加载配置文件: " + e.getMessage());
            }
        } else {
            saveSettings(); // 如果配置文件不存在，创建一个
        }
    }

    /**
     * 保存当前设置到配置文件。
     */
    public void saveSettings() {
        properties.setProperty("asciiArtColor", asciiArtColor);
        properties.setProperty("loadingAnimationColor", loadingAnimationColor);
        properties.setProperty("loggingEnabled", String.valueOf(loggingEnabled));
        properties.setProperty("showLoadingAnimation", String.valueOf(showLoadingAnimation));

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
             OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8")) { // 使用UTF-8编码
            properties.store(osw, "CleanerKing Configuration");
        } catch (IOException e) {
            System.out.println("无法保存配置文件: " + e.getMessage());
        }
    }

    // Getters and Setters
    public String getAsciiArtColor() {
        return asciiArtColor;
    }

    public void setAsciiArtColor(String asciiArtColor) {
        this.asciiArtColor = asciiArtColor;
        saveSettings();
    }

    public String getLoadingAnimationColor() {
        return loadingAnimationColor;
    }

    public void setLoadingAnimationColor(String loadingAnimationColor) {
        this.loadingAnimationColor = loadingAnimationColor;
        saveSettings();
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
        saveSettings();
    }

    public boolean isShowLoadingAnimation() {
        return showLoadingAnimation;
    }

    public void setShowLoadingAnimation(boolean showLoadingAnimation) {
        this.showLoadingAnimation = showLoadingAnimation;
        saveSettings();
    }
}
