package ru.yandex.market.checkout.pushapi.service.shop;

import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.shop.ShopApiResponse;

public interface ErrorsStoreService {

    void storeSuccess(
        long shopId, String request, boolean sandbox, ShopApiResponse response
    );
    void storeError(
        long shopId, String request, ErrorSubCode errorSubCode, String message, boolean sandbox,
        ShopApiResponse response
    );
    
}
