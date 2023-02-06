package ru.yandex.market.api.partner.controllers.delivery;

import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.partner.controllers.delivery.model.DeliveryServices;
import ru.yandex.market.api.partner.controllers.serialization.BaseJaxbSerializationTest;
import ru.yandex.market.api.partner.response.ApiResponseV2;
import ru.yandex.market.core.delivery.DeliveryServiceInfoShort;

/**
 * @author fbokovikov
 */
public class DeliveryServicesSerializationTest extends BaseJaxbSerializationTest {

    DeliveryServices deliveryServices;
    ApiResponseV2 apiResponseV2;

    @Before
    public void init() {
        deliveryServices = new DeliveryServices(Arrays.asList(
                new DeliveryServiceInfoShort(1, "TestService1"),
                new DeliveryServiceInfoShort(774, "FakeService"),
                new DeliveryServiceInfoShort(10, "MyService")
        ));
        apiResponseV2 = ApiResponseV2.ok(deliveryServices);
    }

    @Test
    public void testJsonAndXmlSerialization() throws Exception {
        testSerialization(apiResponseV2,
                IOUtils.toString(this.getClass().getResourceAsStream("deliveryServicesResponse.json"), "UTF-8"),
                IOUtils.toString(this.getClass().getResourceAsStream("deliveryServicesResponse.xml"), "UTF-8"));
    }

}
