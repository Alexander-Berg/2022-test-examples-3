package ru.yandex.cs.billing.tms;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.SafeCall;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.consumer.sync.SyncConsumerConfig;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.pers.pay.model.PersPaymentBillingEventType;
import ru.yandex.market.pers.pay.model.dto.PersPaymentCsBillingEvent;

public class PersPaymentHoldGetExecutorExternalTest extends AbstractCsBillingTmsExternalFunctionalTest {

    private final LogbrokerCluster lbkxCluster;

    private final PersPaymentHoldGetExecutor persPaymentHoldGetExecutor;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PersPaymentHoldGetExecutorExternalTest(LogbrokerCluster lbkxCluster,
                                                  PersPaymentHoldGetExecutor persPaymentHoldGetExecutor,
                                                  Clock clock) {
        this.lbkxCluster = lbkxCluster;
        this.persPaymentHoldGetExecutor = persPaymentHoldGetExecutor;
        this.clock = clock;
        this.objectMapper = new ObjectMapper();
    }

    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/PersPaymentHoldGetExecutorExternalTest/testSaveOneNotExistentEventSuccessful/before.csv",
            after = "/ru/yandex/cs/billing/tms/PersPaymentHoldGetExecutorExternalTest/testSaveOneNotExistentEventSuccessful/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testSaveOneNotExistentEventSuccessful() throws JsonProcessingException, InterruptedException {
        PersPaymentCsBillingEvent event = new PersPaymentCsBillingEvent(
                "TEST-1",
                PersPaymentBillingEventType.HOLD,
                new BigDecimal("13.4"),
                132L,
                1,
                1616223600000L
        );

        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, 3, 20, 9, 0, 0)));

        SyncConsumer syncConsumer = Mockito.mock(SyncConsumer.class);

        Mockito.when(lbkxCluster.createSyncConsumer(Mockito.any(SyncConsumerConfig.class)))
                .thenReturn(syncConsumer);
        MessageBatch messageBatch = new MessageBatch("test", 0, convertTo(List.of(event)));
        Mockito.when(syncConsumer.read()).thenReturn(new ConsumerReadResponse(List.of(messageBatch), 0L));

        persPaymentHoldGetExecutor.doJob(null);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/PersPaymentHoldGetExecutorExternalTest/testSaveTwoNotExistentEventSuccessful/before.csv",
            after = "/ru/yandex/cs/billing/tms/PersPaymentHoldGetExecutorExternalTest/testSaveTwoNotExistentEventSuccessful/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testSaveTwoNotExistentEventSuccessful() throws InterruptedException {
        PersPaymentCsBillingEvent event1 = new PersPaymentCsBillingEvent(
                "TEST-1",
                PersPaymentBillingEventType.HOLD,
                new BigDecimal("13.4"),
                132L,
                1,
                1616223600000L
        );

        PersPaymentCsBillingEvent event2 = new PersPaymentCsBillingEvent(
                "TEST-2",
                PersPaymentBillingEventType.HOLD,
                new BigDecimal("8.00"),
                132L,
                1,
                1616223600000L
        );

        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, 3, 20, 9, 0, 0)));

        SyncConsumer syncConsumer = Mockito.mock(SyncConsumer.class);

        Mockito.when(lbkxCluster.createSyncConsumer(Mockito.any(SyncConsumerConfig.class)))
                .thenReturn(syncConsumer);

        MessageBatch messageBatch = new MessageBatch("test", 0, convertTo(List.of(event1, event2)));
        Mockito.when(syncConsumer.read()).thenReturn(new ConsumerReadResponse(List.of(messageBatch), 0L));

        persPaymentHoldGetExecutor.doJob(null);
    }

    @DisplayName("Дублирующиеся, перемешанные, невалидные события успешно обработаны")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/PersPaymentHoldGetExecutorExternalTest/testSaveMixedEventSuccessful/before.csv",
            after = "/ru/yandex/cs/billing/tms/PersPaymentHoldGetExecutorExternalTest/testSaveMixedEventSuccessful/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testSaveMixedEventSuccessful() throws InterruptedException {
        PersPaymentCsBillingEvent event1 = new PersPaymentCsBillingEvent(
                "TEST-1",
                PersPaymentBillingEventType.HOLD,
                new BigDecimal("13.4"),
                132L,
                1,
                1616223600000L
        );

        PersPaymentCsBillingEvent event2 = new PersPaymentCsBillingEvent(
                "TEST-2",
                PersPaymentBillingEventType.HOLD,
                new BigDecimal("8.00"),
                132L,
                2,
                1616223600000L
        );

        PersPaymentCsBillingEvent event21 = new PersPaymentCsBillingEvent(
                "TEST-2",
                PersPaymentBillingEventType.HOLD,
                new BigDecimal("8.00"),
                132L,
                2,
                1616223600000L
        );

        PersPaymentCsBillingEvent invalidEvent = new PersPaymentCsBillingEvent(
                null,
                PersPaymentBillingEventType.HOLD,
                new BigDecimal("8.00"),
                132L,
                2,
                1616223600000L
        );

        PersPaymentCsBillingEvent event3 = new PersPaymentCsBillingEvent(
                "TEST-1",
                PersPaymentBillingEventType.CHARGE,
                new BigDecimal("13.4"),
                132L,
                1,
                1616223601000L
        );

        PersPaymentCsBillingEvent event4 = new PersPaymentCsBillingEvent(
                "TEST-2",
                PersPaymentBillingEventType.CANCEL,
                new BigDecimal("8.00"),
                132L,
                2,
                1616223602000L
        );

        PersPaymentCsBillingEvent event5 = new PersPaymentCsBillingEvent(
                "TEST-1",
                PersPaymentBillingEventType.REFUND,
                new BigDecimal("13.4"),
                132L,
                1,
                1616223605000L
        );

        PersPaymentCsBillingEvent event6 = new PersPaymentCsBillingEvent(
                "TEST-3",
                PersPaymentBillingEventType.REFUND,
                new BigDecimal("11.4"),
                132L,
                1,
                1616223607000L
        );

        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, 3, 20, 9, 0, 0)));

        SyncConsumer syncConsumer = Mockito.mock(SyncConsumer.class);

        Mockito.when(lbkxCluster.createSyncConsumer(Mockito.any(SyncConsumerConfig.class)))
                .thenReturn(syncConsumer);

        MessageBatch messageBatch = new MessageBatch("test", 0, convertTo(List.of(event1, event2, event3, event21, invalidEvent, event4, event5, event6, event6)));
        Mockito.when(syncConsumer.read()).thenReturn(new ConsumerReadResponse(List.of(messageBatch), 0L));

        persPaymentHoldGetExecutor.doJob(null);
    }

    private List<MessageData> convertTo(List<PersPaymentCsBillingEvent> events) {
        MessageMeta messageMeta = new MessageMeta(null, 0L, 0L, 0L, null, CompressionCodec.RAW, Map.of());
        return events.stream()
                .map(event -> SafeCall.callSafely(() -> objectMapper.writeValueAsBytes(event)))
                .map(data -> new MessageData(data, 0L, messageMeta))
                .collect(Collectors.toUnmodifiableList());
    }
}
