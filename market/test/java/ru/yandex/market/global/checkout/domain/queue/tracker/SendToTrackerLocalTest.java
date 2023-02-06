package ru.yandex.market.global.checkout.domain.queue.tracker;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.TrackerNotificationDetails;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.TrackerNotificationService;
import ru.yandex.market.global.checkout.domain.startrek.GlobalSupportTags;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;

import static ru.yandex.market.global.common.util.StringFormatter.sf;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SendToTrackerLocalTest extends BaseLocalTest {

    private final TrackerNotificationService trackerNotificationService;
    private final TestOrderFactory testOrderFactory;

    @Test
    @SneakyThrows
    public void test() {
        OrderModel order = testOrderFactory.createOrder();

        trackerNotificationService.createTickets(new TrackerNotificationDetails()
                .setUniquePrefix("SendToTrackerLocalTest_test")
                .setOrder(order.getOrder())
                .setOrderDelivery(order.getOrderDelivery())
                .setTitle(sf("Call merchant {} — order {} takes a long time to collect",
                        order.getOrderDelivery().getShopName(),
                        order.getOrder().getId()
                ))
                .setHeader(sf("Call merchant immediately — order takes a long time to collect"))
                .setInternalTags(List.of(GlobalSupportTags.NOTIFY_2)));

        Thread.sleep(10000);
    }

}
