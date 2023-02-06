package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.triggers.services.bpm.BpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.bpm.messages.OrderEventBpmMessageFactory;
import ru.yandex.market.mcrm.utils.PropertiesProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author vtarasoff
 * @since 08.10.2020
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        OrderEventConsumerTest.TestConfiguration.class,
        OrderEventConsumer.class,
        LogTypesResolver.class
})
@TestPropertySource(properties = {
        "logBroker.logIdentifier.checkouter.orderEvent=test--test",
        "logBroker.installation.checkouter.orderEvent=lbkx"
})
public class OrderEventConsumerTest {
    @Configuration
    @ImportResource("classpath:/WEB-INF/checkouter-client.xml")
    static class TestConfiguration {

        @Bean
        MessageSender messageSender() {
            MessageSender sender = mock(MessageSender.class);

            doAnswer(invocation -> {
                List<UidBpmMessage> messages = invocation.getArgument(0, List.class);

                messages.stream()
                        .map(message -> HistoryEventType.valueOf(message.getType()))
                        .forEach(SENT_MESSAGES::add);

                return null;
            }).when(sender).send(anyList());

            return sender;
        }

        @Bean
        OrderEventBpmMessageFactory orderEventBpmMessageFactory() {
            OrderEventBpmMessageFactory mock = mock(OrderEventBpmMessageFactory.class);
            when(mock.from(any(OrderHistoryEvent.class)))
                    .thenAnswer(invocation -> {
                        OrderHistoryEvent event = invocation.getArgument(0, OrderHistoryEvent.class);
                        BpmMessage message = mock(UidBpmMessage.class);
                        when(message.getType()).thenReturn(event.getType().name());
                        return List.of(message);
                    });
            return mock;
        }

        @Bean
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setIgnoreUnresolvablePlaceholders(true);
            return configurer;
        }

        @Bean
        PropertiesProvider propertiesProvider(ConfigurableBeanFactory beanFactory) {
            return new PropertiesProvider(beanFactory);
        }
    }

    private static final Set<HistoryEventType> SENT_MESSAGES = new HashSet<>();

    @Inject
    private OrderEventConsumer consumer;

    @Before
    public void setUp() {
        SENT_MESSAGES.clear();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowsExceptionOnNullMessage() {
        consumer.transform(null);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowsExceptionOnEmptyMessage() {
        consumer.transform(new byte[0]);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowsExceptionOnNonJsonMessage() {
        consumer.transform("Hello World!".getBytes());
    }

    @Test
    public void shouldIgnoreNonExistentJsonProperties() {
        List<OrderHistoryEvent> events = consumer.transform("{\"helloworld\": \"Hello World!\"}".getBytes());
        assertThat(events, hasSize(1));
    }

    @Test
    public void shouldFilterByType() {
        Set<HistoryEventType> acceptedTypes = Set.of(
                HistoryEventType.NEW_ORDER,
                HistoryEventType.ORDER_DELIVERY_UPDATED,
                HistoryEventType.ORDER_STATUS_UPDATED,
                HistoryEventType.ORDER_SUBSTATUS_UPDATED,
                HistoryEventType.ORDER_CHANGE_REQUEST_CREATED,
                HistoryEventType.ORDER_CHANGE_REQUEST_STATUS_UPDATED,
                HistoryEventType.PARCEL_DELIVERY_DEADLINE_STATUS_UPDATED,
                HistoryEventType.ORDER_RETURN_CREATED,
                HistoryEventType.ORDER_RETURN_DELIVERY_UPDATED,
                HistoryEventType.ORDER_RETURN_DELIVERY_STATUS_UPDATED,
                HistoryEventType.ORDER_RETURN_DELIVERY_RESCHEDULED,
                HistoryEventType.TRACK_CHECKPOINT_CHANGED,
                HistoryEventType.ORDER_CANCELLATION_REQUESTED,
                HistoryEventType.ITEMS_UPDATED,
                HistoryEventType.ITEM_SERVICE_STATUS_UPDATED,
                HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED,
                HistoryEventType.RECEIPT_PRINTED,
                HistoryEventType.ORDER_RETURN_STATUS_UPDATED,
                HistoryEventType.CASH_REFUND_RECEIPT_PRINTED,
                HistoryEventType.ORDER_CASHBACK_EMISSION_CLEARED,
                HistoryEventType.PAYMENT_INVOICE_GENERATED
        );

        Stream.of(HistoryEventType.values())
                .map(type -> {
                    OrderHistoryEvent event = new OrderHistoryEvent();
                    event.setType(type);
                    return event;
                })
                .map(List::of)
                .forEach(consumer::accept);


        assertThat(SENT_MESSAGES, equalTo(acceptedTypes));
    }
}
