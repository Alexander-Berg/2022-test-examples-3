package ru.yandex.market.checkout.checkouter.events;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.ConfirmationReason;
import ru.yandex.market.checkout.helpers.utils.EventsTestUtils;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CANCELLATION_REQUESTED;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper.findEvents;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;

public class EventsControllerCancellationChangeRequestTest extends AbstractEventsControllerTestBase {

    @Autowired
    private Clock clock;

    public static Stream<Arguments> parameterizedTestData() {
        return EventsTestUtils.parameters(WHITE).stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void testMultipleCancellationEventsWithoutSubstatusesMap(String caseName,
                                                                    EventsTestUtils.EventGetter eventGetter)
            throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        var parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfDelivery()
                .dates(DeliveryDates.deliveryDates(clock, 0, 2))
                .buildResponse(DeliveryResponse::new));
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());

        var order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        // Создаем заявку на отмену заказа от имени покупателя
        var userEditRequest = new OrderEditRequest();
        userEditRequest.setCancellationRequest(new CancellationRequest(USER_CHANGED_MIND, "user changed mind"));
        var crList = client.editOrder(order.getId(), ClientRole.USER, order.getBuyer().getUid(), List.of(WHITE),
                userEditRequest);

        var userChangeRequest = crList
                .stream()
                .filter(cr -> cr.getType() == ChangeRequestType.CANCELLATION
                        && cr.getRole() == ClientRole.USER)
                .findAny()
                .orElse(null);
        assertThat(userChangeRequest, notNullValue());

        // Переводим заявку на отмену заказа в статус REJECTED от имени магазина
        client.updateChangeRequestStatus(
                order.getId(),
                userChangeRequest.getId(),
                ClientRole.SHOP,
                order.getShopId(),
                new ChangeRequestPatchRequest(
                        ChangeRequestStatus.REJECTED,
                        null,
                        new CancellationRequestPayload(
                                USER_CHANGED_MIND,
                                null,
                                null,
                                ConfirmationReason.DELIVERY
                        )
                )
        );

        // Создаем еще одну заявку на отмену заказа от имени покупателя
        userEditRequest = new OrderEditRequest();
        userEditRequest.setCancellationRequest(new CancellationRequest(USER_CHANGED_MIND, "user changed mind"));
        crList = client.editOrder(order.getId(), ClientRole.USER, order.getBuyer().getUid(), List.of(WHITE),
                userEditRequest);
        userChangeRequest = crList
                .stream()
                .filter(cr -> cr.getType() == ChangeRequestType.CANCELLATION
                        && cr.getRole() == ClientRole.USER)
                .findAny()
                .orElse(null);
        assertThat(userChangeRequest, notNullValue());

        // Переводим заявку на отмену заказа в статус APPLIED от имени магазина
        client.updateChangeRequestStatus(
                order.getId(),
                userChangeRequest.getId(),
                ClientRole.SHOP,
                order.getShopId(),
                new ChangeRequestPatchRequest(
                        ChangeRequestStatus.APPLIED,
                        null,
                        null
                )
        );

        // Проверяем что заказ отменился после второй заявки
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(OrderStatus.CANCELLED));

        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);

        // Проверяем что по заказу было 2 события с типом ORDER_CANCELLATION_REQUESTED
        var filteredEvents = findEvents(events, ORDER_CANCELLATION_REQUESTED);
        assertThat(filteredEvents, hasSize(2));
    }
}
