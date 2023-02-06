package ru.yandex.market.pers.notify.ems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.pers.notify.ems.configuration.NotificationConfig;
import ru.yandex.market.pers.notify.ems.configuration.NotificationConsumerConfig;
import ru.yandex.market.pers.notify.ems.configuration.NotificationEventConfig;
import ru.yandex.market.pers.notify.ems.configuration.NotificationProducerConfig;
import ru.yandex.market.pers.notify.ems.consumer.MailEventConsumer;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.event.NotificationEventPayload;
import ru.yandex.market.pers.notify.ems.event.NotificationEventProcessingResult;
import ru.yandex.market.pers.notify.ems.filter.config.NotificationSpamFilterConfig;
import ru.yandex.market.pers.notify.ems.producer.NotificationEventProducerFactory;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.ems.template.SimpleTemplateResolver;
import ru.yandex.market.pers.notify.model.MailTemplate;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.SenderTemplate;
import ru.yandex.market.pers.notify.model.event.EventAddressType;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         02.09.15
 */
public class NotificationProcessorMockedTest {
    private static final Random RND = new Random();

    @Test
    public void testGetProcessingResults() {
        class ConsumerCart extends MailEventConsumer<Void> {
            @Override
            protected Map<String, Object> getModel(NotificationEventPayload<Void> event, MailTemplate template) {
                return new HashMap<>();
            }

            @Override
            protected NotificationEventStatus sendMessage(NotificationEventPayload<Void> event,
                                                          Map<String, Object> model,
                                                          MailTemplate template,
                                                          List<MailAttachment> attachments) {
                sleep(RND.nextInt(20) + 10);
                assertTrue(event.getNotificationSubtype().equals(NotificationSubtype.CART_1));
                return NotificationEventStatus.IOERROR;
            }
        }

        class ConsumerConfirm extends MailEventConsumer<Void> {
            @Override
            protected Map<String, Object> getModel(NotificationEventPayload<Void> event, MailTemplate template) {
                return new HashMap<>();
            }

            @Override
            protected NotificationEventStatus sendMessage(NotificationEventPayload<Void> event,
                                                          Map<String, Object> model,
                                                          MailTemplate template,
                                                          List<MailAttachment> attachments) {
                sleep(RND.nextInt(20) + 10);
                assertEquals(NotificationSubtype.CONFIRM_SUBSCRIPTION, event.getNotificationSubtype());
                return NotificationEventStatus.SENT;
            }
        }

        ApplicationContext context = mock(ApplicationContext.class);

        when(context.getBean(eq("ConsumerCart"), eq(ConsumerCart.class))).thenReturn(new ConsumerCart());
        when(context.getBean(eq("ConsumerConfirm"), eq(ConsumerConfirm.class))).thenReturn(new ConsumerConfirm());

        List<NotificationEvent> eventsConfirm = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            eventsConfirm.add(new NotificationEvent(i, (long) RND.nextInt(100_000),
                null, null, "mymail@mymail.ru", EventAddressType.MAIL, NotificationSubtype.CONFIRM_SUBSCRIPTION,
                NotificationEventStatus.NEW, new HashMap<>(), new Date(), new Date(), new Date(), false));
        }
        List<NotificationEvent> eventsCart = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            eventsCart.add(new NotificationEvent(i + 1000, (long) RND.nextInt(100_000),
                null, null, "mymail@mymail.ru", EventAddressType.MAIL, NotificationSubtype.CART_1,
                NotificationEventStatus.NEW, new HashMap<>(), new Date(), new Date(), new Date(), false));
        }

        NotificationConfig config = new NotificationConfig() {
            @Override
            public void init() {
                NOTIFICATION_CONFIG.put(NotificationSubtype.CONFIRM_SUBSCRIPTION,
                    new NotificationEventConfig(
                        new NotificationProducerConfig(1000),
                        new NotificationConsumerConfig("ConsumerConfirm", ConsumerConfirm.class),
                        null,
                        Collections.emptyList(),
                        SimpleTemplateResolver.sender(SenderTemplate.CONFIRM_SUBSCRIPTION)
                    ));
                NOTIFICATION_CONFIG.put(NotificationSubtype.CART_1,
                    new NotificationEventConfig(
                        new NotificationProducerConfig(1000),
                        new NotificationConsumerConfig("ConsumerCart", ConsumerCart.class),
                        (NotificationSpamFilterConfig) null,
                        SimpleTemplateResolver.sender(SenderTemplate.ABANDONED_1)
                    ));
            }
        };
        config.init();

        NotificationProcessor notificationProcessor = new NotificationProcessor(
            null,
            null,
            config,
            new NotificationEventProducerFactory(mock(MailerNotificationEventService.class)) {
                @Override
                public NotificationEventProducer createProducer(int limit) {
                    return new NotificationEventProducer() {
                        @Override
                        public List<NotificationEvent> apply(NotificationSubtype subtype) {
                            sleep(RND.nextInt(1000) + 100);
                            switch (subtype) {
                                case CART_1:
                                    return eventsCart;
                                case CONFIRM_SUBSCRIPTION:
                                    return eventsConfirm;
                                default:
                                    throw new RuntimeException("Could not produce events of type " + subtype);
                            }
                        }

                        @Override
                        public int limit() {
                            return limit;
                        }
                    };
                }
            },
            context,
            null
        );
        notificationProcessor.setNotificationEventFilters(Collections.emptyList());

        List<NotificationEventProcessingResult> results = notificationProcessor.getProcessingResults();
        assertEquals(1200, results.size());
        for (NotificationEventProcessingResult result : results) {
            if (result.getEvent().getNotificationSubtype().equals(NotificationSubtype.CART_1)) {
                assertEquals(NotificationEventStatus.IOERROR, result.getStatus());
            } else {
                assertEquals(NotificationEventStatus.SENT, result.getStatus());
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {
        }
    }
}
