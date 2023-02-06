package ru.yandex.market.partner.notification.service.providers.data;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.notification.common.model.TelegramButtonContent;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.simple.model.context.NotificationContextImpl;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.partner.notification.api.XmlToNotificationContextDataConverter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.EMAIL;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.TELEGRAM_BOT;

class TelegramDataProviderTest {
    private static final long COMMON_ACCEPT_ORDER_TEMPLATE_ID = 1612872409;
    private static final long DSBS_ACCEPT_ORDER_TEMPLATE_ID = 1631026286;
    private static final long DSBS_ACCEPT_ORDER_REMINDER_TEMPLATE_ID = 1563377944;

    private final TelegramDataProvider provider = new TelegramDataProvider();

    @Test
    void provideDbsAcceptOrderButton() throws IOException, JDOMException {
        var context = createContext(TELEGRAM_BOT, DSBS_ACCEPT_ORDER_TEMPLATE_ID, "dsbs_accept_order_data.xml");

        assertThat(provider.provide(context), equalTo(
                new ArrayListNotificationData<>(List.of(TelegramButtonContent.create(
                        "Подтвердить",
                        null,
                        "/acceptOrder 555777 12345"
                )))
        ));
    }

    @Test
    void provideDbsAcceptOrderButtonForCommonTemplate() throws IOException, JDOMException {
        var context = createContext(TELEGRAM_BOT, COMMON_ACCEPT_ORDER_TEMPLATE_ID, "dsbs_accept_order_data_commonTemplate.xml");

        assertThat(provider.provide(context), equalTo(
                new ArrayListNotificationData<>(List.of(TelegramButtonContent.create(
                        "Подтвердить",
                        null,
                        "/acceptOrder 555777 12345"
                )))
        ));
    }

    @Test
    void provideDbsAcceptOrderNoButtonIfNotTelegram() throws IOException, JDOMException {
        var context = createContext(EMAIL, DSBS_ACCEPT_ORDER_TEMPLATE_ID, "dsbs_accept_order_data.xml");

        assertThat(provider.provide(context), equalTo(
                new ArrayListNotificationData<>()
        ));
    }

    @Test
    void provideDbsAcceptOrderReminderButton() throws IOException, JDOMException {
        var context = createContext(
                TELEGRAM_BOT,
                DSBS_ACCEPT_ORDER_REMINDER_TEMPLATE_ID,
                "dsbs_accept_order_reminder_data.xml"
        );

        assertThat(provider.provide(context), equalTo(
                new ArrayListNotificationData<>(List.of(TelegramButtonContent.create(
                        "Подтвердить",
                        null,
                        "/acceptOrder 555777 111"
                )))
        ));
    }

    @Test
    void provideFbsAcceptOrderPendingButton() throws IOException, JDOMException {
        var context = createContext(
                TELEGRAM_BOT,
                COMMON_ACCEPT_ORDER_TEMPLATE_ID,
                "fbs_accept_order_reminder_data_pending.xml"
        );

        assertThat(provider.provide(context), equalTo(
                new ArrayListNotificationData<>(List.of(TelegramButtonContent.create(
                        "Подтвердить",
                        null,
                        "/acceptOrder 21620056 12345"
                )))
        ));
    }

    @Test
    void provideFbsAcceptOrderProcessingNoButton() throws IOException, JDOMException {
        var context = createContext(
                TELEGRAM_BOT,
                COMMON_ACCEPT_ORDER_TEMPLATE_ID,
                "fbs_accept_order_reminder_data_processing.xml"
        );

        assertThat(provider.provide(context), equalTo(
                new ArrayListNotificationData<>()
        ));
    }

    private NotificationContext createContext(NotificationTransport transport,
                                              long templateId,
                                              String xmlDataFile) throws IOException, JDOMException {
        var parser = new XmlToNotificationContextDataConverter();
        var data = parser.parse(readFile(xmlDataFile));
        
        return new NotificationContextImpl(
                new CodeNotificationType(templateId),
                NotificationPriority.NORMAL,
                transport,
                Collections.emptyList(),
                Instant.now(),
                new ArrayListNotificationData<>(data),
                null,
                false
        );
    }

    private String readFile(String path) throws IOException {
        return new String(
                new ClassPathResource("ru/yandex/market/partner/notification/service/providers/data/" + path)
                        .getInputStream()
                        .readAllBytes()
        );
    }
}
