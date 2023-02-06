package ru.yandex.market.fulfillment.wrap.marschroute.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteDeliveryInterval;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteTime;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Map;

class OrderParsingTest extends ParsingTest<MarschrouteOrder> {
    OrderParsingTest() {
        super(new ObjectMapper(), MarschrouteOrder.class, "order.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.<String, Object>builder()
                .put("id", "id")
                .put("deliverySum", 550)
                .put("paymentType", MarschroutePaymentType.CASH)
                .put("sendDate", MarschrouteDate.create("28.01.2017"))
                .put("deliveryInterval", MarschrouteDeliveryInterval.WHOLE_DAY)
                .put("timeFrom", MarschrouteTime.create("09:00"))
                .put("placeId", -1)
                .put("weight", 3000)
                .put("consignee", "ИП ИВАНОВ, ИНН 123456789012")
                .put("consigneeDoc", "consignee_doc")
                .put("barcode", "A123456789")
                .put("comment", "вытирайте ноги перед подъездом")
                .build();
    }

    @Override
    protected void performAdditionalAssertions(MarschrouteOrder order) {
        softly.assertThat(order.getLocation())
                .as("Asserting that order has not null location")
                .isNotNull()
                .hasNoNullFieldsOrPropertiesExcept("locality");

        softly.assertThat(order.getOptions())
                .as("Asserting that order has not null options")
                .isNotNull()
                .hasNoNullFieldsOrProperties();
    }
}
