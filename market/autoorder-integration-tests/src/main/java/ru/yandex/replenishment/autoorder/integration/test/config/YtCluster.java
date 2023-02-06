package ru.yandex.replenishment.autoorder.integration.test.config;

import java.util.Arrays;

import javax.annotation.Nonnull;

public enum YtCluster {
    HAHN("hahn");

    private final String name;

    YtCluster(String proxyName) {
        this.name = proxyName;
    }

    public static YtCluster byName(@Nonnull String name) {
        return Arrays.stream(YtCluster.values())
                .filter(ytCluster -> name.equals(ytCluster.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No YT cluster with name " + name));
    }

    public String getName() {
        return name;
    }
}
