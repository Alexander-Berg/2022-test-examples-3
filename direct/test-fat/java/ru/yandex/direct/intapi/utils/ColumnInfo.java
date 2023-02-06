package ru.yandex.direct.intapi.utils;

import javax.annotation.Nullable;

public class ColumnInfo {
    final String name;
    final String type;
    final String expression;
    final boolean isKey;

    public ColumnInfo(String name, String type, @Nullable String expression, boolean isKey) {
        this.name = name;
        this.type = type;
        this.expression = expression;
        this.isKey = isKey;
    }

    public ColumnInfo(String name, String type, boolean isKey) {
        this(name, type, null, isKey);
    }
}
