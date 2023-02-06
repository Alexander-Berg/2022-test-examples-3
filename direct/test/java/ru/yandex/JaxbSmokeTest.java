package ru.yandex;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;

import ru.yandex.bannerstorage.messaging.services.impl.ServiceBrokerPoisonMessage;
import ru.yandex.bannerstorage.messaging.services.impl.ServiceBrokerQueueMessage;
import ru.yandex.bannerstorage.messaging.utils.MessageSerializer;

public class JaxbSmokeTest {
    @Test
    public void jaxbSmokeTest() {
        // Проверяем, что маршаллинг через JAXB работает без исключений
        MessageSerializer.marshal(ServiceBrokerPoisonMessage.fromQueueMessage(
                new ServiceBrokerQueueMessage(
                        "22222",
                        "44444",
                        "ffffff",
                        "ffffff",
                        "ffffff",
                        new Timestamp(new Date().getTime()),
                        "fffff",
                        0
                ),
                true
        ));
    }
}
