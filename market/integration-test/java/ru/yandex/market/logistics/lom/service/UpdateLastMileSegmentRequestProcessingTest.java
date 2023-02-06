package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.exception.ChangeOrderSegmentException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileSegmentProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class UpdateLastMileSegmentRequestProcessingTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(10L, "1", 1L);

    @Autowired
    private UpdateLastMileSegmentProcessor processor;

    @Autowired
    private DeliveryClient deliveryClient;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-10-02T22:00:00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("Успешный вызов ds-api updateOrder для обновления последней мили")
    @DatabaseSetup("/orders/update_last_mile/segment_request_created.xml")
    void updateLastMileSuccessfully() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(new ClientRequestMeta("1"))
        );
        Order value = orderCaptor.getValue();
        softly.assertThat(value.getLocationTo())
            .isEqualTo(CreateLgwDeliveryEntitiesUtils.createCourierLocationTo().build());
        softly.assertThat(value.getDeliveryDate().getFormattedDate())
            .isEqualTo("2020-10-08T00:00:00+03:00");
        softly.assertThat(value.getDeliveryInterval().getFormattedTimeInterval())
            .isEqualTo("09:00:00+03:00/18:00:00+03:00");
        softly.assertThat(value.getComment()).isEqualTo("Комментарий");
    }

    @Test
    @DisplayName("Успешный вызов ds-api updateOrder с пустым комментарием")
    @DatabaseSetup("/orders/update_last_mile/segment_request_created.xml")
    @DatabaseSetup(
        value = "/orders/update_last_mile/payload_without_comment.xml",
        type = DatabaseOperation.UPDATE
    )
    void updateLastMileSuccessfullyWithEmptyComment() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(new ClientRequestMeta("1"))
        );
        Order value = orderCaptor.getValue();
        softly.assertThat(value.getComment()).isNull();
    }

    @Test
    @DisplayName("Попытка обновления последней мили при отсутствии payload'a приводит к ошибке")
    @DatabaseSetup("/orders/update_last_mile/segment_request_without_payload.xml")
    void doNotUpdateLastMileWithoutPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("No available payload for change request 1 in status INFO_RECEIVED");
    }

    @Test
    @DisplayName("Успешный вызов ds-api updateOrder при отсутствии региона в payload'e заявки")
    @DatabaseSetup("/orders/update_last_mile/segment_request_created.xml")
    @DatabaseSetup(
        value = "/orders/update_last_mile/payload_without_region.xml",
        type = DatabaseOperation.UPDATE
    )
    void updateLastMileSuccessfullyWithoutRegionInPayload() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(new ClientRequestMeta("1"))
        );
        Order value = orderCaptor.getValue();
        softly.assertThat(value.getLocationTo().getRegion()).isEqualTo("Волгоградская область");
    }

    @Test
    @DisplayName("Успешный вызов ds-api updateOrder при отсутствии региона в payload'e заявки и locationTo сегмента")
    @DatabaseSetup("/orders/update_last_mile/segment_request_created.xml")
    @DatabaseSetup(
        value = "/orders/update_last_mile/payload_without_region.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/orders/update_last_mile/location_to_without_region.xml",
        type = DatabaseOperation.UPDATE
    )
    void updateLastMileSuccessfullyWithoutRegionInLocationTo() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(new ClientRequestMeta("1"))
        );
        Order value = orderCaptor.getValue();
        softly.assertThat(value.getLocationTo().getRegion()).isEqualTo("Москва и Московская область");
    }

    @Test
    @DisplayName("Успешный вызов ds-api updateOrder при отсутствии региона в payload'e и locationTo у сегмента")
    @DatabaseSetup("/orders/update_last_mile/segment_request_created.xml")
    @DatabaseSetup(
        value = "/orders/update_last_mile/payload_without_region.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/orders/update_last_mile/segment_without_location_to.xml",
        type = DatabaseOperation.UPDATE
    )
    void updateLastMileSuccessfullyWithoutLocationTo() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(new ClientRequestMeta("1"))
        );
        Order value = orderCaptor.getValue();
        softly.assertThat(value.getLocationTo().getRegion()).isEqualTo("Москва и Московская область");
    }

    @Test
    @DisplayName("Успешный вызов ds-api updateOrder при отсутствии региона в payload'e и адреса в locationTo сегмента")
    @DatabaseSetup("/orders/update_last_mile/segment_request_created.xml")
    @DatabaseSetup(
        value = "/orders/update_last_mile/payload_without_region.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/orders/update_last_mile/location_to_without_address.xml",
        type = DatabaseOperation.UPDATE
    )
    void updateLastMileSuccessfullyWithoutAddressInLocationTo() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(deliveryClient).updateOrder(
            orderCaptor.capture(),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(new ClientRequestMeta("1"))
        );
        Order value = orderCaptor.getValue();
        softly.assertThat(value.getLocationTo().getRegion()).isEqualTo("Москва и Московская область");
    }

    @Test
    @DisplayName("Попытка обновления последней мили с невалидным payload'ом приводит к ошибке")
    @DatabaseSetup("/orders/update_last_mile/segment_request_invalid_payload.xml")
    void doNotUpdateLastMileIfPayloadIsInvalid() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("Payload has validation errors");
    }

    @Test
    @DisplayName("Попытка обновления последней мили с невалидным сегментом приводит к ошибке")
    @DatabaseSetup("/orders/update_last_mile/segment_request_invalid_segment.xml")
    void doNotUpdateLastMileIfSegmentIsInvalid() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Order (1) last mile update is not supported for a segment with partner type FULFILLMENT.");
    }
}
