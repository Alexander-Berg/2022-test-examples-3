package ru.yandex.market.logistics.lom.controller.order.processing;

import java.math.BigDecimal;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.exception.ChangeOrderSegmentException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderItemsSegmentProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createItemBuilder;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createOrderItemBuilder;

class UpdateOrderItemsSegmentRequestProcessingTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(1L, "1", 1L);

    @Autowired
    private UpdateOrderItemsSegmentProcessor processor;
    @Autowired
    private DeliveryClient deliveryClient;
    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    @DatabaseSetup("/controller/order/updateitems/segment/before/single_ds_segment_request_created.xml")
    void updateOrderItemSuccess() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateOrderItems(
            refEq(
                createOrderItemBuilder(100, BigDecimal.valueOf(2010))
                    .setItems(List.of(createItemBuilder(1).setSupplier(null).build()))
                    .build()
            ),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            refEq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .isNotEmpty()
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 1 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:1"
            ));
    }

    @Test
    @DatabaseSetup(
        "/controller/order/updateitems/segment/before/single_ds_segment_request_with_assessed_value_1001.xml"
    )
    void updateOrderItemWithAssessedValueMoreThan1000() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateOrderItems(
            refEq(
                createOrderItemBuilder(1001, BigDecimal.valueOf(2010))
                    .setItems(List.of(createItemBuilder(1).setSupplier(null).build()))
                    .build()
            ),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            refEq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .isNotEmpty()
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 1 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:1"
            ));
    }

    @Test
    @DatabaseSetup("/controller/order/updateitems/segment/before/single_ds_segment_request_created.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/segment/before/ff_partner.xml",
        type = DatabaseOperation.UPDATE
    )
    void updateOrderItemInFF() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        verify(fulfillmentClient).updateOrderItems(
            refEq(CreateLgwFulfillmentEntitiesUtils.createOrderItems(BigDecimal.valueOf(2010))),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            refEq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .isNotEmpty()
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 1 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:1"
            ));
    }

    @Test
    @DatabaseSetup("/controller/order/updateitems/segment/before/single_ds_segment_request_created.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/segment/before/sc_partner.xml",
        type = DatabaseOperation.UPDATE
    )
    void updateOrderItemInSC() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        verify(fulfillmentClient).updateOrderItems(
            refEq(CreateLgwFulfillmentEntitiesUtils.createOrderItems(BigDecimal.valueOf(2010))),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            refEq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .isNotEmpty()
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 1 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:1"
            ));
    }

    @Test
    @DatabaseSetup("/controller/order/updateitems/segment/before/segment_request_without_payload.xml")
    void updateOrderWithoutPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("No available payload for change request 1 in status PROCESSING");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Test
    @DatabaseSetup("/controller/order/updateitems/segment/before/segment_request_invalid_payload.xml")
    void updateOrderWithInvalidPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("Payload has validation errors");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Test
    @DatabaseSetup("/controller/order/updateitems/segment/before/single_ds_segment_request_created.xml")
    void processFinalFailure() {
        processor.processFinalFailure(PAYLOAD, new RuntimeException("Process final failure"));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_ITEMS_REQUEST_STATUS_UPDATE,
            PAYLOAD
        );
        softly.assertThat(backLogCaptor.getResults())
            .isNotEmpty()
            .anyMatch(line -> line.contains("level=ERROR"
                + "\tformat=plain"
                + "\tpayload=Failed to process segment change request of order 1. "
                + "Error: java.lang.RuntimeException: Process final failure."
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderRequest,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderRequest:1,changeOrderSegmentRequest:1"
            ));
    }
}
