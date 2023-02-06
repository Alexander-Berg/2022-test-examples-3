package ru.yandex.market.partner.notification.transport.webui;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.notification.common.model.destination.ShopDestination;
import ru.yandex.market.notification.model.WebContent;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.simple.model.context.NotificationTransportContextImpl;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.transport.webui.model.notification.WebAddress;

public class WebTransportServiceTest extends AbstractFunctionalTest {
    private static long TEST_SHOP_ID = 100L;
    private static long TEST_USER_ID = 200L;
    private static long TEST_TEMPLATE_ID = 2L;

    @Autowired
    WebTransportService webTransportService;

    @Autowired
    TestableClock clock;

    @Test
    @DbUnitDataSet(after = "shouldSaveWebUIMessage.after.csv")
    public void shouldSaveAndSendToMbiWebUIMessageWithUserIdSpecified() {
        clock.setFixed(Instant.parse("2022-04-03T16:16:16Z"), ZoneId.of("UTC"));
        webTransportService.send(
                new NotificationTransportContextImpl(
                        WebContent.create(
                                "subject",
                                "body",
                                TEST_TEMPLATE_ID,
                                TEST_SHOP_ID,
                                TEST_USER_ID,
                                1
                        ),
                        List.of(new WebAddress()),
                        new PersistentNotification(1L, 2L, new CodeNotificationType(TEST_TEMPLATE_ID), null, null, null, null, null,
                                false),
                        List.of(ShopDestination.create(TEST_SHOP_ID))
                )
        );
    }

}
