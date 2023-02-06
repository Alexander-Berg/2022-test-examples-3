package ru.yandex.market.wms.api.service.referenceitems.push;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.model.dto.SkuIdDto;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.PushReferenceItemsResultDto;
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushReferenceItemsRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.response.PushReferenceItemsResponse;

import static org.mockito.Mockito.reset;

public class PushReferenceItemsServiceTest extends IntegrationTest {

    @MockBean(name = "servicebusClient")
    @Autowired
    private ServicebusClient servicebusClient;

    @Autowired
    private PushReferenceItemsService pushReferenceItemsService;

    /**
     * Сценарий #1: данные по 1 паре sku + storerkey
    */
    @Test
    @DatabaseSetup("/referenceitems/push/1/before.xml")
    void check1PushResult() throws Exception {
        checkPushResult("referenceitems/push/1/skuiddtos.json",
                "referenceitems/push/1/expected-push-result.json");
    }

    /**
     * Проверка вызова метода ServiceBus
    */
    @Test
    @DatabaseSetup("/referenceitems/push/1/before.xml")
    void checkServiceBusCall() throws Exception {
        checkServiceBusCall("referenceitems/push/1/skuiddtos.json",
                "referenceitems/push/1/expected-request-to-servicebus.json");
    }

    /**
     * Сценарий #2: данные по 2 парам sku + storerkey
     */
    @Test
    @DatabaseSetup("/referenceitems/push/2/before.xml")
    void check2PushResult() throws Exception {
        checkPushResult("referenceitems/push/2/skuiddtos.json",
                "referenceitems/push/2/expected-push-result.json");
    }

    /**
     * Сценарий #3: многоместка
     */
    @Test
    @DatabaseSetup("/referenceitems/push/3/before.xml")
    void check3PushResult() throws Exception {
        checkPushResult("referenceitems/push/3/request.json",
                "referenceitems/push/3/response.json");
    }

    private void checkPushResult(String pathToSkuIdDtos, String pathToExpectedResult) throws Exception {
        Iterable<SkuIdDto> skuIdDtos = Arrays.asList(
                loadJsonToObject(pathToSkuIdDtos, SkuIdDto[].class)
        );

        Iterable<PushReferenceItemsResultDto> receivedResult = pushReferenceItemsService.calcRecordsToPush(skuIdDtos);

        Iterable<PushReferenceItemsResultDto> expectedResult = Arrays.asList(
                loadJsonToObject(pathToExpectedResult, PushReferenceItemsResultDto[].class)
        );

        Assert.assertEquals(expectedResult, receivedResult);
    }

    private void checkServiceBusCall(String pathToInParam, String pathToServiceBusRequest) throws Exception {
        reset(servicebusClient);
        PushReferenceItemsRequest request = loadJsonToObject(pathToServiceBusRequest, PushReferenceItemsRequest.class);

        Mockito.when(this.servicebusClient.pushReferenceItems(request))
                .thenReturn(PushReferenceItemsResponse.builder().build());

        Iterable<SkuIdDto> skuIdDtos = Arrays.asList(
                loadJsonToObject(pathToInParam, SkuIdDto[].class)
        );

        pushReferenceItemsService.runPush(skuIdDtos);

        Mockito.verify(this.servicebusClient).pushReferenceItems(request);
    }

    private <T> T loadJsonToObject(String path, Class<T> generic) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        return mapper.readValue(inputStream, generic);
    }
}
