package ru.yandex.market.delivery.mdbapp.integration.router;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import steps.orderSteps.OrderSteps;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.parcel.ParcelCancelChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrderMilestoneTimingsTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.service.dbs.DbsDeliveryServiceConfigService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_PAYMENT;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CHANGE_REQUEST_CREATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_DELIVERY_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_SUBSTATUS_UPDATED;
import static ru.yandex.market.delivery.mdbapp.integration.router.DbsRouter.CHANNEL_CREATE_COURIER_ORDER_DROPSHIP_BY_SELLER;
import static ru.yandex.market.delivery.mdbapp.integration.router.DbsRouter.CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER;
import static ru.yandex.market.delivery.mdbapp.integration.router.DbsRouter.CHANNEL_CREATE_PICKUP_ORDER_THROUGH_DROPOFF_DROPSHIP_BY_SELLER;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsByTypeRouter.CHANNEL_DISCARDED;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_CHANGE_REQUEST_CREATED;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_FIRST_TIME_CANCELLED_ORDER;
import static ru.yandex.market.delivery.mdbapp.util.DbsOrderEventUtils.event;
import static ru.yandex.market.delivery.mdbapp.util.DbsOrderEventUtils.eventDbsWithRoute;
import static ru.yandex.market.delivery.mdbapp.util.DbsOrderEventUtils.eventWithNotBrandedPvz;
import static ru.yandex.market.delivery.mdbapp.util.DbsOrderEventUtils.orderAfter;

@RunWith(Parameterized.class)
@DisplayName("Роутинг операций с ДБС-заказами")
public class DbsRouterTest {

    @Parameter
    public String name;

    @Parameter(1)
    public OrderHistoryEvent orderHistoryEvent;

    @Parameter(2)
    public String channel;

    @SuppressWarnings("checkstyle:MethodLength")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
            {
                "Курьер: оба идентификатора сд пустые",
                event(ORDER_DELIVERY_UPDATED, (Long) null, null),
                CHANNEL_DISCARDED
            },
            {
                "Курьер: идентификатор сд в заказе после пустой",
                event(ORDER_DELIVERY_UPDATED, 100L, null),
                CHANNEL_DISCARDED
            },
            {
                "Курьер: идентификатор сд правильный, но не поменялся",
                event(ORDER_DELIVERY_UPDATED, 100L, 100L),
                CHANNEL_DISCARDED
            },
            {
                "Курьер: идентификатор сд не поменялся",
                event(ORDER_DELIVERY_UPDATED, 1L, 1L),
                CHANNEL_DISCARDED
            },
            {
                "Курьер: идентификатор сд не поменялся - новый идентификатор неправильный",
                event(ORDER_DELIVERY_UPDATED, 100L, 1L),
                CHANNEL_DISCARDED
            },

            {
                "ПВЗ (изменение статуса заказа): оба идентификатора сд пустые",
                event(ORDER_STATUS_UPDATED, (Long) null, null),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение статуса заказа): идентификатор сд в заказе после пустой",
                event(ORDER_STATUS_UPDATED, 100L, null),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение статуса заказа): идентификатор сд правильный, но не поменялся",
                event(ORDER_STATUS_UPDATED, 100L, 100L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение статуса заказа): идентификатор сд не поменялся",
                event(ORDER_STATUS_UPDATED, 1L, 1L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение статуса заказа): идентификатор сд не поменялся - новый идентификатор неправильный",
                event(ORDER_STATUS_UPDATED, 100L, 1L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },

            {
                "ПВЗ (изменение сабстатуса заказа): оба идентификатора сд пустые",
                event(ORDER_SUBSTATUS_UPDATED, (Long) null, null),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение сабстатуса заказа): идентификатор сд в заказе после пустой",
                event(ORDER_SUBSTATUS_UPDATED, 100L, null),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение сабстатуса заказа): идентификатор сд правильный, но не поменялся",
                event(ORDER_SUBSTATUS_UPDATED, 100L, 100L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение сабстатуса заказа): идентификатор сд не поменялся",
                event(ORDER_SUBSTATUS_UPDATED, 1L, 1L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение сабстатуса заказа): идентификатор сд не поменялся - новый идентификатор неправильный",
                event(ORDER_SUBSTATUS_UPDATED, 100L, 1L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },

            {
                "Курьер: валидное изменение сд с непустого",
                event(ORDER_DELIVERY_UPDATED, 1L, 100L),
                CHANNEL_CREATE_COURIER_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "Курьер: валидное изменение сд с пустого",
                event(ORDER_DELIVERY_UPDATED, null, 100L),
                CHANNEL_CREATE_COURIER_ORDER_DROPSHIP_BY_SELLER
            },

            {
                "ПВЗ (изменение статуса) : валидное изменение сд с непустого",
                event(ORDER_SUBSTATUS_UPDATED, 1L, 100L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение статуса) : валидное изменение сд с пустого",
                event(ORDER_SUBSTATUS_UPDATED, null, 100L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение сабстатуса) : валидное изменение сд с непустого",
                event(ORDER_STATUS_UPDATED, 1L, 100L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "ПВЗ (изменение сабстатуса) : валидное изменение сд с пустого",
                event(ORDER_STATUS_UPDATED, null, 100L),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },

            {
                "Невалидный тип события",
                event(NEW_PAYMENT, null, 100L),
                CHANNEL_DISCARDED
            },

            //заказы не белые
            {
                "Обновление сабстатуса: невалидный цвет заказа",
                event(ORDER_SUBSTATUS_UPDATED, orderAfter(Color.BLUE)),
                CHANNEL_DISCARDED
            },
            {
                "Обновление статуса: невалидный цвет заказа",
                event(ORDER_STATUS_UPDATED, orderAfter(Color.BLUE)),
                CHANNEL_DISCARDED
            },

            //ПВЗ (изменение статуса заказа) не брендирован
            {
                "Обновление сабстатуса: пвз не брендирован",
                eventWithNotBrandedPvz(ORDER_SUBSTATUS_UPDATED),
                CHANNEL_DISCARDED
            },
            {
                "Обновление статуса: пвз не брендирован",
                eventWithNotBrandedPvz(ORDER_STATUS_UPDATED),
                CHANNEL_DISCARDED
            },

            //тип партнера не SHOP
            {
                "Обновление сабстатуса: невалидный тип партнера",
                event(ORDER_SUBSTATUS_UPDATED, orderAfter(DeliveryPartnerType.YANDEX_MARKET)),
                CHANNEL_DISCARDED
            },
            {
                "Обновление статуса: невалидный тип партнера",
                event(ORDER_STATUS_UPDATED, orderAfter(DeliveryPartnerType.YANDEX_MARKET)),
                CHANNEL_DISCARDED
            },

            //статус заказа изменился на невалидный
            {
                "Обновление сабстатуса: статус изменился на некорректный",
                event(ORDER_SUBSTATUS_UPDATED, OrderStatus.DELIVERY),
                CHANNEL_DISCARDED
            },
            {
                "Обновление статуса: статус изменился на некорректный",
                event(ORDER_STATUS_UPDATED, OrderStatus.DELIVERED),
                CHANNEL_DISCARDED
            },

            //сабстатус заказа изменился на невалидный
            {
                "Обновление сабстатуса: сабстатус изменился на некорректный",
                event(ORDER_SUBSTATUS_UPDATED, OrderSubstatus.SHIPPED),
                CHANNEL_DISCARDED
            },
            {
                "Обновление статуса: сабстатус изменился на некорректный",
                event(ORDER_STATUS_UPDATED, OrderSubstatus.SHIPPED),
                CHANNEL_DISCARDED
            },

            // успех в процессинг
            {
                "Обновление сабстатуса: успех из процессинга",
                event(ORDER_SUBSTATUS_UPDATED, OrderStatus.PROCESSING, OrderSubstatus.STARTED),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "Обновление статуса: успех из процессинга",
                event(ORDER_STATUS_UPDATED, OrderStatus.PROCESSING, OrderSubstatus.STARTED),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },

            // успех в пендинг
            {
                "Обновление сабстатуса: успех из пендинга",
                event(ORDER_SUBSTATUS_UPDATED, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            {
                "Обновление статуса: успех из пендинга",
                event(ORDER_STATUS_UPDATED, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION),
                CHANNEL_CREATE_PICKUP_ORDER_DROPSHIP_BY_SELLER
            },
            // через дропофф
            // успех в процессинг
            {
                "Обновление сабстатуса: успех из процессинга",
                eventDbsWithRoute(ORDER_SUBSTATUS_UPDATED, OrderStatus.PROCESSING, OrderSubstatus.STARTED),
                CHANNEL_CREATE_PICKUP_ORDER_THROUGH_DROPOFF_DROPSHIP_BY_SELLER
            },
            {
                "Обновление статуса: успех из процессинга",
                eventDbsWithRoute(ORDER_STATUS_UPDATED, OrderStatus.PROCESSING, OrderSubstatus.STARTED),
                CHANNEL_CREATE_PICKUP_ORDER_THROUGH_DROPOFF_DROPSHIP_BY_SELLER
            },

            // успех в пендинг
            {
                "Обновление сабстатуса: успех из пендинга",
                eventDbsWithRoute(ORDER_SUBSTATUS_UPDATED, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION),
                CHANNEL_CREATE_PICKUP_ORDER_THROUGH_DROPOFF_DROPSHIP_BY_SELLER
            },
            {
                "Обновление статуса: успех из пендинга",
                eventDbsWithRoute(ORDER_STATUS_UPDATED, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION),
                CHANNEL_CREATE_PICKUP_ORDER_THROUGH_DROPOFF_DROPSHIP_BY_SELLER
            },
            //невалидный статус
            {
                "Обновление статуса: заказ не создан",
                eventDbsWithRoute(ORDER_STATUS_UPDATED, OrderStatus.UNKNOWN, OrderSubstatus.AWAIT_CONFIRMATION),
                CHANNEL_DISCARDED
            },
            //отмены
            {
                "ПВЗ: валидная отмена через создание заявки на отмену (USER, CALL_CENTER_OPERATOR)",
                event(
                    ORDER_CHANGE_REQUEST_CREATED,
                    OrderStatus.PICKUP,
                    OrderStatus.PICKUP,
                    List.of(parcelCancelChangeRequest())
                ),
                CHANNEL_CHANGE_REQUEST_CREATED
            },
            {
                "ПВЗ: валидная отмена через перевод в статус CANCELLED (SHOP)",
                event(
                    ORDER_STATUS_UPDATED,
                    OrderStatus.DELIVERY,
                    OrderStatus.CANCELLED
                ),
                CHANNEL_FIRST_TIME_CANCELLED_ORDER
            },
            {
                "ПВЗ: у заказа нет новых чейндж-реквестов",
                event(ORDER_STATUS_UPDATED, OrderStatus.PICKUP, OrderStatus.PICKUP),
                CHANNEL_DISCARDED
            }
        });
    }

    @Nonnull
    private static ChangeRequest parcelCancelChangeRequest() {
        return new ChangeRequest(
            1L,
            OrderSteps.ID,
            new ParcelCancelChangeRequestPayload(1L, OrderSubstatus.PICKUP_SERVICE_RECEIVED, null, null),
            ChangeRequestStatus.NEW,
            Instant.now(),
            null,
            ClientRole.USER
        );
    }

    @Test
    public void routeTest() {
        DbsDeliveryServiceConfigService dbsDeliveryServiceConfigService = mock(DbsDeliveryServiceConfigService.class);
        BackLogOrderMilestoneTimingsTskvLogger tskvLogger = new BackLogOrderMilestoneTimingsTskvLogger(
            new EventFlowParametersHolder(),
            new TestableClock()
        );

        Mockito.when(dbsDeliveryServiceConfigService.getAllIds())
            .thenReturn(Set.of(100L));

        assertEquals(
            "Route to correct channel",
            channel,
            new DbsRouter(dbsDeliveryServiceConfigService, tskvLogger).route(orderHistoryEvent)
        );

        Mockito.reset(dbsDeliveryServiceConfigService);
    }
}
