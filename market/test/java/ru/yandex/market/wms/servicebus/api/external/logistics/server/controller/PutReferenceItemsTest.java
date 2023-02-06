package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.request.PutReferenceItemsRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.SkuIdDto;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;
import ru.yandex.market.wms.servicebus.async.dto.PutReferenceItemsAsyncDto;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PutReferenceItemsTest extends IntegrationTest {
    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean(name = "wmsApiClientImpl")
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    @MockBean
    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wmsApiClientImpl, defaultJmsTemplate);
    }

    @Test
    public void synchronousHappyPath() throws Exception {

        Mockito.reset(dbConfigService);

        Mockito.
                when(dbConfigService.getConfigAsInteger(eq("PUT_REFERENCE_ITEMS_BATCH_SIZE"), eq(500)))
                .thenReturn(500);
        Mockito
                .when(dbConfigService.getConfigAsBoolean(eq("PUT_REFERENCE_ITEMS_BATCH_ASYNC"), eq(false)))
                .thenReturn(false);

        List<Item> putReferenceItemsWmsRequest = objectMapper.readValue(
                getFileContent("api/logistics/server/putReferenceItems/synchronousHappyPath/api.request.json"),
                new TypeReference<List<Item>>() {
                });
        List<SkuIdDto> putReferenceItemsWmsResponse = objectMapper.readValue(
                getFileContent("api/logistics/server/putReferenceItems/synchronousHappyPath/api.response.json"),
                new TypeReference<List<SkuIdDto>>() {
                });
        Mockito.when(wmsApiClientImpl.putReferenceItems(any(), anyBoolean(), anyBoolean()))
                .thenAnswer((Answer) invocation -> {
                    Object[] args = invocation.getArguments();
                    Assertions.assertEquals(args[0], putReferenceItemsWmsRequest, "Request to WMS API is mismatched");
                    return putReferenceItemsWmsResponse;
                });

        mockMvc.perform(post("/api/logistic/putReferenceItems")
                        .contentType(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/putReferenceItems/synchronousHappyPath/request" +
                                ".xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/putReferenceItems/synchronousHappyPath" +
                        "/response.xml")))
                .andReturn();

        verify(wmsApiClientImpl, times(1)).putReferenceItems(putReferenceItemsWmsRequest, true, false);
    }


    @Test
    public void asynchronousHappyPath() throws Exception {
        // три айтема делим на батчи по два
        Mockito.reset(dbConfigService);

        Mockito
                .when(dbConfigService.getConfigAsInteger(eq("PUT_REFERENCE_ITEMS_BATCH_SIZE"), eq(500)))
                .thenReturn(2);
        Mockito
                .when(dbConfigService.getConfigAsBoolean(eq("PUT_REFERENCE_ITEMS_ASYNC"), eq(false)))
                .thenReturn(true);

        RequestWrapper<PutReferenceItemsRequest> putReferenceItemsWmsRequest = xmlMapper.readValue(
                getFileContent("api/logistics/server/putReferenceItems/asynchronousHappyPath/request.xml"),
                new TypeReference<RequestWrapper<PutReferenceItemsRequest>>() {
                });

        mockMvc.perform(post("/api/logistic/putReferenceItems")
                        .contentType(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/putReferenceItems/asynchronousHappyPath/request" +
                                ".xml")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(defaultJmsTemplate, times(2)).convertAndSend(eq("{mq}_{wrh}_putReferenceItems"), notNull(), notNull());

        // соберём все данные из очереди назад и проверим что совпадает
        List<Item> newItems = new ArrayList<>();
        Collection<Invocation> invocations = Mockito.mockingDetails(defaultJmsTemplate).getInvocations();
        invocations.stream()
                .filter(inv -> inv.getMethod().getName().equals("convertAndSend"))
                .forEach(inv -> {
                    newItems.addAll(((PutReferenceItemsAsyncDto) inv.getRawArguments()[1]).getItems());
                    System.out.println(inv.getMethod());
                });
        Assertions.assertEquals(putReferenceItemsWmsRequest.getRequest().getItems(), newItems);
    }
}
