package ru.yandex.market.fulfillment.stockstorage.util;

import ru.yandex.market.delivery.export.client.payload.FulfillmentService;

public class FulfillmentServices {

    private FulfillmentServices() {
        throw new AssertionError();
    }

    public static FulfillmentService from(int id) {
        return from(id, FulfillmentService.FulfillmentStatus.ENABLED);
    }

    public static FulfillmentService from(int id, FulfillmentService.FulfillmentStatus status) {
        FulfillmentService fulfillmentService = new FulfillmentService();
        fulfillmentService.setMarketDeliveryServiceId(id);
        fulfillmentService.setStatus(status);

        return fulfillmentService;
    }
}
