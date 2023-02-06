package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.domain.system.MessageInfo;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.MESSAGE_INFO;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.SMS_TEXT;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.MessageMatchers.messagesMatcher;

@RunWith(MockitoJUnitRunner.class)
public class DbsSmsConsumerTest {

    @Mock
    private MessageSender messageSender;
    @Mock
    private LogTypesResolver logTypes;

    private DbsSmsConsumer consumer;

    private static final String PROXY_PHONE_NUMBER = "+78085553535";

    @Before
    public void setUp() {
        when(logTypes.getLogIdentifier("telephony.dbs_sms"))
                .thenReturn(new LogIdentifier("null/null", LBInstallation.LOGBROKER));
        consumer = new DbsSmsConsumer(messageSender, logTypes);
    }

    @Test
    public void testMessageCreating() {
        var jsonMessage = "{ eventId: \"21212\", timestamp: \"1470064678042\", " +
                          "from: \"+78085553536\", to: \"+78085553535\", " +
                          "text: \"Hello, world!\" }";

        var bpmMessage = new UidBpmMessage(
                MessageTypes.DBS_SMS,
                Uid.asPhone(PROXY_PHONE_NUMBER),
                Collections.singletonMap(ProcessVariablesNames.EVENT_ID, "21212"),
                Map.of(
                    ProcessVariablesNames.DBS_SMS_MESSAGE, new DbsSmsConsumer.DbsSmsMessage(
                        "21212",
                                1470064678042L,
                                "+78085553536",
                                "+78085553535",
                                "Hello, world!"
                    ),
                    MESSAGE_INFO, new MessageInfo(MessageTypes.DBS_SMS, 1470064678042L),
                    ProcessVariablesNames.EVENT_ID, "21212"
                )
        );

        assertMessages(jsonMessage, bpmMessage);
    }

    @Test
    public void testMessageNotCreatesWithoutPhone() {
        var jsonMessage = "{ eventId: \"21212\", timestamp: \"1470064678042\", " +
                          "from: \"+78085553535\", " +
                          "text: \"Hello, world!\" }";
        assertMessages(jsonMessage);
    }

    @Test
    public void testMessageNotCreatesWithoutSmsText() {
        var jsonMessage = "{ eventId: \"21212\", timestamp: \"1470064678042\", " +
                          "from: \"+78085553535\", to: \"+78085553535\" }";
        assertMessages(jsonMessage);
    }

    private void assertMessages(String line, UidBpmMessage... expected) {
        List<DbsSmsConsumer.DbsSmsMessage> rows = consumer.transform(line.getBytes());
        Preconditions.checkArgument(rows != null);
        consumer.accept(rows);

        verify(messageSender).send(argThat(messagesMatcher(expected)));
    }

}
