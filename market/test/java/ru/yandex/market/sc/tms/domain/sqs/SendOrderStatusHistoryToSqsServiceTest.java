package ru.yandex.market.sc.tms.domain.sqs;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.OrderNewCheckpointEvent;
import ru.yandex.market.logistics.les.ScOrderEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.order.jdbc.OrderFFStatusJdbcRepository;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderFFStatusHistoryItemRepository;
import ru.yandex.market.sc.core.domain.sqs.SendOrderStatusHistoryToSqsService;
import ru.yandex.market.sc.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@EmbeddedDbTmsTest
public class SendOrderStatusHistoryToSqsServiceTest {

    private static final String QUEUE_NAME = "sc_out";
    private static final String TRACKING_QUEUE_NAME = "sc_tracking_out";
    private static final String SOURCE = "sc";

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ScOrderFFStatusHistoryItemRepository repository;

    @Autowired
    TestFactory testFactory;

    @MockBean
    JmsTemplate jmsTemplate;

    @SpyBean
    OrderFFStatusJdbcRepository orderFFStatusJdbcRepository;

    @Autowired
    SqsQueueProperties sqsQueueProperties;

    @Autowired
    Clock clock;

    SendOrderStatusHistoryToSqsService service;

    @BeforeEach
    void setUp() {
        configurationService.mergeValue(ConfigurationProperties.SEND_EVENTS_TO_SQS_BY_LES_NUM_ENABLED, true);
        configurationService.mergeValue(ConfigurationProperties.ORDER_FF_STATUS_HISTORY_BATCH_SIZE, 2);
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_ORDER_FF_STATUS_HISTORY_LES_NUM, 0);

        Mockito.when(sqsQueueProperties.getOutQueue()).thenReturn(QUEUE_NAME);
        Mockito.when(sqsQueueProperties.getTrackingOutQueue()).thenReturn(TRACKING_QUEUE_NAME);
        Mockito.when(sqsQueueProperties.getSource()).thenReturn(SOURCE);

        service = new SendOrderStatusHistoryToSqsService(
                orderFFStatusJdbcRepository,
                configurationService,
                configurationService,
                jmsTemplate,
                sqsQueueProperties,
                clock
        );

        testFactory.createOrderForToday(testFactory.storedSortingCenter()).accept().sort().ship().get();
        // called from OrderShipService during order shipment
        Mockito.verify(orderFFStatusJdbcRepository).createOrderFFStatusHistoryItems(any(), any());
        service.markHistoryItemForSending();
    }

    @Test
    void sendOnlyNewItemsInBatches() {
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_ORDER_FF_STATUS_HISTORY_LES_NUM, 1);
        service.sendOrderFfStatusHistoryToSqs();

        ArgumentCaptor<Event> argumentForB2B = ArgumentCaptor.forClass(Event.class);
        ArgumentCaptor<Event> argumentForDT = ArgumentCaptor.forClass(Event.class);

        Mockito.verify(jmsTemplate, Mockito.times(3))
                .convertAndSend(Mockito.eq(QUEUE_NAME), argumentForB2B.capture());

        Mockito.verify(jmsTemplate, Mockito.times(3))
                .convertAndSend(Mockito.eq(TRACKING_QUEUE_NAME), argumentForDT.capture());

        argumentForB2B.getAllValues().forEach(event -> {
                    assertThat(event.getSource()).isEqualTo(SOURCE);
                    assertThat(event.getEventId()).isBetween("1", "4");
                    assertThat(event.getDescription()).startsWith("Заказ перешел в статус");
                    assertThat(event.getPayload()).isInstanceOf(ScOrderEvent.class);
                }
        );

        argumentForDT.getAllValues().forEach(event -> {
                    assertThat(event.getSource()).isEqualTo(SOURCE);
                    assertThat(event.getEventId()).isBetween("1", "4");
                    assertThat(event.getDescription()).startsWith("");
                    assertThat(event.getPayload()).isInstanceOf(OrderNewCheckpointEvent.class);
                }
        );
    }

    @Test
    void doNotSendIfFlagIsOff() {
        configurationService.mergeValue(ConfigurationProperties.SEND_EVENTS_TO_SQS_BY_LES_NUM_ENABLED, false);
        service.sendOrderFfStatusHistoryToSqs();

        Mockito.verifyNoMoreInteractions(jmsTemplate);
    }

    @Test
    void sendAllInOneBatch_ForB2b() {
        configurationService.mergeValue(ConfigurationProperties.ORDER_FF_STATUS_HISTORY_BATCH_SIZE, 5);
        service.sendOrderFfStatusHistoryToSqs();

        ArgumentCaptor<Event> argumentForB2B = ArgumentCaptor.forClass(Event.class);

        ArgumentCaptor<Event> argumentForDT = ArgumentCaptor.forClass(Event.class);

        Mockito.verify(jmsTemplate, Mockito.times(4))
                .convertAndSend(Mockito.eq(QUEUE_NAME), argumentForB2B.capture());
        Mockito.verify(jmsTemplate, Mockito.times(4))
                .convertAndSend(Mockito.eq(TRACKING_QUEUE_NAME), argumentForDT.capture());

        argumentForB2B
            .getAllValues()
            .forEach(event -> {
                assertThat(event.getSource()).isEqualTo(SOURCE);
                assertThat(event.getEventId()).isBetween("1", "4");
                assertThat(event.getDescription()).startsWith("Заказ перешел в статус");
                assertThat(event.getPayload()).isInstanceOf(ScOrderEvent.class);
        });

        argumentForDT
            .getAllValues()
            .forEach(event -> {
                assertThat(event.getSource()).isEqualTo(SOURCE);
                assertThat(event.getEventId()).isBetween("1", "4");
                assertThat(event.getDescription()).startsWith("");
                assertThat(event.getPayload()).isInstanceOf(OrderNewCheckpointEvent.class);
            }
        );
    }

    @Test
    void doNotSendIfNothingNewAppeared() {
        var maxLesNum = findMaxLesNum();
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_ORDER_FF_STATUS_HISTORY_LES_NUM, maxLesNum);
        service.sendOrderFfStatusHistoryToSqs();

        Mockito.verifyNoMoreInteractions(jmsTemplate);
    }

    private long findMaxLesNum() {
        return repository.findAll()
            .stream()
            .mapToLong(i -> Optional.ofNullable(i.getLesNum()).orElse(0L))
            .max()
            .orElse(0);
    }
}
