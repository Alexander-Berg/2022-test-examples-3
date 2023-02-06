package ru.yandex.market.core.order.summary;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.util.functional.Changed;

/**
 * Тесты для {@link PartnerOrderSummaryService}
 */
@ParametersAreNonnullByDefault
public class PartnerOrderSummaryServiceTest extends FunctionalTest {
    private static final long SUPPLIER_ID = 774L;
    private static final long SHOP_ID = 600L;

    @Autowired
    private PartnerOrderSummaryService partnerOrderSummaryService;

    @Test
    @DbUnitDataSet(
            after = "PartnerOrderSummaryServiceTest.testDailyOrdersStatusProcessing.after.csv"
    )
    @DisplayName("Проверка перехода статуса магазина из необработанного в обработанный")
    void testDailyOrdersStatusProcessing() throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        long firstShopId = 777L;
        long secondShopId = 778L;

        Date today = simpleDateFormat.parse("06-01-2021");

        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.PROCESSING,
                today, firstShopId, 1L));
        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.DELIVERED,
                today, firstShopId, 2L));
        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.PENDING,
                today, firstShopId, 2L));
        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.PROCESSING,
                today, firstShopId, 3L));
        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.DELIVERY,
                today, firstShopId, 3L));

        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.DELIVERY,
                today, secondShopId, 1L));
    }

    @Test
    @DbUnitDataSet(
            after = "PartnerOrderSummaryServiceTest.testOrderCreatedYesterday.after.csv"
    )
    @DisplayName("Заказ был создан вчера, и взят в обработку магазином сегодня")
    void testOrderCreatedYesterday() throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        long shopId = 777L;

        Date yesterday = simpleDateFormat.parse("05-01-2021");
        Date today = simpleDateFormat.parse("06-01-02021");

        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.RESERVED,
                yesterday, shopId, 1L));
        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.PROCESSING,
                yesterday, shopId, 2L));

        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.PROCESSING,
                today, shopId, 1L));
        partnerOrderSummaryService.processDBSOrderUpdateEvent(createDBSHistoryOrderUpdateEvent(OrderStatus.DELIVERY,
                today, shopId, 2L));
    }

    private OrderHistoryEvent createDBSHistoryOrderUpdateEvent(OrderStatus newOrderStatus, Date date, long shopId,
                                                               long orderId) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        event.setFromDate(date);

        Order orderAfter = new Order();
        orderAfter.setRgb(Color.WHITE);
        orderAfter.setShopId(shopId);
        orderAfter.setId(orderId);
        orderAfter.setStatus(newOrderStatus);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        orderAfter.setDelivery(delivery);

        event.setOrderAfter(orderAfter);

        return event;
    }
}
