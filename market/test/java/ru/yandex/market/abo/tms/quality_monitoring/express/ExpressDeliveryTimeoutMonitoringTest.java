package ru.yandex.market.abo.tms.quality_monitoring.express;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.order.delivery.OrderDelivery;
import ru.yandex.market.abo.cpa.order.delivery.OrderDeliveryRepo;
import ru.yandex.market.abo.cpa.order.model.CpaOrderStat;
import ru.yandex.market.abo.cpa.order.service.CpaOrderStatRepo;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;

/**
 * @author komarovns
 */
class ExpressDeliveryTimeoutMonitoringTest extends EmptyTest {
    private static final AtomicLong COUNTER = new AtomicLong();
    private static final long SHOP_1 = 1;
    private static final long SHOP_2 = 2;

    ExpressDeliveryTimeoutMonitoring expressDeliveryTimeoutMonitoring;

    @Autowired
    CpaOrderStatRepo cpaOrderStatRepo;
    @Autowired
    OrderDeliveryRepo orderDeliveryRepo;
    @Autowired
    ConfigurationService coreCounterService;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Mock
    ExpressDeliveryTimeoutMonitoringStTicketCreator stTicketCreator;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        expressDeliveryTimeoutMonitoring = new ExpressDeliveryTimeoutMonitoring(
                jdbcTemplate, coreCounterService, stTicketCreator
        );
    }

    @Test
    void findExpiredOrdersTest() {
        var now = new Date();
        var maxProcessed_1 = minusMinutes(now, 40);
        var maxProcessed_2 = minusMinutes(now, 45);
        var expectedOrdersByShop = Map.of(
                SHOP_1, Set.of(
                        order(SHOP_1, maxProcessed_1, null, PENDING, null, true),
                        order(SHOP_1, null, minusMinutes(maxProcessed_1, 5), PROCESSING, OrderSubstatus.STARTED, true)
                ),
                SHOP_2, Set.of(
                        order(SHOP_2, null, maxProcessed_2, PROCESSING, OrderSubstatus.STARTED, true),
                        order(SHOP_2, minusMinutes(maxProcessed_2, 10), null, PENDING, null, true)
                )
        );
        // express=false
        order(SHOP_1, maxProcessed_1, null, PENDING, null, false);
        // pending раньше последней проверки
        order(SHOP_1, minusMinutes(now, 130), null, PENDING, null, true);
        // не прошло половины часа
        order(SHOP_1, minusMinutes(now, 15), null, PENDING, null, true);
        // текущий статус DELIVERY
        order(SHOP_2, maxProcessed_2, null, DELIVERY, null, true);
        // статус PROCESSING, но сабстатус READY_TO_SHIP
        order(SHOP_2, null, maxProcessed_2, PROCESSING, READY_TO_SHIP, true);

        var maxProcessedByShop = Map.of(SHOP_1, maxProcessed_1, SHOP_2, maxProcessed_2);
        var expiredOrders = expressDeliveryTimeoutMonitoring.findExpiredOrders(minusMinutes(now, 120));
        expiredOrders.forEach(e -> assertEquals(
                maxProcessedByShop.get(e.shopId).getTime(),
                e.processingDate.getTime()
        ));
        var ordersByShop = StreamEx.of(expiredOrders)
                .toMap(e -> e.shopId, e -> Set.copyOf(e.orders));
        assertEquals(expectedOrdersByShop, ordersByShop);
    }

    /**
     * Один заказ обрабатывается ровно 1 раз
     */
    @Test
    void updateLastProcessingTest() {
        var orderId = order(SHOP_1, minusMinutes(new Date(), 40), null, PENDING, null, true);
        expressDeliveryTimeoutMonitoring.createStartrekTickets();
        expressDeliveryTimeoutMonitoring.createStartrekTickets();
        verify(stTicketCreator, only()).createTicket(SHOP_1, List.of(orderId));
    }

    private long order(long shopId,
                       Date pending,
                       Date processing,
                       OrderStatus status,
                       OrderSubstatus substatus,
                       boolean express
    ) {
        var orderId = COUNTER.incrementAndGet();

        var stat = new CpaOrderStat();
        stat.setOrderId(orderId);
        stat.setShopId(shopId);
        stat.setPending(pending);
        stat.setProcessing(processing);
        stat.setStatus(status);
        stat.setSubstatus(substatus);
        cpaOrderStatRepo.save(stat);

        var delivery = new OrderDelivery();
        delivery.setOrderId(orderId);
        delivery.setExpress(express);
        orderDeliveryRepo.save(delivery);

        flushAndClear();
        return orderId;
    }

    private static Date minusMinutes(Date date, long mins) {
        return new Date(date.getTime() - TimeUnit.MINUTES.toMillis(mins));
    }
}
