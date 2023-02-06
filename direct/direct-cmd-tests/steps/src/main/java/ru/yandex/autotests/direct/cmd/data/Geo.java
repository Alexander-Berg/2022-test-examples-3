package ru.yandex.autotests.direct.cmd.data;

public enum Geo {

    RUSSIA("Россия", "225"),
    SIBERIA("Сибирь", "59"),
    CRIMEA("Крым", "977"),
    UKRAINE("Украина", "187"),
    KAZAKHSTAN("Казахстан", "159"),
    TURKEY("Турция", "983"),
    GERMANY("Германия", "96"),
    BELORUSSIA("Белоруссия", "149"),
    AUSTRIA("Австрия", "113"),
    BALASHIHA("Балашиха", "10716"),
    ALL("Весь мир", "0");

    private String name;
    private String geo;

    Geo(String name, String geo) {
        this.name = name;
        this.geo = geo;
    }

    public String getName() {
        return name;
    }

    public String getGeo() {
        return geo;
    }

    public Long getGeoNumber() {
        return Long.valueOf(geo);
    }

    @Override
    public String toString() {
        return name;
    }
}
