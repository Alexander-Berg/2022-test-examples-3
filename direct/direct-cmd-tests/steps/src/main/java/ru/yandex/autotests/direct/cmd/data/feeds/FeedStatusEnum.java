package ru.yandex.autotests.direct.cmd.data.feeds;

/**
 * Created by aleran on 02.09.2015.
 */
public enum FeedStatusEnum {
    NEW("New"),
    OUTDATED("Outdated"),
    UPDATING("Updating"),
    DONE("Done"),
    ERROR("Error");

    private String text;

    FeedStatusEnum(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
