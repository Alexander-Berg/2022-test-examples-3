package ru.yandex.market.marketpromo.core.test.context.initializer;

import ru.yandex.market.marketpromo.core.application.properties.YdbProperties;

public class YdbContainerContextInitializer
        extends ru.yandex.market.ydb.integration.context.initializer.YdbContainerContextInitializer {

    static {
        prefix = YdbProperties.PREFIX;
    }
}
