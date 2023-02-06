package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.market.delivery.mdbapp.integration.payload.CreateLgwFfOrder;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;

public class CreateLfwFfOrderMixIn extends CreateLgwFfOrder {

    public CreateLfwFfOrderMixIn(@JsonProperty("order") Order order,
                                 @JsonProperty("partner") Partner partner) {
        super(order, partner);
    }
}
