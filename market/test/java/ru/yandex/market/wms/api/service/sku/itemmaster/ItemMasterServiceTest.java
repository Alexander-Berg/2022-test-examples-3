package ru.yandex.market.wms.api.service.sku.itemmaster;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.api.utils.JsonToObject;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.ManufacturerSku;
import ru.yandex.market.wms.common.spring.service.sku.ItemMasterService;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.request.MapSkuRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.response.MapSkuResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;

public class ItemMasterServiceTest extends IntegrationTest {
    @Autowired
    private JsonToObject jsonToObject;

    @MockBean(name = "servicebusClient")
    @Autowired
    private ServicebusClient servicebusClient;

    @Autowired
    private ItemMasterService itemMasterService;

    @Test
    @DatabaseSetup("/item-master/service/sku/1/before.xml")
    void checkMapSkuBom() throws Exception {
        reset(servicebusClient);

        MapSkuRequest serviceBusRequest = jsonToObject.loadJsonToObject(
                "item-master/service/sku/1/expected-request-to-servicebus.json", MapSkuRequest.class);

        MapSkuResponse serviceBusResponse = jsonToObject.loadJsonToObject(
                "item-master/service/sku/1/expected-response-from-servicebus.json", MapSkuResponse.class);

        Mockito.when(this.servicebusClient.mapSku(serviceBusRequest)).thenReturn(serviceBusResponse);

        Set<ManufacturerSku> items = Collections.singleton(new ManufacturerSku("649164", "e75019"));

        Map<ManufacturerSku, String> expectedResult = new HashMap<>();
        expectedResult.put(new ManufacturerSku("649164", "e75019"), "ROV0000000000001206580");

        Map<ManufacturerSku, String> receivedResult = itemMasterService.mapSku(items, true);

        assertEquals(receivedResult, expectedResult);
    }

    @Test
    @DatabaseSetup("/item-master/service/sku/1/junk-before.xml")
    void mapSkuFiltered() throws Exception {
        reset(servicebusClient);

        MapSkuRequest serviceBusRequest = jsonToObject.loadJsonToObject(
                "item-master/service/sku/1/expected-request-to-servicebus.json", MapSkuRequest.class);

        MapSkuResponse serviceBusResponse = jsonToObject.loadJsonToObject(
                "item-master/service/sku/1/expected-response-from-servicebus.json", MapSkuResponse.class);

        Mockito.when(this.servicebusClient.mapSku(serviceBusRequest)).thenReturn(serviceBusResponse);

        Set<ManufacturerSku> items = Collections.singleton(new ManufacturerSku("649164", "e75019"));

        Map<ManufacturerSku, String> expectedResult = new HashMap<>();
        expectedResult.put(new ManufacturerSku("649164", "e75019"), "ROV0000000000001206580");

        Map<ManufacturerSku, String> receivedResult = itemMasterService.mapSku(items, true);

        assertEquals(receivedResult, expectedResult);
    }
}
