package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.WaybillSegmentCancellationProcessor;
import ru.yandex.market.logistics.lom.service.AbstractExternalServiceTest;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DatabaseSetup("/controller/order/cancel/before/cancellation_request_segment_not_created.xml")
@DatabaseSetup(
    value = "/controller/order/cancel/before/cancellation_request_segment_created.xml",
    type = DatabaseOperation.UPDATE
)
public class CancellationSegmentRequestProcessingTest extends AbstractExternalServiceTest {

    @Autowired
    private WaybillSegmentCancellationProcessor processor;
    @Autowired
    private FulfillmentClient fulfillmentClient;
    @Autowired
    private DeliveryClient deliveryClient;

    @Test
    @DisplayName("Отмена заказа в СД")
    void deliveryProcessing() throws GatewayApiException {
        processor.processPayload(PayloadFactory.createSegmentCancellationRequestIdPayload(1L, "123", 1L));

        verify(deliveryClient).cancelOrder(
            safeRefEq(ResourceId.builder().setYandexId("LO1").setPartnerId("ext_1").build()),
            safeRefEq(new Partner(48L)),
            safeRefEq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Отмена заказа в дропоффе (сегмент партнёра DELIVERY с FF API)")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/cancellation_request_delivery_sorting_segment.xml",
        type = DatabaseOperation.UPDATE
    )
    void deliveryFfProcessing() throws GatewayApiException {
        processor.processPayload(PayloadFactory.createSegmentCancellationRequestIdPayload(1L, "123", 1L));

        verify(fulfillmentClient).cancelOrder(
            safeRefEq(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder()
                    .setYandexId("LO1")
                    .setPartnerId("ext_1")
                    .build()
            ),
            safeRefEq(new Partner(48L)),
            safeRefEq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Отмена заказа в СЦ")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/cancellation_request_sorting_segment.xml",
        type = DatabaseOperation.UPDATE
    )
    void sortingCenterProcessing() throws GatewayApiException {
        processor.processPayload(PayloadFactory.createSegmentCancellationRequestIdPayload(1L, "123", 1L));

        verify(fulfillmentClient).cancelOrder(
            safeRefEq(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder()
                    .setYandexId("LO1")
                    .setPartnerId("ext_1")
                    .build()
            ),
            safeRefEq(new Partner(42L)),
            safeRefEq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Отмена заказа в дропшипе")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/cancellation_request_dropship_segment.xml",
        type = DatabaseOperation.UPDATE
    )
    void dropshipProcessing() throws GatewayApiException {
        processor.processPayload(PayloadFactory.createSegmentCancellationRequestIdPayload(1L, "123", 1L));

        verify(fulfillmentClient).cancelOrder(
            safeRefEq(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder()
                    .setYandexId("LO1")
                    .setPartnerId("ext_1")
                    .build()
            ),
            safeRefEq(new Partner(555L)),
            safeRefEq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Неподдерживаемый тип сегмента")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/cancellation_request_supplier_segment.xml",
        type = DatabaseOperation.UPDATE
    )
    void unsupportedPartnerTypeProcessing() {
        softly.assertThatThrownBy(
            () -> processor.processPayload(PayloadFactory.createSegmentCancellationRequestIdPayload(1L, 1L))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Order (1) cancellation is unsupported for a segment with type SUPPLIER.");

        verifyZeroInteractions(fulfillmentClient);
        verifyZeroInteractions(deliveryClient);
    }

    @Test
    @DisplayName("Обработка финальной ошибки")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/segment_request_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processingFail() {
        processor.processFinalFailure(
            PayloadFactory.createSegmentCancellationRequestIdPayload(1L, 1L),
            new RuntimeException("Process final failure")
        );

        verifyZeroInteractions(fulfillmentClient);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }
}
