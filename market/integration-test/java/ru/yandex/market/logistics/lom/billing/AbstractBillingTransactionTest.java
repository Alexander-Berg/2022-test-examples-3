package ru.yandex.market.logistics.lom.billing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrack;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackCheckpoint;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackMeta;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTracks;
import ru.yandex.market.logistics.lom.converter.tracker.SegmentStatusConverter;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.jobs.consumer.ProcessCheckpointsFromTrackerConsumer;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdSegmentStatusesPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.BillingTransactionSegmentStatusesProcessor;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdSegmentStatusesPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
public abstract class AbstractBillingTransactionTest extends AbstractContextualTest {

    @Autowired
    QueueTaskChecker queueTaskChecker;
    @Autowired
    protected BillingTransactionSegmentStatusesProcessor billingTransactionProcessor;
    @Autowired
    private SegmentStatusConverter segmentStatusConverter;
    @Autowired
    private ProcessCheckpointsFromTrackerConsumer processDeliveryTrackerTrackConsumer;
    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Транзакции создаются для дропоффа (партнёр СД, тип сегмента SC) при получении 130 чп")
    @DatabaseSetup("/billing/before/dropoff_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/dropoff_tx_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkTransactionNotCreatedForDropoff() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 1);
    }

    void notifyTrack(DeliveryTracks tracks) throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/notifyTracks", tracks))
            .andExpect(status().isOk())
            .andExpect(jsonPath("results[0].status").value("OK"));
    }

    @Nonnull
    static DeliveryTracks.DeliveryTracksBuilder defaultTracks(OrderDeliveryCheckpointStatus status) {
        return defaultTracks(List.of(status), 100L, false);
    }

    @Nonnull
    static DeliveryTracks.DeliveryTracksBuilder defaultTracks(OrderDeliveryCheckpointStatus status, long trackId) {
        return defaultTracks(List.of(status), trackId, false);
    }

    @Nonnull
    static DeliveryTracks.DeliveryTracksBuilder defaultTracks(List<OrderDeliveryCheckpointStatus> statuses) {
        return defaultTracks(statuses, 100L, false);
    }

    @Nonnull
    static DeliveryTrack.DeliveryTrackBuilder defaultTrack(OrderDeliveryCheckpointStatus status) {
        return defaultTrack(List.of(status), 100L, false);
    }

    @Nonnull
    static DeliveryTrack.DeliveryTrackBuilder defaultTrack(OrderDeliveryCheckpointStatus status, long trackId) {
        return defaultTrack(List.of(status), trackId, false);
    }

    @Nonnull
    static DeliveryTrack.DeliveryTrackBuilder defaultTrack(List<OrderDeliveryCheckpointStatus> statuses) {
        return defaultTrack(statuses, 100L, false);
    }

    @Nonnull
    static DeliveryTracks.DeliveryTracksBuilder defaultTracks(
        List<OrderDeliveryCheckpointStatus> statuses,
        boolean needReverse
    ) {
        return defaultTracks(statuses, 100L, needReverse);
    }

    @Nonnull
    static DeliveryTracks.DeliveryTracksBuilder defaultTracks(
        List<OrderDeliveryCheckpointStatus> statuses,
        long trackId,
        boolean needReverse
    ) {
        return DeliveryTracks.builder().tracks(List.of(defaultTrack(statuses, trackId, needReverse).build()));
    }

    @Nonnull
    static DeliveryTrack.DeliveryTrackBuilder defaultTrack(
        List<OrderDeliveryCheckpointStatus> statuses,
        long trackId,
        boolean needReverse
    ) {
        long dateMilli = 1565092800000L;
        return DeliveryTrack.builder()
            .deliveryTrackCheckpoints(EntryStream.of(statuses)
                .mapKeyValue((index, status) -> DeliveryTrackCheckpoint.builder()
                    .id(777L + (needReverse ? statuses.size() - 1 - index : index))
                    .checkpointDate(Instant.ofEpochMilli(dateMilli))
                    .deliveryCheckpointStatus(status)
                    .build())
                .collect(Collectors.toList()))
            .deliveryTrackMeta(DeliveryTrackMeta.builder()
                .id(trackId)
                .lastUpdatedDate(Instant.ofEpochMilli(dateMilli))
                .trackCode("1807474")
                .entityId("LO1")
                .build());
    }

    void checkAsyncTask(DeliveryTracks tracks, long sequenceId, long... subRequestIds) {
        OrderIdSegmentStatusesPayload payload = getAndCheckPayload(tracks, sequenceId, subRequestIds);
        billingTransactionProcessor.processPayload(payload);
    }

    @Nonnull
    private List<LomSegmentCheckpoint> mapToSegmentCheckpoints(DeliveryTracks tracks) {
        DeliveryTrack track = tracks.getTracks().get(0);
        return track.getDeliveryTrackCheckpoints().stream()
            .map(
                deliveryTrackCheckpoint ->
                    LomSegmentCheckpoint.builder()
                        .segmentStatus(segmentStatusConverter.convertToSegmentStatus(
                            deliveryTrackCheckpoint.getDeliveryCheckpointStatus()
                        ))
                        .date(deliveryTrackCheckpoint.getCheckpointDate())
                        .trackerCheckpointId(deliveryTrackCheckpoint.getId())
                        .trackerId(track.getDeliveryTrackMeta().getId())
                        .trackerCheckpointStatus(deliveryTrackCheckpoint.getDeliveryCheckpointStatus().name())
                        .build()
            )
            .collect(Collectors.toList());
    }

    @Nonnull
    private OrderIdSegmentStatusesPayload getAndCheckPayload(
        DeliveryTracks tracks,
        long sequenceId,
        long... subRequestIds
    ) {
        List<LomSegmentCheckpoint> checkpoints = mapToSegmentCheckpoints(tracks);
        OrderIdSegmentStatusesPayload payload = createOrderIdSegmentStatusesPayload(
            1L,
            checkpoints,
            subRequestIds
        );
        payload.setSequenceId(sequenceId);
        queueTaskChecker.assertQueueTaskCreated(QueueType.CREATE_BILLING_TRANSACTION_BY_SEGMENT_STATUSES, payload);
        return payload;
    }

    void processDeliveryTrack(DeliveryTrack deliveryTrack) {
        processDeliveryTrackerTrackConsumer.execute(
            TaskFactory.createTask(
                QueueType.PROCESS_CHECKPOINTS_FROM_TRACKER,
                PayloadFactory.createOrderIdDeliveryTrackPayload(
                    1,
                    deliveryTrack,
                    "1",
                    1
                )
            )
        );
    }

    void processAndCheckTransaction(OrderDeliveryCheckpointStatus status, long subRequestId) throws Exception {
        DeliveryTracks tracks = defaultTracks(status).build();
        notifyTrack(tracks);
        processDeliveryTrack(defaultTrack(status).build());
        checkAsyncTask(tracks, 1 + subRequestId, 1, subRequestId);
    }

    void processAndCheckTransaction(
        OrderDeliveryCheckpointStatus status,
        long trackId,
        long subRequestId
    ) throws Exception {
        DeliveryTracks tracks = defaultTracks(status, trackId).build();
        notifyTrack(tracks);
        processDeliveryTrack(defaultTrack(status, trackId).build());
        checkAsyncTask(tracks, 1 + subRequestId, 1, subRequestId);
    }

    protected void mockLmsClientGetPartner(List<PartnerExternalParam> partnerExternalParamTypes) {
        when(lmsClient.getPartner(101L)).thenReturn(Optional.of(
            LmsFactory.createPartnerResponse(101L, PartnerType.DELIVERY)
                .params(partnerExternalParamTypes)
                .build()
        ));
    }
}
