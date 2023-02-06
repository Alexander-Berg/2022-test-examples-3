package ru.yandex.market.pers.notify.push;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PushSimpleConsumerIsolatedTest {
    private static final String MESSAGE_WITH_TITLE =
            "Заказ принят!\n" +
            "\n" +
            "Мы уже начали его собирать.";

    private static final String MESSAGE_WITHOUT_TITLE =
            "Заказ принят!\n" +
            "Мы уже начали его собирать.";
    @Test
    void testMessageWithTitle() throws Exception {
        PushSimpleConsumer.PushMessage message = PushSimpleConsumer.extractTitleAndBody(MESSAGE_WITH_TITLE);
        assertEquals("Заказ принят!", message.getTitle());
        assertEquals("Мы уже начали его собирать.", message.getBody());
    }

    @Test
    void testMessageWithoutTitle() throws Exception {
        PushSimpleConsumer.PushMessage message = PushSimpleConsumer.extractTitleAndBody(MESSAGE_WITHOUT_TITLE);
        assertNull(message.getTitle());
        assertEquals(MESSAGE_WITHOUT_TITLE, message.getBody());
    }

}
