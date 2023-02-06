package ru.yandex.autotests.direct.cmd.data.banners;

/**
 * Параметры для поиска баннеров
 */
public enum SearchWhere {
    DIRECT("direct"),
    MCB("mcb");

    private String name;


    SearchWhere(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
