package ru.yandex.market.logistics.management.util;

import org.assertj.core.api.AbstractStandardSoftAssertions;

import ru.yandex.market.logistics.Logistics;

public class DynamicBuilderSoftAssert extends AbstractStandardSoftAssertions {
    public DynamicBuilderAssert assertThat(Logistics.MetaInfo actual) {
        return proxy(DynamicBuilderAssert.class, Logistics.MetaInfo.class, actual);
    }
}
