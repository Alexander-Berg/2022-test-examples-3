package ru.yandex.autotests.market.stat.beans;

/**
 * Created by jkt on 02.07.14.
 */
public enum Packages {

    DICTIONARIES_YT("yandex-market-dictionaries-yt");

    private String packageName;

    Packages(String packageName) {
        this.packageName = packageName;
    }

    public String asString() {
        return packageName;
    }
}
