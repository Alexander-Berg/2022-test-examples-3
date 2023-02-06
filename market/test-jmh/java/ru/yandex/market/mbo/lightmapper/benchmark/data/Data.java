package ru.yandex.market.mbo.lightmapper.benchmark.data;

public class Data {
    private String name;
    private Long data;

    public Data() {
    }

    public Data(String name, Long data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public Data setName(String name) {
        this.name = name;
        return this;
    }

    public Long getData() {
        return data;
    }

    public Data setData(Long data) {
        this.data = data;
        return this;
    }
}
