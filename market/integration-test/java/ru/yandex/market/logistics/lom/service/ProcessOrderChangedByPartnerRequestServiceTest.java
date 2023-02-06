package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.LgwClientRequestMetaConverter;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.items.ProcessOrderChangedByPartnerRequestService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartner;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createResourceId;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class ProcessOrderChangedByPartnerRequestServiceTest extends AbstractContextualTest {
    private static final OrderIdWaybillSegmentPayload ORDER_ID_PARTNER_ID_PAYLOAD =
        PayloadFactory.createWaybillSegmentPayload(1L, 1L, "29", 1L);

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private ProcessOrderChangedByPartnerRequestService processor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-05-02T22:00:11Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Успешная обработка заявки об автоматическом изменении заказа партнёром")
    @DatabaseSetup("/service/orderchangedbypartner/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/orderchangedbypartner/after/success_creating_order_changed_by_partner_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderChangedByPartnerRequest() throws Exception {
        processor.processPayload(ORDER_ID_PARTNER_ID_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            createResourceId("2-LOinttest-1", "external-id-1").build(),
            createPartner(),
            LgwClientRequestMetaConverter.convertSequenceIdToClientRequestMeta(29L)
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/orderchangedbypartner/response/get_order_with_undefined_count.json"
                ))
        )
            .andExpect(status().isOk());

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=ORDER_CHANGED_BY_PARTNER/1/UPDATE_REQUEST/CREATED/INFO_RECEIVED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=ORDER_CHANGED_BY_PARTNER,INFO_RECEIVED,CREATED,1,10.0,1.0,20\n"
        );
    }

    @Test
    @DisplayName(
        "Успешная обработка changeRequest об автоматическом изменении заказа партнёром, заявка уже обработана"
    )
    @DatabaseSetup("/service/orderchangedbypartner/before/setup_change_request_processed.xml")
    @ExpectedDatabase(
        value = "/service/orderchangedbypartner/after/failed_request_processed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processOrderChangedByPartnerChangeRequestAlreadyProcessed() throws Exception {

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/orderchangedbypartner/response/get_order_with_undefined_count.json"
                ))
        )
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName(
        "Не обрабатывать changeRequest об автоматическом изменении заказа партнёром, если заявка уже обработана"
    )
    @DatabaseSetup("/service/orderchangedbypartner/before/setup_change_request_started_processing.xml")
    @ExpectedDatabase(
        value = "/service/orderchangedbypartner/after/failed_change_order_request_processing_start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processOrderChangedByPartnerChangeRequestNotInCreated() {
        softly.assertThat(processor.processPayload(ORDER_ID_PARTNER_ID_PAYLOAD))
            .usingRecursiveComparison()
            .isEqualTo(ProcessingResult.unprocessed(
                "Processing change request with status INFO_RECEIVED for order with id [1] is not required"
            ));
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName(
        "Успешная обработка changeRequest об автоматическом изменении заказа партнёром - " +
            "данные о товарах уже получены"
    )
    @DatabaseSetup("/service/orderchangedbypartner/before/setup_change_request.xml")
    @DatabaseSetup("/service/orderchangedbypartner/before/payload.xml")
    @ExpectedDatabase(
        value =
            "/service/orderchangedbypartner/after/success_processing_change_request_info_already_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processOrderChangedByPartnerChangeRequestInfoAlreadyReceived() {
        processor.processPayload(ORDER_ID_PARTNER_ID_PAYLOAD);

        verifyZeroInteractions(fulfillmentClient);

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=ORDER_CHANGED_BY_PARTNER/1/UPDATE_REQUEST/CREATED/INFO_RECEIVED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=ORDER_CHANGED_BY_PARTNER,INFO_RECEIVED,CREATED,1,10.0,1.0,20\n"
        );
    }

    @Test
    @DisplayName("Обработка changeRequest о ненайденном партнёром товаре заказа — заказ не изменился")
    @DatabaseSetup("/service/orderchangedbypartner/before/setup_change_request.xml")
    @ExpectedDatabase(
        value = "/service/orderchangedbypartner/after/failed_processing_order_changed_by_partner_change_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processorderchangedbypartnerChangeRequestWithoutUndefinedCount() throws Exception {
        processor.processPayload(ORDER_ID_PARTNER_ID_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            createResourceId("2-LOinttest-1", "external-id-1").build(),
            createPartner(),
            LgwClientRequestMetaConverter.convertSequenceIdToClientRequestMeta(29)
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/orderchangedbypartner/response/get_order_without_positive_undefined_count.json"
                ))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName(
        "Успешное создание заявки об автоматическом изменении заказа партнёром " +
            "с указанием количества ненайденного товара больше чем количество товара"
    )
    @DatabaseSetup("/service/orderchangedbypartner/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/orderchangedbypartner/after/undefined_count_greater_than_count.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createOrderChangedByPartnerRequestUndefinedCountGreaterThanCount() throws Exception {
        processor.processPayload(ORDER_ID_PARTNER_ID_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            createResourceId("2-LOinttest-1", "external-id-1").build(),
            createPartner(),
            LgwClientRequestMetaConverter.convertSequenceIdToClientRequestMeta(29L)
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/orderchangedbypartner/response/get_order_undefined_count_greater_than_count.json"
                ))
        )
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Undefined count can't be greater than count"));
    }

    @Test
    @DisplayName("Создание заявки для несуществующего заказа")
    void createOrderChangedByPartnerRequestOrderNotFound() {
        softly.assertThatThrownBy(() -> processor.processPayload(ORDER_ID_PARTNER_ID_PAYLOAD))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER] with id [1]");
    }

    @Test
    @DisplayName("Создание заявки для отменённого заказа")
    @DatabaseSetup("/service/orderchangedbypartner/before/setup.xml")
    @DatabaseSetup("/service/orderchangedbypartner/before/order_cancelled.xml")
    void createOrderChangedByPartnerRequestOrderCancelled() {
        processor.processPayload(ORDER_ID_PARTNER_ID_PAYLOAD);
        verifyZeroInteractions(fulfillmentClient);
        assertOrderHistoryNeverChanged(1L);
    }

    @Test
    @DisplayName("Обработка заказа без заявок на изменение")
    @DatabaseSetup("/service/orderchangedbypartner/before/setup_without_request.xml")
    void processOrderWithoutAnyRequest() {
        softly.assertThat(processor.processPayload(ORDER_ID_PARTNER_ID_PAYLOAD))
            .isEqualTo(ProcessingResult.unprocessed(
                "No change request found with type ORDER_CHANGED_BY_PARTNER for order with id: [1]"
            ));
    }

    @Test
    @DisplayName("Обработка заказа без заявок на изменение")
    @DatabaseSetup("/service/orderchangedbypartner/before/setup_without_request.xml")
    void processGetOrderResponseWithoutAnyRequest() throws Exception {
        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/orderchangedbypartner/response/get_order_with_undefined_count.json"
                ))
        )
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("No changeOrderRequest found for order with id: [1]"));
    }
}
