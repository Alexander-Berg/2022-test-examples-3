package ru.yandex.market.delivery.tracker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.delivery.tracker.configuration.TestQueueConfiguration;
import ru.yandex.market.delivery.tracker.domain.dto.TrackConsumerDto;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackService;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = "queue.push-to-consumer.threadCount=1")
class PushTrackQueueTest extends AbstractContextualTest {

    @Autowired
    @Qualifier("pushToTrackConsumerQueueProducer")
    private QueueProducer<TrackConsumerDto> queueProducer;

    @Autowired
    private TaskLifecycleListener listener;

    @Autowired
    private PushTrackService pushTrackService;

    private TestQueueConfiguration.LatchQueueTaskListener taskListener;
    private CountDownLatch countDownLatch;

    @BeforeEach
    void setUp() {
        taskListener = (TestQueueConfiguration.LatchQueueTaskListener) listener;
        countDownLatch = new CountDownLatch(1);
        taskListener.setFinishedLatch(countDownLatch);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    void testQueueConsumer() throws InterruptedException {
        doNothing().when(pushTrackService).pushTrack(anyLong(), anyLong());

        queueProducer.enqueue(EnqueueParams.create(new TrackConsumerDto(10L, 1L)));

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertions().assertThat(taskListener.getLastResults().get(0))
            .isEqualTo(TaskExecutionResult.finish());
        verify(pushTrackService).pushTrack(10L, 1L);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    void testQueueConsumerFailed() throws InterruptedException {
        doThrow(RuntimeException.class).when(pushTrackService).pushTrack(anyLong(), anyLong());

        queueProducer.enqueue(EnqueueParams.create(new TrackConsumerDto(10L, 1L)));

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertions().assertThat(taskListener.getLastResults().get(0))
            .isEqualTo(TaskExecutionResult.fail());
        verify(pushTrackService).pushTrack(10L, 1L);
    }

}
