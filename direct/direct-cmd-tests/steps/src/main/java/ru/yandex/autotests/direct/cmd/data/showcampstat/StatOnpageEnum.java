package ru.yandex.autotests.direct.cmd.data.showcampstat;

public enum StatOnpageEnum {
    TEN("10"),
    HUNDRED("100"),
    THOUSAND("1000"),
    TEN_THOUSANDS("10000");

    private String value;

    StatOnpageEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
