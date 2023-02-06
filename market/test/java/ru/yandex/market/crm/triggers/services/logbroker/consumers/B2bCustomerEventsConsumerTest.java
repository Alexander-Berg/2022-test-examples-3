package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.b2b.office.customerapi.B2bCustomerEdoEvent;
import ru.yandex.market.b2b.office.customerapi.B2bCustomerNotifyBaseEvent;
import ru.yandex.market.crm.core.jackson.CustomObjectMapperFactory;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.B2B_CUSTOMER_EDO_PROVIDER;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.B2B_CUSTOMER_EDO_PROVIDER_LINK;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.B2B_CUSTOMER_EMAIL;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.B2B_CUSTOMER_INN;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.B2B_CUSTOMER_KPP;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.B2B_CUSTOMER_MAILING_ADDRESS;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.B2B_CUSTOMER_NAME;
import static ru.yandex.market.crm.core.services.trigger.MessageTypes.B2B_CUSTOMER_EDO_CONNECTED;
import static ru.yandex.market.crm.core.services.trigger.MessageTypes.B2B_CUSTOMER_EDO_INVITED;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.MessageMatchers.messagesMatcher;

@ExtendWith(MockitoExtension.class)
public class B2bCustomerEventsConsumerTest {
    private B2bCustomerEventsConsumer consumer;
    private ObjectMapper objectMapper;
    @Mock
    private MessageSender messageSender;
    @Mock
    private LogTypesResolver logTypes;

    @BeforeEach
    void setUp() {
        when(logTypes.getLogIdentifier("b2bcustomer.events"))
                .thenReturn(new LogIdentifier("null/null", LBInstallation.LOGBROKER));

        objectMapper = CustomObjectMapperFactory.INSTANCE.getJsonObjectMapper();
        consumer = new B2bCustomerEventsConsumer(logTypes, objectMapper, messageSender);
    }

    public static Stream<Arguments> edoEventsSource() {
        return Stream.of(
                Arguments.of("b2b_customer__edo_invite.json", B2B_CUSTOMER_EDO_INVITED),
                Arguments.of("b2b_customer__edo_connected.json", B2B_CUSTOMER_EDO_CONNECTED)
        );
    }

    @ParameterizedTest
    @MethodSource("edoEventsSource")
    public void testNewEdoEvents(String eventJsonFile, String expectedMessageType) throws IOException {
        InputStream eventStream = getClass().getResourceAsStream(eventJsonFile);
        byte[] eventBytes = IOUtils.toByteArray(Objects.requireNonNull(eventStream));
        Assertions.assertNotNull(eventBytes);

        transformAndAccept(eventBytes);

        B2bCustomerEdoEvent event = objectMapper.readValue(eventBytes, B2bCustomerEdoEvent.class);
        Assertions.assertNotNull(event);
        UidBpmMessage expectedMessage = new UidBpmMessage(
                expectedMessageType,
                Uid.asEmail(event.getTargetEmail()),
                Map.of(
                        B2B_CUSTOMER_INN, event.getInn(),
                        B2B_CUSTOMER_KPP, event.getKpp()
                ),
                Map.of(
                        B2B_CUSTOMER_EMAIL, event.getTargetEmail(),
                        B2B_CUSTOMER_NAME, event.getCompanyName(),
                        B2B_CUSTOMER_INN, event.getInn(),
                        B2B_CUSTOMER_KPP, event.getKpp(),
                        B2B_CUSTOMER_MAILING_ADDRESS, event.getMailingAddress(),
                        B2B_CUSTOMER_EDO_PROVIDER, event.getEdoProvider(),
                        B2B_CUSTOMER_EDO_PROVIDER_LINK, event.getEdoProviderLink()
                )
        );

        verify(messageSender).send(argThat(messagesMatcher(expectedMessage)));
    }

    @Test
    void testUnknownSubtypesOfB2bCustomerNotifyBaseEvent() {
        JsonSubTypes jsonSubTypes = B2bCustomerNotifyBaseEvent.class.getAnnotation(JsonSubTypes.class);
        Assertions.assertEquals(jsonSubTypes.value().length, edoEventsSource().count(),
                "Количество подтипов B2bCustomerNotifyBaseEvent отличается от количества проверяемых подтипов. " +
                        "Нужно реализовать обработку новых типов в B2bCustomerEventsConsumer? " +
                        "Нужно добавить обработку новых типов в B2bCustomerEventsConsumerTest?");
    }

    private void transformAndAccept(byte[] eventBytes) {
        List<B2bCustomerNotifyBaseEvent> rows = consumer.transform(eventBytes);
        Assertions.assertNotNull(rows);
        consumer.accept(rows);
    }
}
