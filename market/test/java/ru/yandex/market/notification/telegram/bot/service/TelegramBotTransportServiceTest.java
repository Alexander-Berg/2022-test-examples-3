package ru.yandex.market.notification.telegram.bot.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.notification.common.model.TelegramNotificationContent;
import ru.yandex.market.notification.model.context.NotificationTransportContext;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.simple.model.context.NotificationTransportContextImpl;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;
import ru.yandex.market.notification.telegram.bot.model.address.TelegramIdAddress;
import ru.yandex.market.notification.telegram.bot.model.dto.InlineKeyboardButton;
import ru.yandex.market.notification.telegram.bot.model.dto.ParseMode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TelegramBotTransportServiceTest {

    private final static String NOTIFICATION_TEXT = "blabla";
    private final static String BOT_ID = "YesIAmRobot";
    private final static long TELEGRAM_ID_1 = 100500L;
    private final static long TELEGRAM_ID_2 = 9000L;

    @Test
    public void testSendMessageToEachUid() {
        test((service, restClient) -> {
            TelegramIdAddress address1 = TelegramIdAddress.create(BOT_ID, TELEGRAM_ID_1);
            TelegramIdAddress address2 = TelegramIdAddress.create(BOT_ID, TELEGRAM_ID_2);

            TelegramNotificationContent content = TelegramNotificationContent.create(NOTIFICATION_TEXT);
            NotificationTransportContext context = createContext(content, Arrays.asList(address1, address2));
            service.send(context);

            ArgumentCaptor<Collection<TelegramIdAddress>> addressesCaptor = ArgumentCaptor.forClass(Collection.class);
            ArgumentCaptor<String> notificationTextCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Collection<InlineKeyboardButton>> keyboardCollectionCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            ArgumentCaptor<ParseMode> parseModeCaptor =
                    ArgumentCaptor.forClass(ParseMode.class);

            verify(restClient).sendMessage(
                    addressesCaptor.capture(),
                    notificationTextCaptor.capture(),
                    keyboardCollectionCaptor.capture(),
                    parseModeCaptor.capture()
            );

            verifyNoMoreInteractions(restClient);

            InlineKeyboardButton keyboardButton = new InlineKeyboardButton(
                    "Не получать такие уведомления",
                    null,
                    "/unsubscribe 1");
            assertThat(addressesCaptor.getValue(), containsInAnyOrder(address1, address2));
            assertThat(notificationTextCaptor.getValue(), equalTo(NOTIFICATION_TEXT));
            assertThat(keyboardCollectionCaptor.getValue(), containsInAnyOrder(keyboardButton));
        });
    }

    /**
     * Создать все моки и сервис и провести точечный тест.
     */
    private void test(final TelegramBotTransportTester tester) {
        final PartnerBotRestClient restClient = mockPartnerBotRestClient();

        final TelegramBotTransportService service =
                new TelegramBotTransportService(restClient);

        tester.test(service, restClient);
    }

    private PartnerBotRestClient mockPartnerBotRestClient() {
        final PartnerBotRestClient restClient = mock(PartnerBotRestClient.class);
        when(restClient.sendMessage(anyCollection(), anyString())).thenAnswer(invocation -> Collections.emptyMap());
        return restClient;
    }

    private NotificationTransportContext createContext(final TelegramNotificationContent content,
                                                       final Collection<NotificationAddress> addresses) {
        return new NotificationTransportContextImpl(content, addresses,
                new PersistentNotification(1L, 2L, new CodeNotificationType(1L), null, null, null, null, null, false),
                Collections.emptyList());
    }

    private interface TelegramBotTransportTester {
        void test(TelegramBotTransportService service, PartnerBotRestClient restClient);
    }
}
