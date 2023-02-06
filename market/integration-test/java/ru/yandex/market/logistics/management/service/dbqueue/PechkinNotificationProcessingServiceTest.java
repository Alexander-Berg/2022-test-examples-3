package ru.yandex.market.logistics.management.service.dbqueue;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.queue.model.PechkinNotificationPayload;
import ru.yandex.market.logistics.management.queue.processor.PechkinNotificationProcessingService;

public class PechkinNotificationProcessingServiceTest extends AbstractContextualTest {

    @Autowired
    private PechkinNotificationProcessingService pechkinNotificationProcessingService;
    @Autowired
    private PechkinHttpClient pechkinHttpClient;

    @Test
    void testTaskProcessedAndMessageProperlySent() {
        MessageDto messageDto = new MessageDto();
        messageDto.setSender("LMS");
        messageDto.setChannel("Delivery_capacity");
        messageDto.setMessage("\uD83D\uDD25\uD83D\uDD25\uD83D\uDD25 - Сроки доставки\n" +
            "По этим партнерам капасити заполнено на 3 дня вперед (включая сегодняшнюю дату). Нужно " +
            "договориться с партнерами об увеличении капасити, забитое капасити негативно влияет на сроки" +
            " доставки.\n" +
            "\n" +
            "1.[DeliveryService1 (2)](https://lms.market.yandex-team.ru/lms/partner/2) (capacity: " +
            "[400](https://lms-admin.market.yandex-team.ru/lms/partner-capacity/5))\n");

        pechkinNotificationProcessingService.processPayload(new PechkinNotificationPayload("", messageDto));

        ArgumentCaptor<MessageDto> argumentCaptor = ArgumentCaptor.forClass(MessageDto.class);
        Mockito.verify(pechkinHttpClient, Mockito.times(1)).sendMessage(argumentCaptor.capture());

        MessageDto sentMessageDto = argumentCaptor.getValue();
        softly.assertThat(sentMessageDto)
            .as("MessageDto was passed")
            .isNotNull();

        softly.assertThat(sentMessageDto.getChannel())
            .as("Sent to the correct channel")
            .isEqualTo("Delivery_capacity");

        softly.assertThat(sentMessageDto.getMessage())
            .as("Sent correct message")
            .isEqualTo("\uD83D\uDD25\uD83D\uDD25\uD83D\uDD25 - Сроки доставки\n" +
                "По этим партнерам капасити заполнено на 3 дня вперед (включая сегодняшнюю дату). Нужно " +
                "договориться с партнерами об увеличении капасити, забитое капасити негативно влияет на сроки" +
                " доставки.\n" +
                "\n" +
                "1.[DeliveryService1 (2)](https://lms.market.yandex-team.ru/lms/partner/2) (capacity: " +
                "[400](https://lms-admin.market.yandex-team.ru/lms/partner-capacity/5))\n");
    }
}
