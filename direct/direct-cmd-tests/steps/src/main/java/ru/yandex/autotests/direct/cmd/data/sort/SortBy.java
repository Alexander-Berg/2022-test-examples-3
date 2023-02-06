package ru.yandex.autotests.direct.cmd.data.sort;

public enum SortBy {

    ID("id"),
    PRICE("price");

    private String name;

    SortBy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
