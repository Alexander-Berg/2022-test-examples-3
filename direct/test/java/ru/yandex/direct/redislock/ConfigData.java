package ru.yandex.direct.redislock;

import java.util.List;
import java.util.stream.Collectors;

import io.lettuce.core.RedisURI;

import static java.util.Arrays.asList;

public class ConfigData {

    private ConfigData() {
    }

    public static final List<String> HOSTS_LIST = asList(
            "redis-test01k.ppc.yandex.ru",
            "redis-test01f.ppc.yandex.ru",
            "redis-test01i.ppc.yandex.ru");

    public static final List<RedisURI> REDIS_URIS = HOSTS_LIST.stream().
            map(host -> RedisURI.create(host, 6411)).
            collect(Collectors.toList());

}
