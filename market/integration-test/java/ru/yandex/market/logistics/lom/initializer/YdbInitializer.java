package ru.yandex.market.logistics.lom.initializer;

import ru.yandex.market.ydb.integration.context.initializer.YdbContainerContextInitializer;

public class YdbInitializer extends YdbContainerContextInitializer {

    public YdbInitializer() {
        super();
        prefix = "lom.ydb";
    }
}
