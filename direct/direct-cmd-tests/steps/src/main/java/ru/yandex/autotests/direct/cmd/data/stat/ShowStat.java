package ru.yandex.autotests.direct.cmd.data.stat;

public enum ShowStat {

    SHOW("1");

    private String name;

    ShowStat(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
