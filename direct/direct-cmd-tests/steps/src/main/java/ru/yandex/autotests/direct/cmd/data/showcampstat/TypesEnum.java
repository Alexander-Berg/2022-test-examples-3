package ru.yandex.autotests.direct.cmd.data.showcampstat;

public enum TypesEnum {

    TOTAL("total");

    TypesEnum(String value) {
        this.value = value;
    }

    private String value;

    @Override
    public String toString() {
        return value;
    }
}
