package ru.yandex.market.logistics.pechkin.app.notificator;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.pechkin.app.component.ChannelPropertyReader;
import ru.yandex.market.logistics.pechkin.app.telegram.TelegramClient;
import ru.yandex.market.logistics.pechkin.app.telegram.processors.TestDebugProcessor;

class TelegramNotificatorTest {

    @Mock
    ChannelPropertyReader channelPropertyReader;

    @Mock
    TelegramClient telegramClient;

    @Test
    void sendNotification() {
        Mockito.when(channelPropertyReader.getChannelMap()).thenReturn(Map.of());
        var tele = new TelegramNotificator(channelPropertyReader, telegramClient, List.of());
        MessageDto mes =
            new MessageDto();
        mes.setMessage("test");
        mes.setChannel("channel");
        mes.setSender("sender");
        tele.sendNotification(mes);
        Assertions.assertEquals("test", mes.getMessage());
    }

    @Test
    void sendNotificationTest() {
        Mockito.when(channelPropertyReader.getChannelMap()).thenReturn(Map.of());
        var proc = new TestDebugProcessor();
        var tele = new TelegramNotificator(channelPropertyReader, telegramClient, List.of(
            proc
        ));
        var test = "test";

        MessageDto mes = new MessageDto();
        mes.setMessage(test);
        mes.setChannel("channel");
        mes.setSender("sender");

        MessageDto mes2 = new MessageDto();
        mes.setMessage(test);
        mes.setChannel("channel");
        mes.setSender("sender");
        proc.process(mes2);

        tele.sendNotification(mes);
        Assertions.assertNotEquals(test, mes.getMessage());
        Assertions.assertEquals(mes2.getMessage(), mes.getMessage());
    }
}
