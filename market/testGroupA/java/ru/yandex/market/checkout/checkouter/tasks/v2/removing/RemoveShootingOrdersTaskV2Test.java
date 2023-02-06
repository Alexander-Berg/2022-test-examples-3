package ru.yandex.market.checkout.checkouter.tasks.v2.removing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.sdk.userinfo.service.UidConstants;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class RemoveShootingOrdersTaskV2Test extends AbstractWebTestBase {
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Value("${market.checkouter.oms.service.tms.removeShootingOrders.maxOrderAgeDays:7}")
    private int maxOrderAgeDays;

    @Test
    public void removeOrders() {
        Instant fakeNow = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(-maxOrderAgeDays - 1)).instant();
        setFixedTime(fakeNow);
        Order orderToSkipByUser = orderServiceHelper.createPostOrder();
        Order orderToDelete = orderServiceHelper.createPostOrder(
                order -> {
                    Buyer buyer = BuyerProvider.getBuyer();
                    buyer.setUid(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint());
                    order.setBuyer(buyer);
                }
        );
        Order orderToDelete2 = orderServiceHelper.createPostOrder(
                order -> {
                    Buyer buyer = BuyerProvider.getBuyer();
                    buyer.setUid(UidConstants.NO_SIDE_EFFECT_UID);
                    order.setBuyer(buyer);
                }
        );

        fakeNow = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(-maxOrderAgeDays + 1)).instant();
        setFixedTime(fakeNow);
        Order orderToSkipByDate = orderServiceHelper.createPostOrder(
                order -> {
                    Buyer buyer = BuyerProvider.getBuyer();
                    buyer.setUid(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint());
                    order.setBuyer(buyer);
                }
        );
        clearFixed();
        tmsTaskHelper.runRemoveShootingOrderTaskV2();

        Map<Long, Order> orders = orderService.getOrders(
                List.of(
                        orderToSkipByUser.getId(),
                        orderToDelete.getId(),
                        orderToDelete2.getId(),
                        orderToSkipByDate.getId()
                )
        );

        assertThat(orders.keySet(), hasSize(2));
        assertThat(orders.keySet(), containsInAnyOrder(orderToSkipByUser.getId(), orderToSkipByDate.getId()));
    }
}
