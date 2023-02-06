package ru.yandex.market.logistic.gateway.service.converter;

import org.junit.Test;

import ru.yandex.market.logistic.api.model.common.ApiType;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.exceptions.PropertiesException;
import ru.yandex.market.logistic.gateway.model.entity.ServiceProperties;

public class PartnerConverterTest extends BaseTest {

    @Test
    public void testConvertDeliveryApiPartnerIdToPartnerId() {
        assertions.assertThat(PartnerConverter.convertDeliveryApiPartnerIdToPartnerId(1)).isEqualTo(1L);
    }

    @Test(expected = PropertiesException.class)
    public void testConvertDeliveryApiPartnerIdToPartnerIdException() {
        PartnerConverter.convertDeliveryApiPartnerIdToPartnerId(null);
    }

    @Test
    public void testFromServiceProperties() {
        assertions.assertThat(
                PartnerConverter.fromServiceProperties(ServiceProperties.create(1L, ApiType.FULFILLMENT))
            )
            .contains(new Partner(1L));
    }

    @Test
    public void testFromServicePropertiesEmpty() {
        assertions.assertThat(PartnerConverter.fromServiceProperties(null))
            .isEmpty();
    }
}
