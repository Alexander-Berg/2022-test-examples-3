package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.LgwClientRequestMetaConverter;
import ru.yandex.market.logistics.lom.entity.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.items.ProcessOrderItemNotFoundRequestProcessor;
import ru.yandex.market.logistics.lom.service.order.OrderCancellationService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus.UNPROCESSED;
import static ru.yandex.market.logistics.lom.utils.jobs.ProcessingResultFactory.processingResult;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartner;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createResourceId;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/service/orderitemnotfound/before/setup_change_request.xml")
class ProcessOrderItemNotFoundRequestProcessorTest extends AbstractContextualTest {
    private static final String GET_ORDER_WITH_UNDEFINED_COUNT =
        "service/orderitemnotfound/response/get_order_with_undefined_count.json";

    @Autowired
    private FulfillmentClient fulfillmentClient;
    @Autowired
    private ProcessOrderItemNotFoundRequestProcessor processor;
    @Autowired
    private OrderCancellationService orderCancellationService;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-05-02T22:00:11Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName("Успешная обработка changeRequest о ненайденном партнёром товаре заказа")
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/success_processing_order_item_not_found_change_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processOrderItemNotFoundChangeRequest() throws Exception {
        processor.processPayload(getFirstChangeOrderRequestPayload());

        verifyGetOrder();

        postSuccess(GET_ORDER_WITH_UNDEFINED_COUNT)
            .andExpect(status().isOk());

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/UPDATE_REQUEST/CREATED/INFO_RECEIVED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=ITEM_NOT_FOUND,INFO_RECEIVED,CREATED,1,10.0,1.0,20\n"
        );
    }

    @Test
    @DisplayName(
        "Обработка changeRequest о ненайденном партнёром товаре заказа — партнёр не умеет обновлять товары заказа"
    )
    @DatabaseSetup(
        value = "/service/orderitemnotfound/before/partner_cant_change_items.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value =
            "/service/orderitemnotfound/after/" +
                "processing_order_item_not_found_change_request_partner_cant_change_items.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processOrderItemNotFoundChangeRequestPartnerCantChangeOrderItems() throws Exception {
        processor.processPayload(getFirstChangeOrderRequestPayload());

        verifyGetOrder();

        postSuccess(GET_ORDER_WITH_UNDEFINED_COUNT)
            .andExpect(status().isOk());

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/UPDATE_REQUEST/CREATED/REJECTED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=ITEM_NOT_FOUND,REJECTED,CREATED,1,10.0,1.0,20\n"
        );
    }

    @Test
    @DisplayName(
        "Обработка changeRequest для заказа, отменённого службой — партнёр не умеет обновлять товары заказа"
    )
    @DatabaseSetup(
        value = "/service/orderitemnotfound/before/partner_cant_change_items.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/service/orderitemnotfound/before/waybill_has_cancel_status.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value =
            "/service/orderitemnotfound/after/" +
                "processing_order_item_not_found_change_request_partner_cant_change_items.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processChangeRequestPartnerCantChangeOrderItemsOrderHasCancelCheckpoint() throws Exception {
        processor.processPayload(getFirstChangeOrderRequestPayload());

        verifyGetOrder();

        postSuccess(GET_ORDER_WITH_UNDEFINED_COUNT)
            .andExpect(status().isOk());

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/UPDATE_REQUEST/CREATED/REJECTED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=ITEM_NOT_FOUND,REJECTED,CREATED,1,10.0,1.0,20\n"
        );
    }

    @Test
    @DisplayName(
        "Обработка changeRequest о ненайденном партнёром товаре заказа — заказ уже отменён"
    )
    @ExpectedDatabase(
        value =
            "/service/orderitemnotfound/after/" +
                "processing_order_item_not_found_change_request_partner_cant_change_items_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processOrderItemNotFoundChangeRequestPartnerCantChangeOrderItemsCancelled() throws Exception {
        processor.processPayload(getFirstChangeOrderRequestPayload());

        verifyGetOrder();

        orderCancellationService.cancelOrder(
            1L,
            null,
            CancellationOrderReason.DELIVERY_SERVICE_UNDELIVERED,
            null
        );

        postSuccess(GET_ORDER_WITH_UNDEFINED_COUNT)
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("No itemNotFound request found for order with id: [1]"));
    }

    @Test
    @DisplayName("Обработка changeRequest о ненайденном партнёром товаре заказа — заказ не изменился")
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/failed_processing_order_item_not_found_change_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processOrderItemNotFoundChangeRequestWithoutUndefinedCount() throws Exception {
        processor.processPayload(getFirstChangeOrderRequestPayload());

        verifyGetOrder();
        postSuccess("service/orderitemnotfound/response/get_order_without_positive_undefined_count.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обработка несуществующего changeRequest о ненайденном партнёром товаре заказа")
    void processNonExistItemNotFoundChangeRequest() {
        softly.assertThatThrownBy(
            () -> processor.processPayload(PayloadFactory.createChangeOrderRequestPayload(2, "29", 1L))
        )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER_CHANGE_REQUEST] with id [2]");
    }

    @Test
    @DisplayName("Обработка changeRequest с неправильным типом")
    @DatabaseSetup(
        value = "/service/orderitemnotfound/before/order_changed_by_partner_type.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/processing_order_item_not_found_change_request_with_wrong_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processRequestWithWrongType() {
        softly.assertThat(processor.processPayload(getFirstChangeOrderRequestPayload()))
            .isEqualTo(processingResult(
                UNPROCESSED,
                "Cannot process change request with type ORDER_CHANGED_BY_PARTNER. Processable type is ITEM_NOT_FOUND"
            ));
    }

    @Test
    @DisplayName("Обработка changeRequest с неправильным статусом")
    @DatabaseSetup(
        value = "/service/orderitemnotfound/before/info_received_status.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/processing_order_item_not_found_change_request_in_wrong_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processRequestWithWrongStatus() {
        softly.assertThat(processor.processPayload(getFirstChangeOrderRequestPayload()))
            .isEqualTo(processingResult(
                UNPROCESSED,
                "Cannot process change request with status INFO_RECEIVED. Processable status is CREATED"
            ));
    }

    @Test
    @DisplayName("Обработка changeRequest о ненайденном партнёром товаре заказа с заявками на отмену")
    @DatabaseSetup("/service/orderitemnotfound/before/order_cancelled.xml")
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/processing_order_item_not_found_change_request_in_cancellation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processRequestForOrderInCancellation() {
        softly.assertThat(processor.processPayload(getFirstChangeOrderRequestPayload()))
            .isEqualTo(processingResult(
                UNPROCESSED,
                "Cannot process change request for order with cancellationRequests"
            ));
    }

    @Test
    @DisplayName("Обработка changeRequest о ненайденном партнёром товаре заказа — несколько ошибок")
    @DatabaseSetup("/service/orderitemnotfound/before/order_cancelled.xml")
    @DatabaseSetup(
        value = "/service/orderitemnotfound/before/info_received_status.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/processing_order_item_not_found_change_request_multiple_errors.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processRequestForOrderWithMultipleErrors() {
        softly.assertThat(processor.processPayload(getFirstChangeOrderRequestPayload()))
            .isEqualTo(processingResult(
                UNPROCESSED,
                "Cannot process change request with status INFO_RECEIVED. Processable status is CREATED;" +
                    " Cannot process change request for order with cancellationRequests"
            ));
    }

    @Test
    @DisplayName("Обработка changeRequest о ненайденном партнёром товаре заказа, заказ уже собран")
    @DatabaseSetup("/service/orderitemnotfound/before/order_received_transit_prepared_status.xml")
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/processing_order_item_not_found_change_request_transit_prepared.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processRequestForTransitPreparedOrder() {
        softly.assertThat(processor.processPayload(getFirstChangeOrderRequestPayload()))
            .isEqualTo(processingResult(UNPROCESSED, "Segment 1 has non-processable status"));

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/UPDATE_REQUEST/CREATED/REJECTED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=ITEM_NOT_FOUND,REJECTED,CREATED,1,10.0,1.0,20\n"
        );
    }

    @Test
    @DisplayName("Обработка changeRequest о ненайденном партнёром товаре заказа, заказ уже не в 113 статусе")
    @DatabaseSetup(
        value = "/service/orderitemnotfound/before/waybill_segment_status_in.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/processing_order_item_not_found_change_request_transit_prepared.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processRequestForOrderNotOutOfTransit() {
        softly.assertThat(processor.processPayload(getFirstChangeOrderRequestPayload()))
            .isEqualTo(processingResult(UNPROCESSED, "Segment 1 has non-processable status"));

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/UPDATE_REQUEST/CREATED/REJECTED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=ITEM_NOT_FOUND,REJECTED,CREATED,1,10.0,1.0,20\n"
        );
    }

    @Test
    @DisplayName("Обновляется запрос в статусе CREATED")
    @DatabaseSetup(
        value = "/service/orderitemnotfound/before/rejected_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/orderitemnotfound/after/success_processing_with_rejected_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processWithRejectedRequest() throws Exception {
        processor.processPayload(getChangeOrderRequestPayload(2));
        verifyGetOrder();
        postSuccess(GET_ORDER_WITH_UNDEFINED_COUNT)
            .andExpect(status().isOk());
    }

    @Nonnull
    private ChangeOrderRequestPayload getFirstChangeOrderRequestPayload() {
        return getChangeOrderRequestPayload(1);
    }

    @Nonnull
    private ChangeOrderRequestPayload getChangeOrderRequestPayload(long requestId) {
        return PayloadFactory.createChangeOrderRequestPayload(requestId, "29", 1L);
    }

    private void verifyGetOrder() throws Exception {
        verify(fulfillmentClient).getOrder(
            createResourceId("2-LOinttest-1", "external-id-1").build(),
            createPartner(),
            LgwClientRequestMetaConverter.convertSequenceIdToClientRequestMeta(29)
        );
    }

    @Nonnull
    private ResultActions postSuccess(String path) throws Exception {
        return mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(path))
        );
    }
}
