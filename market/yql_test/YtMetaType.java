package ru.yandex.market.yql_test;

enum YtMetaType {
    OPTIONAL("optional"),
    NOT("not"),
    LIST("list");

    private String value;

    YtMetaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }

    public static YtMetaType of(String value) {
        for (YtMetaType v : values()) {
            if (v.getValue().equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Not supported meta type v3: " + value);
    }
}
