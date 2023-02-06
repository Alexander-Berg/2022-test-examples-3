package ru.yandex.market.logistic.gateway.service.converter;

import org.junit.Test;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundDetailsXDoc;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.InboundsXDocConverter;

public class InboundsXDocConverterTest extends BaseTest {

    @Test
    public void convertInboundDetailsXDocFromApi() {
        String yandexId = "111";
        String partnerId = "222";

        String unitId = "444";
        Long unitVendorId = 100L;
        String unitArticle = "TestArticle";

        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId resourceId =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .build();

        UnitId unit = new UnitId(unitId, unitVendorId, unitArticle);
        InboundDetailsXDoc expectedInboundDetailsXDoc = new InboundDetailsXDoc(resourceId, 10, 15);

        ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundDetailsXDoc inboundDetailsXDocToConvert =
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundDetailsXDoc(
                    new ResourceId(yandexId, partnerId), 10, 15);

        InboundDetailsXDoc inboundDetailsXDoc = InboundsXDocConverter.convertInboundDetailsXDocFromApi(
            inboundDetailsXDocToConvert).orElse(null);

        assertions.assertThat(inboundDetailsXDoc)
            .as("Asserting the actual xDoc Inbound details is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedInboundDetailsXDoc);
    }
}
