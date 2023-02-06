package ru.yandex.market.checkout.checkouter.order.shop.schedule;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopClient;
import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;
import ru.yandex.market.checkout.checkouter.shop.ShopScheduleSerializationHandler;

public class ScheduleControllerTest extends AbstractWebTestBase {

    private final ShopScheduleSerializationHandler shopScheduleSerializationHandler =
            new ShopScheduleSerializationHandler();
    @Autowired
    private CheckouterShopClient checkouterShopClient;

    @Test
    @RepeatedTest(10)
    public void multiThreadTest() throws Exception {
        final long shopId = 431782L;

        var schedule = new ArrayList<>(shopScheduleSerializationHandler.deserialize("2:540-1140"));
        Thread thread = new Thread(() ->
                checkouterShopClient.pushSchedules(shopId, schedule));

        Thread thread2 = new Thread(() ->
                checkouterShopClient.pushSchedules(shopId, schedule));

        thread.start();
        thread2.start();

        thread.join();
        thread2.join();

        List<ScheduleLine> scheduleLines = checkouterShopClient.getSchedule(shopId);

        Assertions.assertEquals(1, scheduleLines.size());
    }
}
