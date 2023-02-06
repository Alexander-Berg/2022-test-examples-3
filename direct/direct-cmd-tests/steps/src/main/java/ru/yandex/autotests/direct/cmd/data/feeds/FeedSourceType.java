package ru.yandex.autotests.direct.cmd.data.feeds;

/**
 * Created by aleran on 28.08.2015.
 */
public enum FeedSourceType {
    URL("url"),
    FILE("file");

    private String value;

    FeedSourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
