package ru.yandex.market.logistics.lom.service;

import java.util.HashSet;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableSortedSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.StartDeliveryTracksConsumer;
import ru.yandex.market.logistics.lom.jobs.model.DeliveryTrackIdsPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verify;

public class StartDeliveryTracksTest extends AbstractContextualTest {
    private static final Set<Long> TRACK_IDS = ImmutableSortedSet.of(1L, 2L);

    private static final Task<DeliveryTrackIdsPayload> TASK = TaskFactory.createTask(
        QueueType.START_DELIVERY_TRACKS,
        PayloadFactory.createDeliveryTrackIdsPayload(TRACK_IDS, "1", 1L)
    );

    @Autowired
    private TrackerApiClient trackerApiClient;

    @Autowired
    private StartDeliveryTracksConsumer startDeliveryTracksConsumer;

    @Test
    @DisplayName("Запуск трекинга треков в Delivery Tracker")
    @DatabaseSetup("/service/business_process_state/start_delivery_tracks_1_1_enqueued.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/start_delivery_tracks_1_1_succeeded.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void startMultipleTracksSuccessful() {
        startDeliveryTracksConsumer.execute(TASK);

        verify(trackerApiClient).startMultipleTracks(TRACK_IDS, 3);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=START_TRACKS_SUCCESS\t" +
                    "payload=Tracks started\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                    "tags=BUSINESS_ORDER_EVENT\t" +
                    "entity_types=trackIds\t" +
                    "entity_values=trackIds:[1, 2]\n"
            );
    }

    @Test
    @DisplayName("Неуспешная попытка запуска треков")
    @DatabaseSetup("/service/business_process_state/start_delivery_tracks_1_1_enqueued.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/start_delivery_tracks_1_1_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void startMultipleTracksSuccessfulError() {
        mockTrackerApiClientThenThrow();
        startDeliveryTracksConsumer.execute(TASK);

        verify(trackerApiClient).startMultipleTracks(new HashSet<>(TRACK_IDS), 3);
    }

    private void mockTrackerApiClientThenThrow() {
        Mockito.doThrow(new RuntimeException("Ooops... Delivery tracker is not responding"))
            .when(trackerApiClient).startMultipleTracks(TRACK_IDS, 3);
    }
}
