package ru.yandex.market.abo.cpa.order.delivery;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.order.CpaOrderStatService;
import ru.yandex.market.abo.cpa.order.model.CpaOrderStat;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 18/09/2020.
 */
class ShopOrderDeliveryRepoTest extends EmptyTest {
    private static final long ORDER_ID = 2423423L;
    private static final long SHOP_ID = 321312L;
    @Autowired
    private CpaOrderStatService cpaOrderStatService;
    @Autowired
    private OrderDeliveryRepo orderDeliveryRepo;
    @Autowired
    private ShopOrderDeliveryRepo shopOrderDeliveryRepo;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    void setUp() {
        pgJdbcTemplate.update("INSERT INTO shop (id, name) VALUES (?, ?)", SHOP_ID, SHOP_ID + "-name");
    }

    @AfterEach
    void tearDown() {
        pgJdbcTemplate.update("TRUNCATE shop, cpa_order_delivery, cpa_order_stat; COMMIT");
    }

    @ParameterizedTest
    @CsvSource({"false, 0", "true, -1", "true, -2", "false, -3"})
    void loadOrdersWithEmptyDelivered(boolean orderInLookupWindow, int deliveryDateDiff) {
        Date currentDate = DateUtils.truncate(new Date(), Calendar.DATE);
        Date deliveryDate = DateUtils.addDays(currentDate, deliveryDateDiff);
        CpaOrderStat cpaOrderStat = TestHelper.generateCpaOrderStat(
                ORDER_ID, SHOP_ID, false, LocalDateTime.now(), Color.WHITE);
        cpaOrderStatService.save(List.of(cpaOrderStat));

        OrderDelivery orderDelivery = TestHelper.generateCpaOrderDelivery(ORDER_ID, deliveryDate, DeliveryPartnerType.SHOP);
        orderDeliveryRepo.save(orderDelivery);

        flushAndClear();
        pgJdbcTemplate.update("COMMIT");

        var ordersWithNoDelivered = shopOrderDeliveryRepo.loadOrdersWithEmptyDelivered(2);
        assertEquals(orderInLookupWindow ? 1 : 0, ordersWithNoDelivered.size());
        if (orderInLookupWindow) {
            assertEquals(ORDER_ID, ordersWithNoDelivered.get(0).getOrderId());
            assertEquals(SHOP_ID, ordersWithNoDelivered.get(0).getShopId());
        }
    }
}
