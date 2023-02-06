package ru.yandex.market.checkout.checkouter.tasks.v2.delivery;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.DeliveryDeadlineStatus;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class DeliveryDeadlineNowTaskV2Test extends AbstractWebTestBase {

    private static final String UPDATE_ORDER_DELIVERY_TO_DATE =
            "update order_delivery SET to_date = ? where order_delivery.id = ?";

    private static final String UPDATE_PARCEL_PACKAGING_TIME =
            "update parcel SET packaging_time = ? where parcel.id = ?";

    @Autowired
    private DeliveryDeadlineNowTaskV2 deliveryDeadlineNowTaskV2;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void shouldCorrectStayInProgress() {
        Order order = createOrderExpectsPackagingTime();

        // Сетим: Доставка сегодня, packagingTime 2 часа в будущем
        LocalDate deliveryToDate = LocalDate.now(getClock());
        Instant packagingTime = Instant.now(getClock())
                .plus(2, ChronoUnit.HOURS);

        setPackagingTimeAndDeliveryToDate(order, deliveryToDate, packagingTime);

        deliveryDeadlineNowTaskV2.run(TaskRunType.ONCE);

        Order orderAfter = orderService.getOrder(order.getId());
        Parcel parcelAfter = orderAfter.getDelivery().getParcels().iterator().next();
        assertEquals(DeliveryDeadlineStatus.PROGRESS, parcelAfter.getDeliveryDeadlineStatus());
    }

    @Test
    public void shouldCorrectSetDeliveryDeadlineStatusNow() {
        Order order = createOrderExpectsPackagingTime();

        // Сетим: Доставка сегодня, профакапили packagingTime 2 часа назад
        LocalDate deliveryToDate = LocalDate.now(getClock());
        Instant packagingTime = Instant.now(getClock())
                .minus(2, ChronoUnit.HOURS);

        setPackagingTimeAndDeliveryToDate(order, deliveryToDate, packagingTime);

        deliveryDeadlineNowTaskV2.run(TaskRunType.ONCE);

        Order orderAfter = orderService.getOrder(order.getId());
        Parcel parcelAfter = orderAfter.getDelivery().getParcels().iterator().next();
        assertEquals(DeliveryDeadlineStatus.DELIVERY_DATES_DEADLINE_NOW, parcelAfter.getDeliveryDeadlineStatus());
    }

    @Nonnull
    private Order createOrderExpectsPackagingTime() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withPackagingTime(Duration.ofHours(2))
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        orderStatusHelper.updateOrderStatus(order.getId(), PROCESSING, READY_TO_SHIP);

        Parcel parcel = order.getDelivery().getParcels().iterator().next();
        assertEquals(DeliveryDeadlineStatus.PROGRESS, parcel.getDeliveryDeadlineStatus());
        return order;
    }

    private void setPackagingTimeAndDeliveryToDate(Order order,
                                                   LocalDate deliveryToDate,
                                                   Instant packagingTime) {
        Long internalDeliveryId = order.getInternalDeliveryId();

        Parcel parcel = order.getDelivery().getParcels().iterator().next();
        Long parcelId = parcel.getId();

        transactionTemplate.execute(status -> {
            masterJdbcTemplate.update(UPDATE_ORDER_DELIVERY_TO_DATE,
                    java.sql.Date.valueOf(deliveryToDate), internalDeliveryId);


            masterJdbcTemplate.update(UPDATE_PARCEL_PACKAGING_TIME,
                    Timestamp.from(packagingTime), parcelId);

            return null;
        });
    }

}
