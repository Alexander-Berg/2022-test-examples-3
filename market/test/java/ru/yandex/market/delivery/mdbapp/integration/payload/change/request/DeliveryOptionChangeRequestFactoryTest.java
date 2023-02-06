package ru.yandex.market.delivery.mdbapp.integration.payload.change.request;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryOptionChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.UpdateOrderDeliveryDateDto;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.enums.UpdateRequestStatus;
import ru.yandex.market.delivery.mdbapp.integration.converter.ChangeRequestStatusConverter;
import ru.yandex.market.delivery.mdbapp.util.DeliveryDateUpdateReason;

@DisplayName("Проверка обработки заявки на измененеи опций доставки")
class DeliveryOptionChangeRequestFactoryTest extends AbstractTest {

    private static final LocalDate FROM_DATE = LocalDate.of(2022, 6, 1);
    private static final LocalDate TO_DATE = LocalDate.of(2022, 6, 2);
    private static final LocalTime FROM_TIME = LocalTime.of(10, 0);
    private static final LocalTime TO_TIME = LocalTime.of(18, 0);
    private static final Long DELIVERY_SERVICE_ID = 201L;
    private static final Long ORDER_ID = 101L;
    private static final Long TARIFF_ID = 101L;
    private static final Long CHANGE_REQUEST_ID = 1L;
    private static final Long EVENT_ID = 1L;

    private final FeatureProperties featureProperties = new FeatureProperties();

    private final DeliveryOptionChangeRequestFactory factory = new DeliveryOptionChangeRequestFactory(
        new ChangeRequestStatusConverter(),
        featureProperties
    );

    @BeforeEach
    void setup() {
        featureProperties.setProcessDeliveryOptionChangeRequests(true);
    }

    @Test
    @DisplayName("Успешно")
    void success() {
        ChangeRequestInternal<UpdateOrderDeliveryDateDto> requestInternal = factory.create(
            getDeliveryOptionChangeEvent(DELIVERY_SERVICE_ID),
            getDeliveryOptionChangeRequest(DELIVERY_SERVICE_ID)
        );
        softly.assertThat(requestInternal)
            .usingRecursiveComparison()
            .isEqualTo(
                new DeliveryDateChangeRequestInternal(
                    new UpdateOrderDeliveryDateDto(
                        ORDER_ID,
                        null,
                        CHANGE_REQUEST_ID,
                        null,
                        FROM_DATE.atStartOfDay(),
                        TO_DATE.atStartOfDay(),
                        FROM_TIME,
                        TO_TIME,
                        DELIVERY_SERVICE_ID,
                        null,
                        DeliveryDateUpdateReason.UNKNOWN
                    ),
                    UpdateRequestStatus.NEW
                )
            );
    }

    @Test
    @DisplayName("Обработка отключена")
    void disabled() {
        featureProperties.setProcessDeliveryOptionChangeRequests(false);
        ChangeRequestInternal<UpdateOrderDeliveryDateDto> requestInternal = factory.create(
            getDeliveryOptionChangeEvent(DELIVERY_SERVICE_ID),
            getDeliveryOptionChangeRequest(DELIVERY_SERVICE_ID)
        );
        softly.assertThat(requestInternal).isNull();
    }

    @Test
    @DisplayName("Есть изменение СД")
    void deliveryServiceChanged() {
        ChangeRequestInternal<UpdateOrderDeliveryDateDto> requestInternal = factory.create(
            getDeliveryOptionChangeEvent(202L),
            getDeliveryOptionChangeRequest(202L)
        );
        softly.assertThat(requestInternal).isNull();
    }

    @Nonnull
    private OrderHistoryEvent getDeliveryOptionChangeEvent(
        Long newDeliveryServiceId
    ) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(EVENT_ID);
        event.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);
        Order orderAfter = new Order();
        orderAfter.setId(ORDER_ID);
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        delivery.setTariffId(TARIFF_ID);
        orderAfter.setDelivery(delivery);
        orderAfter.setChangeRequests(List.of(getDeliveryOptionChangeRequest(newDeliveryServiceId)));
        event.setOrderAfter(orderAfter);
        return event;
    }

    @Nonnull
    private ChangeRequest getDeliveryOptionChangeRequest(Long deliveryServiceId) {
        DeliveryOptionChangeRequestPayload payload = new DeliveryOptionChangeRequestPayload();
        payload.setDeliveryServiceId(deliveryServiceId);
        payload.setTariffId(TARIFF_ID);
        payload.setFromDate(FROM_DATE);
        payload.setToDate(TO_DATE);
        payload.setTimeInterval(new TimeInterval(FROM_TIME, TO_TIME));
        return new ChangeRequest(
            CHANGE_REQUEST_ID,
            ORDER_ID,
            payload,
            ChangeRequestStatus.NEW,
            Instant.now(),
            null,
            ClientRole.SYSTEM
        );
    }
}
