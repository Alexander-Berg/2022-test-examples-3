package ru.yandex.market.checkout.checkouter.antifraud;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.util.matching.OrderFailureResultMatcher;

import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

@Disabled
public class FFPromoOrdersAntifraudTest extends AbstractFFPromoAntifraudTestBase {

    public static final OrderFailureResultMatcher FF_PROMO_FRAUD_MATCHER =
            new OrderFailureResultMatcher("FRAUD_DETECTED", "FFPromoUserFraudDetector");

    private static final Long UID = 3728474L;
    private static final Long MUID = 1152921504648203038L;
    private static final String PHONE = "+74952234562";
    private static final String EMAIL = "user@yandex.ru";

    public static final int EXPECTED_ORDER_LIMIT = 4;

    @Test
    public void testDifferentOrders() {

        createOrder(true, false, true); // Out of scope: not FF
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING); // Out of scope even in PROCESSING
        createOrder(true, true, false); // Out of scope: not Promo
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY); // Out of scope even in DELIVERY
        createOrder(false, false, false); // Out of scope: not FF, not Promo
        orderStatusHelper.proceedOrderToStatus(order, DELIVERED); // Out of scope even in DELIVERED
        createOrder(true, false, false); // Out of scope: not FF, not Promo
        orderUpdateService.updateOrderStatus(order.getId(), CANCELLED, OrderSubstatus.USER_CHANGED_MIND); // Out of
        // scope: been cancelled
        createOrder(true, true, false); // Out of scope: not Promo
        jumpToPast(25, ChronoUnit.HOURS);
        createOrder(true, true, true); // Out of scope: created more than 24 hours ago
        clearFixed();

        for (int i = 0; i < EXPECTED_ORDER_LIMIT; i++) {
            createOrder(true, true, true);
        }
        createOrder(true, true, true, null, FF_PROMO_FRAUD_MATCHER); // Exceeds limit of 5
    }

    @Test
    public void testDifferentBuyers() {
        tryFraudWithBuyer(() -> newBuyer(UID, randomPhone(), randomEmail()));
        tryFraudWithBuyer(() -> newBuyer(MUID, randomPhone(), randomEmail()));
        tryFraudWithBuyer(() -> newBuyer(randomUid(), PHONE, randomEmail()));
        tryFraudWithBuyer(() -> newBuyer(randomUid(), randomPhone(), EMAIL));
    }

}
