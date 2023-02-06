package ru.yandex.direct.internaltools.tools.testtool.container;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TestToolResultInfo {
    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public TestToolResultInfo withKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TestToolResultInfo withValue(String value) {
        this.value = value;
        return this;
    }
}
