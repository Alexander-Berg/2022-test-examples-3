package ru.yandex.market.logistic.gateway.service.converter.fulfillment;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetAttachedDocsRequest;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;

public class OrdersDataConverterTest extends BaseTest {
    private OrdersDataConverter ordersDataConverter;

    @Before
    public void setUp() {
        ordersDataConverter = new OrdersDataConverter(new DateTimeConverter(), new DocOrderConverter());
    }

    @Test
    public void toWwOrdersData() {
        GetAttachedDocsRequest.OrdersData ordersDataRequest = FulfillmentDataFactory.createOrdersData();

        RtaOrdersData wwOrdersData = ordersDataConverter.convertToWwOrdersData(ordersDataRequest);
        assertions.assertThat(wwOrdersData).isEqualTo(FulfillmentDataFactory.wwOrdersDataBuilder().build());
    }
}
