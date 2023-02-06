package ru.yandex.market.mbo.lightmapper.benchmark.data;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import ru.yandex.market.mbo.lightmapper.reflective.annotations.GeneratedValue;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Id;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Jsonb;

public class Item {
    private static int nextId;
    private static Random random = new Random();

    @GeneratedValue
    @Id
    private  int id;
    private  String name;
    private  Instant date;
    @Jsonb
    private  Data data;
    private  Long mappingId;

    public Item() {
    }

    public Item(int id, String name, Instant date, Data data, Long mappingId) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.data = data;
        this.mappingId = mappingId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getDate() {
        return date;
    }

    public Data getData() {
        return data;
    }

    public Long getMappingId() {
        return mappingId;
    }

    public Item setId(int id) {
        this.id = id;
        return this;
    }

    public Item setName(String name) {
        this.name = name;
        return this;
    }

    public Item setDate(Instant date) {
        this.date = date;
        return this;
    }

    public Item setData(Data data) {
        this.data = data;
        return this;
    }

    public Item setMappingId(Long mappingId) {
        this.mappingId = mappingId;
        return this;
    }

    public static Item randomItem() {
        return new Item(nextId++, "Some very random string",
                Instant.now().plus(100000 - random.nextInt(200000), ChronoUnit.SECONDS),
                new Data("again, random string", random.nextLong()), random.nextLong());
    }

    public static Item randomItemNoJson() {
        return new Item(nextId++, "Some very random string",
                Instant.now().plus(100000 - random.nextInt(200000), ChronoUnit.SECONDS),
                null, random.nextLong());
    }
}
