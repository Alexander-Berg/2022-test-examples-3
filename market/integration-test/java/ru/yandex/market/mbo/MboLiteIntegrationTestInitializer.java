package ru.yandex.market.mbo;

import ru.yandex.market.mbo.utils.test.IntegrationTestInitializer;

/**
 * @author yuramalinov
 * @created 24.04.18
 */
public class MboLiteIntegrationTestInitializer extends IntegrationTestInitializer {
    {
        forceLazyInit = true;
    }
}
