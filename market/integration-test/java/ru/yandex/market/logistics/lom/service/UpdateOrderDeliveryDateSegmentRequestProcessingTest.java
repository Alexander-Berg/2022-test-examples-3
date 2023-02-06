package ru.yandex.market.logistics.lom.service;

import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.ResourceIdConverter;
import ru.yandex.market.logistics.lom.exception.ChangeOrderSegmentException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderDeliveryDateSegmentProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("Обновление даты доставки на сегменте")
class UpdateOrderDeliveryDateSegmentRequestProcessingTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(10L, "1", 1L);

    @Autowired
    private UpdateOrderDeliveryDateSegmentProcessor processor;
    @Autowired
    private DeliveryClient deliveryClient;
    @Autowired
    private ResourceIdConverter resourceIdConverter;

    @Test
    @DisplayName("Успешная отправка обновление даты доставки заказа")
    @DatabaseSetup("/orders/update_delivery_date/single_ds_segment_request_created.xml")
    void updateOrderDeliveryDateSuccess() throws GatewayApiException {
        TimeInterval timeInterval = TimeInterval.of(LocalTime.of(9, 0), LocalTime.of(18, 0));
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateOrderDeliveryDate(
            eq(createOrderDeliveryDate(timeInterval)),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(10L),
            eq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 10 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:10"
            ));
    }

    @Test
    @DisplayName("Успешная отправка обновление даты доставки заказа без интервала времени доставки")
    @DatabaseSetup("/orders/update_delivery_date/single_ds_segment_request_created_without_interval.xml")
    void updateOrderDeliveryDateWithoutTimeIntervalSuccess() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateOrderDeliveryDate(
            eq(createOrderDeliveryDate(null)),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(10L),
            eq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 10 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:10"
            ));
    }

    @Test
    @DisplayName("Успешная отправка обновление даты доставки заказа для средней мили")
    @DatabaseSetup("/orders/update_delivery_date/single_ds_middle_mile_segment_request_created.xml")
    void updateOrderDeliveryDateMiddleMileSuccess() throws GatewayApiException {
        TimeInterval timeInterval = TimeInterval.of(LocalTime.of(7, 0), LocalTime.of(12, 0));
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateOrderDeliveryDate(
            eq(createOrderDeliveryDate(timeInterval)),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(10L),
            eq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 10 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:10"
            ));
    }

    @Test
    @DisplayName("Ошибка обновление даты доставки заказа из-за отсутствия PAYLOAD")
    @DatabaseSetup("/orders/update_delivery_date/segment_request_without_payload.xml")
    void updateOrderDeliveryDateWithoutPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("No available payload for change request 1 in status INFO_RECEIVED");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Test
    @DisplayName("Ошибка обновление даты доставки заказа из-за невалидного PAYLOAD")
    @DatabaseSetup("/orders/update_delivery_date/segment_request_invalid_payload.xml")
    void updateOrderDeliveryDateWithInvalidPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("Payload has validation errors");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Test
    @DisplayName("Ошибка обновление даты доставки заказа из-за невалидного сегмента")
    @DatabaseSetup("/orders/update_delivery_date/segment_request_invalid_segment.xml")
    void updateOrderDeliveryDateWithInvalidSegment() {
        softly.assertThat(processor.processPayload(PAYLOAD))
            .isEqualTo(ProcessingResult.unprocessed(
                "Order (1) delivery date updating is unsupported for a segment with partner type FULFILLMENT, "
                    + "segment type FULFILLMENT."
            ));

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t"
                    + "format=plain\t"
                    + "payload=Delivery date updating is unsupported for a segment with type FULFILLMENT."
                    + " Segment partner type FULFILLMENT\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
            );
    }

    @Nonnull
    private OrderDeliveryDate createOrderDeliveryDate(@Nullable TimeInterval timeInterval) {
        return new OrderDeliveryDate(
            resourceIdConverter.toExternalDsId("barcode-1", "external-id-1"),
            DateTime.fromLocalDateTime(LocalDateTime.of(2020, 10, 8, 0, 0)),
            timeInterval,
            "Update partner delivery date: UNKNOWN"
        );
    }
}
