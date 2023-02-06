package ru.yandex.autotests.innerpochta.util.surfwax;

public class Host {
    
    private final String name;
    private final int port;

    public Host(String name, int port) {
        this.name = name;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }
}
