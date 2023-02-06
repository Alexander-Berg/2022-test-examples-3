package ru.yandex.market.tpl.core.domain.routing.async.publish;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.common.db.queue.base.QueueProcessingService;
import ru.yandex.market.tpl.common.util.exception.TplRoutingException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultStatus;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED;

@ExtendWith(MockitoExtension.class)
class PublishRoutingResultConsumerTest {

    public static final String PROCESSING_ID = UUID.randomUUID().toString();
    public static final String ROUTING_REQUEST_ID = UUID.randomUUID().toString();
    public static final Task TASK = new Task(new QueueShardId("fake-shard-id"), new PublishRoutingResultPayload(null,
            PROCESSING_ID,
            ROUTING_REQUEST_ID), 0,
            ZonedDateTime.now(), null, "actor");
    public static final TplRoutingException ERROR_EXCEPTION = new TplRoutingException("err");
    @Mock
    private QueueProcessingService<PublishRoutingResultPayload> processingService;
    @Mock
    private RoutingLogDao routingLogDao;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @InjectMocks
    private PublishRoutingResultConsumer publishRoutingResultConsumer;

    @Test
    void handleFailure_newVersion() {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED))
                .thenReturn(true);

        //when
        publishRoutingResultConsumer.handleFailure(ERROR_EXCEPTION, TASK);

        //then
        verify(routingLogDao).updatePublishingStatusByRequestId(eq(ROUTING_REQUEST_ID), eq(RoutingResultStatus.FAILED));
        verify(routingLogDao, never()).updatePublishingStatus(any(), any());
    }

    @Test
    void handleFailure_oldVersion() {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED))
                .thenReturn(false);

        //when
        publishRoutingResultConsumer.handleFailure(ERROR_EXCEPTION, TASK);

        //then
        verify(routingLogDao).updatePublishingStatus(eq(PROCESSING_ID), eq(RoutingResultStatus.FAILED));
        verify(routingLogDao, never()).updatePublishingStatusByRequestId(any(), any());
    }
}
