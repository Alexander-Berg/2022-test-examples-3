package ru.yandex.cs.billing.tms;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.AbstractCsBillingTmsFunctionalTest;
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

public class PersPaymentHoldGetExecutorFunctionalTest extends AbstractCsBillingTmsFunctionalTest {

    private final LogbrokerCluster lbkxCluster;

    private final PersPaymentHoldGetExecutor persPaymentHoldGetExecutor;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PersPaymentHoldGetExecutorFunctionalTest(LogbrokerCluster lbkxCluster,
                                                    PersPaymentHoldGetExecutor persPaymentHoldGetExecutor,
                                                    Clock clock) {
        this.lbkxCluster = lbkxCluster;
        this.persPaymentHoldGetExecutor = persPaymentHoldGetExecutor;
        this.clock = clock;
        this.objectMapper = new ObjectMapper();
    }

    @DbUnitDataSet(
            after = "/ru/yandex/cs/billing/tms/PersPaymentHoldGetExecutorFunctionalTest/testNoDatasourceIsFoundFailed/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testNoDatasourceIsFoundFailed() throws JsonProcessingException, InterruptedException {
        PersPaymentCsBillingEvent persPaymentCsBillingEvent = new PersPaymentCsBillingEvent(
                "TEST-1",
                PersPaymentBillingEventType.HOLD,
                new BigDecimal(10L),
                1L,
                50,
                1000
        );

        byte[] data = objectMapper.writeValueAsBytes(persPaymentCsBillingEvent);

        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 3, 20)));

        SyncConsumer syncConsumer = Mockito.mock(SyncConsumer.class);

        Mockito.when(lbkxCluster.createSyncConsumer(Mockito.any(SyncConsumerConfig.class)))
                .thenReturn(syncConsumer);
        MessageMeta messageMeta = new MessageMeta(null, 0L, 0L, 0L, null, CompressionCodec.RAW, Map.of());
        MessageData messageData = new MessageData(data, 0L, messageMeta);
        MessageBatch messageBatch = new MessageBatch("test", 0, List.of(messageData));
        Mockito.when(syncConsumer.read()).thenReturn(new ConsumerReadResponse(List.of(messageBatch), 0L));

        persPaymentHoldGetExecutor.doJob(null);
    }
}
