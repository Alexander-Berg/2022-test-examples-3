package ru.yandex.market.logistics.lom.service;

import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.ResourceIdConverter;
import ru.yandex.market.logistics.lom.exception.ChangeOrderSegmentException;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.RecalculateOrderDatesSegmentProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Отправка запроса на обновление даты доставки при пересчете маршрута")
@DatabaseSetup("/orders/recalculate_route_dates/prepare_data.xml")
class RecalculateOrderDatesSegmentProcessorTest extends AbstractContextualTest {
    private static final Partner PARTNER = CreateLgwCommonEntitiesUtils.createPartner(1L);

    @Autowired
    private RecalculateOrderDatesSegmentProcessor processor;
    @Autowired
    private DeliveryClient deliveryClient;
    @Autowired
    private ResourceIdConverter resourceIdConverter;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("Успех - новый пэйлоад")
    void successNewPayload() {
        long orderId = 1L;
        TimeInterval timeInterval =  TimeInterval.of(LocalTime.of(12, 0), LocalTime.of(14, 0));
        OrderDeliveryDate deliveryDate = createOrderDeliveryDate(orderId, timeInterval);

        processor.processPayload(payload(orderId));

        verifyDsUpdateOrderDeliveryDate(orderId, deliveryDate);

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 1 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:1"
            ));
    }

    @Test
    @DisplayName("Успех без интервала времени доставки - новый пэйлоад")
    void successWithoutTimeIntervalNewPayload() {
        long orderId = 2L;

        processor.processPayload(payload(orderId));
        verifyDsUpdateOrderDeliveryDate(orderId, createOrderDeliveryDate(orderId, null));

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 2 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1002,lom_order:2,changeOrderSegmentRequest:2"
            ));
    }

    @Test
    @DisplayName("Успех - обновление сегмента средней мили")
    void successMiddleMileSegment() {
        long orderId = 7L;
        TimeInterval timeInterval =  TimeInterval.of(LocalTime.of(7, 0), LocalTime.of(12, 0));
        OrderDeliveryDate deliveryDate = createOrderDeliveryDate(orderId, timeInterval);

        processor.processPayload(payload(orderId));

        verifyDsUpdateOrderDeliveryDate(orderId, deliveryDate);

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 7 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1007,lom_order:7,changeOrderSegmentRequest:7"
            ));
    }

    @Test
    @DisplayName("Ошибка из-за отсутствия реквеста для сегмента")
    void failNoRequestForSegment() {
        softly.assertThatThrownBy(() -> processor.processPayload(payload(4)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER_CHANGE_SEGMENT_REQUEST] with id [4]");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Test
    @DisplayName("Не обновляем дату доставки тк FF сегмент")
    void doNothingForFF() {
        softly.assertThatCode(() -> processor.processPayload(payload(5)))
            .doesNotThrowAnyException();

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=WARN"
                + "\tformat=plain"
                + "\tpayload=Try to update delivery date of ff segment 5 in delivery service."
            ));
    }

    @Test
    @DisplayName("Пэйлоад не в том статусе")
    void invalidPayloadStatus() {
        softly.assertThatThrownBy(() -> processor.processPayload(payload(6)))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("No available payload for change request 6 in status INFO_RECEIVED");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Nonnull
    OrderDeliveryDate createOrderDeliveryDate(long orderId, @Nullable TimeInterval timeInterval) {
        return new OrderDeliveryDate(
            resourceIdConverter.toExternalDsId("barcode-" + orderId, "external-id-" + orderId),
            DateTime.fromLocalDateTime(LocalDateTime.of(2021, 8, 20, 0, 0)),
            timeInterval,
            "Update partner delivery date"
        );
    }

    @Nonnull
    private static ChangeOrderSegmentRequestPayload payload(long requestId) {
        return PayloadFactory.createChangeOrderSegmentRequestPayload(requestId, String.valueOf(requestId), requestId);
    }

    @SneakyThrows
    private void verifyDsUpdateOrderDeliveryDate(long orderId, OrderDeliveryDate orderDeliveryDate) {
        verify(deliveryClient).updateOrderDeliveryDate(
            orderDeliveryDate,
            PARTNER,
            orderId,
            new ClientRequestMeta(String.valueOf(orderId))
        );
    }
}
