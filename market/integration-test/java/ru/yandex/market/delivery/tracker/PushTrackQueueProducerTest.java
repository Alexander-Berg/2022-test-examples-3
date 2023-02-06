package ru.yandex.market.delivery.tracker;

import java.sql.Date;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.delivery.tracker.domain.dto.TrackConsumerDto;
import ru.yandex.market.delivery.tracker.service.ConsumerService;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackQueueProducer;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackService;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskPayloadTransformer;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PushTrackQueueProducerTest extends AbstractContextualTest {

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    @Qualifier("pushToTrackConsumerQueueProducer")
    private QueueProducer<TrackConsumerDto> queueProducer;

    @Autowired
    private PushTrackService pushTrackService;

    @Autowired
    private TaskPayloadTransformer<TrackConsumerDto> payloadTransformer;

    @Autowired
    private Clock clock;

    private QueueProducer<TrackConsumerDto> queueProducerSpy;
    private PushTrackQueueProducer producer;
    private ArgumentCaptor<EnqueueParams<TrackConsumerDto>> argumentCaptor;

    @BeforeEach
    void setUp() {
        queueProducerSpy = Mockito.spy(queueProducer);
        producer = new PushTrackQueueProducer(consumerService, queueProducerSpy, payloadTransformer, clock);
        doNothing().when(pushTrackService).pushTrack(anyLong(), anyLong());
        argumentCaptor = ArgumentCaptor.forClass(EnqueueParams.class);
    }

    /**
     * В БД сущность, у которой единственный трек с незаполненой датой получения чекпоинта. Очередь пуста.
     * Сущность не будет запушена.
     */
    @Test
    @DatabaseSetup("/database/states/track_never_acquired_checkpoints.xml")
    void doNotEnqueueTrackNeverAcquiredCheckpoints() {
        producer.queueTracksToPush(Date.from(clock.instant().truncatedTo(ChronoUnit.DAYS)));

        verify(queueProducerSpy, never()).enqueue(any(EnqueueParams.class));
    }

    /**
     * В БД сущность с треком, у которой незаполненая датой последнего успешного пуша. Очередь пуста.
     * Сущность будет запушена.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/states/single_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void enqueueTrackWithCheckpointsFirstTime() {
        producer.queueTracksToPush(Date.from(clock.instant().truncatedTo(ChronoUnit.DAYS)));

        verify(queueProducerSpy).enqueue(argumentCaptor.capture());
        assertTrackConsumerDto(10L, 1L);
    }

    /**
     * В БД сущность, для которой актуальное состояние трека уже было запушено. Очередь пуста.
     * Сущность не будет запушена.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_pushed_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/states/single_track_with_pushed_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void doNotEnqueueTrackWithoutNewCheckpointsAfterLastPush() {
        producer.queueTracksToPush(Date.from(clock.instant().truncatedTo(ChronoUnit.DAYS)));

        verify(queueProducerSpy, never()).enqueue(any(EnqueueParams.class));
    }

    /**
     * При попытке пуша entity_id=ORDER_1 два раза в очереди будет только одна сущность.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/states/single_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void enqueueAlreadyQueuedEntityId() {
        producer.queueTracksToPush(Date.from(clock.instant().truncatedTo(ChronoUnit.DAYS)));
        producer.queueTracksToPush(Date.from(clock.instant().truncatedTo(ChronoUnit.DAYS)));

        verify(queueProducerSpy).enqueue(argumentCaptor.capture());
        assertTrackConsumerDto(10L, 1L);
    }

    /**
     * Очередь пуста.
     * Пуш нескольких entity_id.
     */
    @Test
    @DatabaseSetup("/database/states/tracks_batch.xml")
    @ExpectedDatabase(
        value = "/database/states/tracks_batch.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void enqueueBatchOfEntityIds() {
        producer.queueTracksToPush(Date.from(clock.instant().truncatedTo(ChronoUnit.DAYS)));
        verify(queueProducerSpy, times(7)).enqueue(argumentCaptor.capture());

        assertTrackConsumerDto(Lists.newArrayList(
            new TrackConsumerDto(10L, 1L),
            new TrackConsumerDto(10L, 2L),
            new TrackConsumerDto(10L, 3L),
            new TrackConsumerDto(10L, 4L),
            new TrackConsumerDto(10L, 5L),
            new TrackConsumerDto(10L, 7L),
            new TrackConsumerDto(10L, 10L)
        ));
    }

    /**
     * В БД сущность с треком, который был удалён, но остался незапушенный чекпоинт.
     * Сущность будет запушена.
     */
    @Test
    @DatabaseSetup("/database/states/deleted_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/states/deleted_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void enqueueDeletedEntityId() {
        producer.queueTracksToPush(Date.from(clock.instant().truncatedTo(ChronoUnit.DAYS)));

        verify(queueProducerSpy, atLeastOnce()).enqueue(argumentCaptor.capture());
        assertTrackConsumerDto(10L, 1L);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_two_consumers_and_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/states/single_track_with_two_consumers_and_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void pushTrackTwoConsumers() {
        producer.enqueue(1L);

        verify(queueProducerSpy, times(2)).enqueue(argumentCaptor.capture());
        assertTrackConsumerDto(Lists.newArrayList(
            new TrackConsumerDto(2L, 1L),
            new TrackConsumerDto(10L, 1L)
        ));
    }


    private void assertTrackConsumerDto(List<TrackConsumerDto> expected) {
        List<TrackConsumerDto> actual = argumentCaptor.getAllValues().stream()
            .map(item -> item.getPayload())
            .collect(Collectors.toList());
        assertions().assertThat(actual).hasSize(expected.size()).containsAll(expected);
    }

    private void assertTrackConsumerDto(long expectedConsumerId, long expectedTrackId) {
        TrackConsumerDto actual = argumentCaptor.getValue().getPayload();

        assertions().assertThat(actual.getConsumerId()).isEqualTo(expectedConsumerId);
        assertions().assertThat(actual.getTrackId()).isEqualTo(expectedTrackId);
    }
}
