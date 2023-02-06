package ru.yandex.market.checkout.checkouter.tasks.v2.removing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class RemoveFakeOrdersTaskV2Test extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private RemoveFakeOrdersTaskV2 removeFakeOrdersTask;
    @Value("${market.checkouter.oms.service.tms.removeFakeOrders.maxOrderAgeDays:10}")
    private int maxOrderAgeDays;
    @Value("${market.notifier.abo.user}")
    private String[] skipUsers;

    @Test
    public void removesFakeOrder() {
        Instant fakeNow = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(-maxOrderAgeDays - 1)).instant();
        setFixedTime(fakeNow);
        Order orderToDelete = orderServiceHelper.createPostOrder();
        Order orderToDeleteByUid = orderServiceHelper.createPostOrder(
                order -> {
                    Buyer buyer = BuyerProvider.getBuyer();
                    buyer.setUid(Long.valueOf(skipUsers[0]));
                    order.setBuyer(buyer);
                }
        );

        fakeNow = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(-maxOrderAgeDays + 1)).instant();
        setFixedTime(fakeNow);
        Order orderToSkipByDate = orderServiceHelper.createPostOrder(
                order -> {
                    Buyer buyer = BuyerProvider.getBuyer();
                    buyer.setUid(Long.valueOf(skipUsers[0]));
                    order.setBuyer(buyer);
                }
        );
        clearFixed();
        var result = removeFakeOrdersTask.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        Map<Long, Order> orders = orderService.getOrders(
                List.of(
                        orderToDelete.getId(),
                        orderToDeleteByUid.getId(),
                        orderToSkipByDate.getId()
                )
        );

        assertThat(orders.keySet(), hasSize(1));
        assertThat(orders.keySet(), containsInAnyOrder(orderToSkipByDate.getId()));
    }

}
