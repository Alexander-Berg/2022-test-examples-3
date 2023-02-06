package ru.yandex.market.wms.api.service.order;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.Assert.assertEquals;


class CarrierMappingServiceTest  extends IntegrationTest {

    @Autowired
    CarrierMappingService carrierMappingService;

    @Test
    @DatabaseSetup("/order-carrier-mapping/1/before.xml")
    @ExpectedDatabase(value = "/order-carrier-mapping/1/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void carrierCodeShouldNotBeChanged() {
        String mappedCarrierCode = carrierMappingService.mapCarrierCode("Order_id_1", "300");
        assertEquals("Carrier code should not change for not mapped carriers", "300", mappedCarrierCode);
    }

    @Test
    @DatabaseSetup("/order-carrier-mapping/2/before.xml")
    @ExpectedDatabase(value = "/order-carrier-mapping/2/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void carrierCodeShouldChangeForKnownMapping() {
        String mappedCarrierCode = carrierMappingService.mapCarrierCode("Order_id_1", "100");
        assertEquals("Carrier code should change for mapped carriers", "200", mappedCarrierCode);
    }

}
