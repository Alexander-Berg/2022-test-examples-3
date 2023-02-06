package ru.yandex.market.checkout.checkouter.order.status.actions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.ActualDeliveryBuilder.DEFAULT_INTAKE_SHIPMENT_DAYS;

public class CalculateOutletStorageLimitDateActionTest extends AbstractWebTestBase {

    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;
    @Autowired
    private TestableClock clock;

    @Test
    public void shouldCalculateAndWriteOutletStorageLimitDate() {
        //given:
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        var expectedDate = LocalDate.now().plusDays(order.getDelivery().getOutletStoragePeriod());

        //when:
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        //then:
        order = orderService.getOrder(order.getId());
        assertEquals(order.getDelivery().getOutletStorageLimitDate(), expectedDate);
    }

    @Test
    public void shouldCalculateAndWriteActualOutletStorageLimitDate() {
        clock.setFixed(Instant.parse("2022-05-18T10:15:30.00Z"), ZoneId.systemDefault());
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_HOLIDAYS_FOR_STORAGE_LIMIT_DATE, true);

        //given:
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        // 5 дней storageLimitPeriod + 2 выходных (2022-05-21, 2022-05-22) + 2 праздничных (2022-05-24, 2022-05-25)
        // выходные и праздничные дни замоканы в files/report/outlets.xml
        var expectedDate = LocalDate.parse("2022-05-27");

        //when:
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        //then:
        order = orderService.getOrder(order.getId());
        assertEquals(order.getDelivery().getOutletStorageLimitDate(), expectedDate);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_HOLIDAYS_FOR_STORAGE_LIMIT_DATE, false);
        clock.clearFixed();
    }

    @Test
    public void shouldCalculateAndWriteActualOutletStorageLimitDatePickupDateIsHoliday() {
        clock.setFixed(Instant.parse("2022-05-25T10:15:30.00Z"), ZoneId.systemDefault());
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_HOLIDAYS_FOR_STORAGE_LIMIT_DATE, true);

        //given:
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(100505L,
                                DEFAULT_INTAKE_SHIPMENT_DAYS,
                                Collections.singletonList(12312306L))
                        .build())
                .build();

        // 7 дней storageLimitPeriod + 4 выходных (2022-05-28, 2022-05-29, 2022-06-04, 2022-06-05)
        // + 2 праздничных (2022-05-25, 2022-06-01)
        // выходные и праздничные дни замоканы в files/report/outlets.xml
        var expectedDate = LocalDate.parse("2022-06-06");

        //when:
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        //then:
        order = orderService.getOrder(order.getId());
        assertEquals(order.getDelivery().getOutletStorageLimitDate(), expectedDate);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_HOLIDAYS_FOR_STORAGE_LIMIT_DATE, false);
        clock.clearFixed();
    }

    @Test
    public void shouldNotCalculateAndWriteOutletStorageLimitDateIfNotPickupStatus() {
        //given:
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        //when:
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        //then:
        order = orderService.getOrder(order.getId());
        assertNull(order.getDelivery().getOutletStorageLimitDate());
    }

    @Test
    public void shouldNotCalculateAndWriteOutletStorageLimitDateIfPeriodAbsent() {
        //given:
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID, 1, List.of(12312302L))
                        .build())
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        order = orderService.getOrder(order.getId());
        assertNull(order.getDelivery().getOutletStoragePeriod());

        //when:
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        //then:
        order = orderService.getOrder(order.getId());
        assertNull(order.getDelivery().getOutletStorageLimitDate());
    }
}
