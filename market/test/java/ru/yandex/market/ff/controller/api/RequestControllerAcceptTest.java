package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.RequestAcceptDTO;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;
import ru.yandex.market.ff.service.LmsClientCachingService;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.service.util.MbiNotificationTypes.SUPPLY_ACCEPTED_BY_SERVICE;

public class RequestControllerAcceptTest extends MvcIntegrationTest {

    private static final long VALID_REQ_ID = 1;
    private static final long INVALID_REQ_ID = 2;
    private static final long NOT_EXISTED_REQ_ID = 66;
    private static final long RETURN_REQ_ID = 4;
    private static final long WITHDRAW_REQ_ID = 5;
    private static final long WITHDRAW_CANCELLED_REQ_ID = 6;
    private static final long VALID_SUPPLIER_ID = 1;
    private static final long ALIEN_SUPPLIER_ID = 2;
    private static final long VALID_TRANSFER_REQ_ID = 4;
    private static final long ALIEN_TRANSFER_REQ_ID = 12;
    private static final String SERVICE_REQ_ID = "req_id_1";

    private static final RequestAcceptDTO ACCEPT_CONTENT = new RequestAcceptDTO(SERVICE_REQ_ID);

    private static final long FULFILLMENT_ID = 1;

    private static final String REQUEST_NOT_FOUND_ERROR =
            "{\"message\":\"Failed to find [REQUEST] with id [66]\",\"resourceType\":\"REQUEST\"," +
                    "\"identifier\":\"66\"}";

    private static final String ALIEN_REQUEST_NOT_FOUND_ERROR =
            "{\"message\":\"Failed to find [REQUEST] with id [1]\",\"resourceType\":\"REQUEST\",\"identifier\":\"1\"}";

    private static final String ALIEN_TRANSFER_NOT_FOUND_ERROR =
            "{\"message\":\"Failed to find [REQUEST] with id [12]\",\"resourceType\":\"REQUEST\"," +
                    "\"identifier\":\"12\"}";

    private static final String INVALID_STATUS_ERROR_ACCEPTED =
            "{\"message\":\"It's not allowed to change status for request 2 from CREATED to ACCEPTED_BY_SERVICE\","
                    + "\"type\":\"INCONSISTENT_REQUEST_MODIFICATION\"}";

    private static final String NN_ACCEPTED_PARAMS_XML = ""
            + "<request-info>"
            + "<id>1</id>"
            + "<service-request-id>req_id_1</service-request-id>"
            + "<destination-warehouse-id>1</destination-warehouse-id>"
            + "<destination-warehouse-name>test</destination-warehouse-name>"
            + "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>"
            + "<merchandise-receipt-time>09:09</merchandise-receipt-time>"
            + "</request-info>";

    @Autowired
    private LmsClientCachingService lmsClientCachingService;

    @AfterEach
    public void initMocks() {
        lmsClientCachingService.invalidateCache();
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-accept-supply.xml", assertionMode = NON_STRICT)
    void acceptSuccessfully() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);

        performAccept(VALID_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_ACCEPTED_BY_SERVICE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(NN_ACCEPTED_PARAMS_XML, argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-accept-supply.xml", assertionMode = NON_STRICT)
    void acceptSupplierSuccessfully() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);

        performAccept(VALID_SUPPLIER_ID, VALID_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_ACCEPTED_BY_SERVICE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals(NN_ACCEPTED_PARAMS_XML, argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-accept-return.xml", assertionMode = NON_STRICT)
    void acceptSuccessfullyReturn() throws Exception {
        performAccept(RETURN_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isOk());
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-accept-withdraw.xml", assertionMode = NON_STRICT)
    void acceptSuccessfullyWithdraw() throws Exception {
        performAccept(WITHDRAW_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isOk());
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-accept-withdraw-cancelled.xml",
            assertionMode = NON_STRICT)
    void acceptSuccessfullyWithdrawWhenCancelled() throws Exception {
        performAccept(WITHDRAW_CANCELLED_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isOk());

        verifyZeroInteractions(sendMbiNotificationQueueProducer);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);

        final ResourceId resourceId = ResourceId.builder()
                .setYandexId(String.valueOf(WITHDRAW_CANCELLED_REQ_ID))
                .setPartnerId(SERVICE_REQ_ID)
                .build();

        verify(fulfillmentClient).cancelOutbound(resourceId, new Partner(FULFILLMENT_ID));
        verifyZeroInteractions(stockStorageOutboundClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-accept-reject-utilization-outbound.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-accept-utilization-outbound.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptSuccessfullyUtilizationOutbound() throws Exception {
        performAccept(88, toJson(ACCEPT_CONTENT)).andExpect(status().isBadRequest());
        performAccept(8, toJson(ACCEPT_CONTENT)).andExpect(status().isOk());
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-acceptance.xml", assertionMode = NON_STRICT)
    void acceptSupplierNotFound() throws Exception {
        final MvcResult mvcResult = performAccept(ALIEN_SUPPLIER_ID, VALID_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo(ALIEN_REQUEST_NOT_FOUND_ERROR));
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-acceptance.xml", assertionMode = NON_STRICT)
    void acceptNotFound() throws Exception {
        final MvcResult mvcResult = performAccept(NOT_EXISTED_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo(REQUEST_NOT_FOUND_ERROR));
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-acceptance.xml", assertionMode = NON_STRICT)
    void acceptInvalidStatus() throws Exception {
        final MvcResult mvcResult = performAccept(INVALID_REQ_ID, toJson(ACCEPT_CONTENT))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(INVALID_STATUS_ERROR_ACCEPTED));
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-acceptance.xml", assertionMode = NON_STRICT)
    void acceptInvalidParams() throws Exception {
        performAccept(VALID_REQ_ID, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andReturn();
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }


    /**
     * Проверяет работу метода {@link RequestController#acceptByService(long, RequestAcceptDTO)}
     * для правильного набора данных
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/on-accept-transfer.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-accept-transfer.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptTransferSuccessfully() throws Exception {
        performAccept(VALID_SUPPLIER_ID, VALID_TRANSFER_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isOk());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    /**
     * Проверяет ошибку метода {@link RequestController#acceptByService(long, RequestAcceptDTO)}
     * для отсутствующего трансфера
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/on-accept-transfer.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-accept-transfer.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptTransferNotFound() throws Exception {
        MvcResult mvcResult = performAccept(VALID_SUPPLIER_ID, ALIEN_TRANSFER_REQ_ID, toJson(ACCEPT_CONTENT))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo(ALIEN_TRANSFER_NOT_FOUND_ERROR));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    private ResultActions performAccept(long requestId, String content) throws Exception {
        return perform(null, requestId, "accept-by-service", content);
    }

    private ResultActions performAccept(long supplierId, long requestId, String content) throws Exception {
        return perform(supplierId, requestId, "accept-by-service", content);
    }

    private ResultActions performRegisterAccept(long requestId, String content) throws Exception {
        return perform(null, requestId, "accept-register-by-service", content);
    }

    private ResultActions perform(Long supplierId, long requestId, String method, String content) throws Exception {
        return mockMvc.perform(
                put((supplierId == null ? "" : "/suppliers/" + supplierId) + "/requests/" + requestId + "/" + method)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        ).andDo(print());
    }
}
