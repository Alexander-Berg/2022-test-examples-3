package ru.yandex.direct.useractionlog.model;

import java.util.LinkedHashMap;

import javax.annotation.Nonnull;

import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;

public class TestRowModel extends RowModel {
    public static final String VERSION = "0";

    public TestRowModel() {
    }

    public TestRowModel(LinkedHashMap<String, String> map) {
        super(map);
    }

    public TestRowModel(FieldValueList list) {
        super(list);
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String toString() {
        return "Test" + super.toString();
    }

    @Override
    @Nonnull
    Object getObjectPathParts() {
        return 1;
    }
}
