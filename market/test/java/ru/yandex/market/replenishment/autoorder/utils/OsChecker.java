package ru.yandex.market.replenishment.autoorder.utils;

public class OsChecker {
    public static String getOsType() {
        String osName = System.getProperty("os.name", "generic").toLowerCase();
        if (osName.contains("mac") || osName.contains("darwin")) return "darwin";
        if (osName.contains("win")) return "windows";
        if (osName.contains("linux")) return "linux";
        return "other";
    }
}
