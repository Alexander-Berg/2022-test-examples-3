package ru.yandex.market.fulfillment.stockstorage.util;

import javax.annotation.Nonnull;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

public final class ModelUtil {
    private ModelUtil() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static ResourceId resourceId(String partnerId, String yandexId) {
        return ResourceId.builder().setPartnerId(partnerId).setYandexId(yandexId).build();
    }
}
