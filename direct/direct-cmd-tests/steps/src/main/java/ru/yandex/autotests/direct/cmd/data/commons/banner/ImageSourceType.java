package ru.yandex.autotests.direct.cmd.data.commons.banner;

public enum ImageSourceType {
    FILE, URL;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
