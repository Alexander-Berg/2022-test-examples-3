package ru.yandex.market.delivery.transport_manager.service.notifications;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;

public class TelegramNotificationServiceTest extends AbstractContextualTest {
    @Autowired
    private TelegramNotificationService telegramNotificationService;

    @Autowired
    private PechkinHttpClient pechkinHttpClient;

    @Test
    void testSending() {
        ArgumentCaptor<MessageDto> captor = ArgumentCaptor.forClass(MessageDto.class);
        telegramNotificationService.send(TelegramChannel.INTERWAREHOUSE_CHANNEL, "message");

        Mockito.verify(pechkinHttpClient).sendMessage(captor.capture());

        MessageDto dto = captor.getValue();
        MessageDto expected = new MessageDto();

        expected.setChannel("Interwarehouse_transportations");
        expected.setSender("TM");
        expected.setMessage("message");

        assertThatModelEquals(expected, dto);

    }
}
