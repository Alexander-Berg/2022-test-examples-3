package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.entity.notification.NotificationEntity;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.MessageMatchers.messagesMatcher;

@RunWith(MockitoJUnitRunner.class)
public class AntifraudNotificationConsumerTest {

    @Mock
    private MessageSender messageSender;
    @Mock
    private LogTypesResolver logTypesResolver;

    AntifraudNotificationConsumer consumer;

    @Before
    public void setUp() {
        when(logTypesResolver.getLogIdentifier("antifraud.notifications"))
                .thenReturn(new LogIdentifier("null/null", LBInstallation.LOGBROKER));
        consumer = new AntifraudNotificationConsumer(logTypesResolver, new ObjectMapper(), messageSender);
    }

    @Test
    public void accept() {
        var jsonMessage = "{ \"type\": \"UID\", \"value\": \"1\", " +
                "\"blockingType\": \"LOYALTY\", \"actionType\": \"BLOCK\" }";

        var bpmMessage = new UidBpmMessage(
                MessageTypes.ANTIFRAUD_LOYALTY_BLOCK,
                Uid.asPuid("1"),
                Collections.emptyMap(),
                Collections.emptyMap());

        List<NotificationEntity> notifications = consumer.transform(jsonMessage.getBytes());
        Preconditions.checkArgument(notifications != null);
        consumer.accept(notifications);

        verify(messageSender).send(argThat(messagesMatcher(bpmMessage)));
    }
}
