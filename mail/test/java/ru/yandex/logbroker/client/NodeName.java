package ru.yandex.logbroker.client;

public class NodeName {
    private final String dc;
    private final String name;

    public NodeName(final String dc, final String name) {
        this.dc = dc;
        this.name = name;
    }

    public String dc() {
        return dc;
    }

    public String name() {
        return name;
    }
}
