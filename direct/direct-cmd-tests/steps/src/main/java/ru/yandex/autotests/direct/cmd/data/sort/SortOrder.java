package ru.yandex.autotests.direct.cmd.data.sort;

/**
 * Enum направления сортировки
 */
public enum SortOrder {

    DESCENDING("Сортировка по убыванию", 0, "desc"),
    ASCENDING("Сортировка по возрастанию", 1, "asc");

    private int order;
    private String sortName;
    private String name;

    SortOrder(String sortName, int order, String name) {
        this.sortName = sortName;
        this.order = order;
        this.name = name;
    }

    @Override
    public String toString() {
        return sortName;
    }

    public String getSortParameter() {
        return "reverse=" + order;
    }

    public int getOrder() {
        return order;
    }

    public String getName() {
        return name;
    }
}
