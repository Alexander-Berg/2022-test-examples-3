package ru.yandex.market.logistic.gateway.service.converter.fulfillment;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;

public class DocOrderConverterTest extends BaseTest {
    private DocOrderConverter docOrderConverter;

    @Before
    public void setUp() {
        docOrderConverter = new DocOrderConverter();
    }

    @Test
    public void fromOrdersData() {
        String yandexId = "1";
        String partnerId = "2";
        ResourceId requestId = FulfillmentDataFactory.createResourceId(yandexId, partnerId).build();

        DocOrder docOrderWw = docOrderConverter.convertFromFfDocOrder(FulfillmentDataFactory.createDocOrder(requestId));
        assertions.assertThat(docOrderWw)
            .isEqualTo(FulfillmentDataFactory.wwDocOrderBuilder(partnerId, yandexId).build());
    }
}
