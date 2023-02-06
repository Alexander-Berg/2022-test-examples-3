package ru.yandex.downloader.url;

/**
 * @author akirakozov
 */
public class StringTargetId implements TargetReference {
    private final String value;

    public StringTargetId(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
