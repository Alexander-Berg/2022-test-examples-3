package ru.yandex.travel.yt.benchmark;

import java.io.UnsupportedEncodingException;

import ru.yandex.travel.yt.mappings.YtColumn;
import ru.yandex.travel.yt.mappings.YtTable;

@YtTable(tableName = "sync_test2")
public class TestEntity {
    @YtColumn()
    private int name;

    @YtColumn
    private String key;

    @YtColumn()
    private byte[] value;

    public TestEntity(int name, byte[] value) {
        this.name = name;
        try {
            this.key = new String(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        this.value = value;
    }

    public TestEntity() {
    }
}


