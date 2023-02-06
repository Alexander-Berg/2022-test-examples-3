package ru.yandex.market.yql_test;

enum YtType {
    INT32("int32"),
    INT64("int64"),
    UINT32("uint32"),
    UINT64("uint64"),
    STRING("string"),
    BOOL("bool"),
    DOUBLE("double"),
    YSON("yson"),
    UTF8("utf8");

    private String value;

    YtType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }

    public static YtType of(String value) {
        for (YtType v : values()) {
            if (v.getValue().equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Not supported type: " + value);
    }
}
