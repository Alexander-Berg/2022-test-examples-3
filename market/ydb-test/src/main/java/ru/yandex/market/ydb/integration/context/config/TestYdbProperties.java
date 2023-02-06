package ru.yandex.market.ydb.integration.context.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

import ru.yandex.market.ydb.integration.BaseYdbProperties;

@ConfigurationProperties(prefix = TestYdbProperties.PREFIX)
public class TestYdbProperties extends BaseYdbProperties {

    public static final String PREFIX = "ydb";
}
