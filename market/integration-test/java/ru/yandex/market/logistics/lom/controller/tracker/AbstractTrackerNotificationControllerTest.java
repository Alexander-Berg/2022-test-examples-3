package ru.yandex.market.logistics.lom.controller.tracker;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrack;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackCheckpoint;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackMeta;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.jobs.consumer.ProcessCheckpointsFromTrackerConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.ProcessSegmentCheckpointsConsumer;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessSegmentCheckpointsPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
abstract class AbstractTrackerNotificationControllerTest extends AbstractContextualTest {
    protected static final long FF_PARTNER_ID = 145;
    protected static final long DELIVERY_PARTNER_ID = 48;
    protected static final long DS_PARTNER_ID = 1;

    protected static final String CHECKPOINTS_FLOW_DISPLAY_NAME = "New checkpoints flow: {0}";

    @Autowired
    protected QueueTaskChecker queueTaskChecker;
    @Autowired
    protected ProcessCheckpointsFromTrackerConsumer processDeliveryTrackerTrackConsumer;
    @Autowired
    protected ProcessSegmentCheckpointsConsumer processSegmentCheckpointsConsumer;
    @Autowired
    private TvmClientApi tvmClientApi;
    @Autowired
    protected LMSClient lmsClient;
    @Autowired
    protected InternalVariableRepository internalVariableRepository;

    @BeforeEach
    void setUp() {
        setCheckpointsProcessingFlow(false);
    }

    @Nonnull
    static DeliveryTrack createDeliveryTrack(
        List<DeliveryTrackCheckpoint> checkpoints,
        String entityId,
        long trackId,
        String trackCode,
        long lastUpdateDate
    ) {
        return DeliveryTrack.builder()
            .deliveryTrackCheckpoints(checkpoints)
            .deliveryTrackMeta(DeliveryTrackMeta.builder()
                .id(trackId)
                .lastUpdatedDate(Instant.ofEpochMilli(lastUpdateDate))
                .trackCode(trackCode)
                .entityId(entityId)
                .build())
            .build();
    }

    @Nonnull
    static DeliveryTrackCheckpoint createDeliveryTrackCheckpoint(
        long checkpointId,
        long checkpointDate,
        OrderDeliveryCheckpointStatus checkpointStatus
    ) {
        return DeliveryTrackCheckpoint.builder()
            .id(checkpointId)
            .checkpointDate(Instant.ofEpochMilli(checkpointDate))
            .deliveryCheckpointStatus(checkpointStatus)
            .build();
    }

    @Nonnull
    static DeliveryTrackCheckpoint createDeliveryTrackCheckpoint(
        long checkpointId,
        long checkpointDate,
        OrderDeliveryCheckpointStatus checkpointStatus,
        String country,
        String city,
        String location,
        String zipCode
    ) {
        return DeliveryTrackCheckpoint.builder()
            .id(checkpointId)
            .checkpointDate(Instant.ofEpochMilli(checkpointDate))
            .deliveryCheckpointStatus(checkpointStatus)
            .country(country)
            .city(city)
            .location(location)
            .zipCode(zipCode)
            .build();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
        setCheckpointsProcessingFlow(false);
    }

    void notifyTracks(String request, String response) throws Exception {
        mockMvc.perform(
                post("/notifyTracks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent(response));
    }

    void notifyTracksWithTVM(String request, String response) throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
                post("/notifyTracks")
                    .headers(toHttpHeaders(SERVICE_HEADERS))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent(response));
    }

    void assertBillingTransactionQueueTaskCreated(
        SegmentStatus segmentStatus,
        String trackerCheckpointStatus,
        int sequenceId,
        long... subRequestIds
    ) {
        assertBillingTransactionQueueTaskCreatedWithAuthor(
            segmentStatus,
            trackerCheckpointStatus,
            sequenceId,
            new OrderHistoryEventAuthor(),
            subRequestIds
        );
    }

    void assertBillingTransactionQueueTaskCreatedWithAuthor(
        SegmentStatus segmentStatus,
        String trackerCheckpointStatus,
        int sequenceId,
        @Nullable OrderHistoryEventAuthor author,
        long... subRequestIds
    ) {
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_BILLING_TRANSACTION_BY_SEGMENT_STATUSES,
            PayloadFactory.createOrderIdSegmentStatusesPayload(
                8L,
                List.of(
                    LomSegmentCheckpoint.builder()
                        .trackerId(800L)
                        .trackerCheckpointId(1L)
                        .segmentStatus(segmentStatus)
                        .date(Instant.parse("2019-08-06T13:40:00.00Z"))
                        .trackerCheckpointStatus(trackerCheckpointStatus)
                        .build()
                ),
                author,
                subRequestIds
            ),
            sequenceId
        );
    }

    protected void mockLmsClientGetFfPartner(List<PartnerExternalParam> partnerExternalParamTypes) {
        mockLmsClientGetPartner(FF_PARTNER_ID, PartnerType.FULFILLMENT, partnerExternalParamTypes);
    }

    protected void mockLmsClientGetDsPartner(List<PartnerExternalParam> partnerExternalParamTypes) {
        mockLmsClientGetPartner(DELIVERY_PARTNER_ID, PartnerType.DELIVERY, partnerExternalParamTypes);
    }

    protected void mockLmsClientGetDropshipPartner(List<PartnerExternalParam> partnerExternalParamTypes) {
        mockLmsClientGetPartner(DS_PARTNER_ID, PartnerType.DROPSHIP, partnerExternalParamTypes);
    }

    protected void mockLmsClientGetPartner(
        Long partnerId,
        PartnerType partnerType,
        List<PartnerExternalParam> partnerExternalParamTypes
    ) {
        when(lmsClient.getPartner(partnerId)).thenReturn(Optional.of(
            LmsFactory.createPartnerResponse(partnerId, partnerType)
                .params(partnerExternalParamTypes)
                .build()
        ));
    }

    @Nonnull
    protected OrderIdDeliveryTrackPayload getOrderIdDeliveryTrackPayload(
        long orderId,
        OrderDeliveryCheckpointStatus checkpointStatus,
        long trackId
    ) {
        return getOrderIdDeliveryTrackPayload(orderId, checkpointStatus, trackId, "1", 1);
    }

    @Nonnull
    protected OrderIdDeliveryTrackPayload getOrderIdDeliveryTrackPayload(
        long orderId,
        OrderDeliveryCheckpointStatus checkpointStatus,
        long trackId,
        String sequenceId,
        long... subRequestIds
    ) {
        return getOrderIdDeliveryTrackPayload(orderId, checkpointStatus, 1, trackId, sequenceId, subRequestIds);
    }

    @Nonnull
    protected OrderIdDeliveryTrackPayload getOrderIdDeliveryTrackPayload(
        long orderId,
        OrderDeliveryCheckpointStatus checkpointStatus,
        long checkpointId,
        long trackId,
        String sequenceId,
        long... subRequestIds
    ) {
        return PayloadFactory.createOrderIdDeliveryTrackPayload(
            orderId,
            createDeliveryTrack(
                List.of(
                    createDeliveryTrackCheckpoint(checkpointId, 1565098800000L, checkpointStatus)
                ),
                "LO" + orderId,
                trackId,
                "1807474",
                1565092800000L
            ),
            sequenceId,
            subRequestIds
        );
    }

    protected void assertAndProcessTrackTask(OrderIdDeliveryTrackPayload payload) {
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CHECKPOINTS_FROM_TRACKER,
            payload
        );
        processDeliveryTrackerTrackConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_CHECKPOINTS_FROM_TRACKER,
            payload
        ));
    }

    @Nonnull
    protected ProcessSegmentCheckpointsPayload processSegmentCheckpointsPayload(
        long waybillSegmentId,
        long trackerId
    ) {
        return new ProcessSegmentCheckpointsPayload(
            REQUEST_ID,
            waybillSegmentId,
            trackerId,
            new OrderHistoryEventAuthor().setTvmServiceId(222L)
        );
    }

    @Nonnull
    protected ProcessSegmentCheckpointsPayload processSegmentCheckpointsPayloadWithSequence(
        long waybillSegmentId,
        long trackerId
    ) {
        return new ProcessSegmentCheckpointsPayload(
            REQUEST_ID + "/1",
            waybillSegmentId,
            trackerId,
            new OrderHistoryEventAuthor()
        );
    }

    @Nonnull
    protected ProcessSegmentCheckpointsPayload processSegmentCheckpointsPayloadWithSequence(
        long waybillSegmentId,
        long trackerId,
        long sequenceId
    ) {
        ProcessSegmentCheckpointsPayload payload =  new ProcessSegmentCheckpointsPayload(
            REQUEST_ID + "/" + sequenceId,
            waybillSegmentId,
            trackerId,
            new OrderHistoryEventAuthor()
        );
        payload.setSequenceId(sequenceId);

        return payload;
    }

    @Nonnull
    protected ProcessSegmentCheckpointsPayload processSegmentCheckpointsPayloadWithSequenceAndTvm(
        long waybillSegmentId,
        long trackerId,
        long sequenceId
    ) {
        ProcessSegmentCheckpointsPayload payload = new ProcessSegmentCheckpointsPayload(
            REQUEST_ID + "/" + sequenceId,
            waybillSegmentId,
            trackerId,
            new OrderHistoryEventAuthor().setTvmServiceId(222L)
        );
        payload.setSequenceId(sequenceId);

        return payload;
    }

    protected void setCheckpointsProcessingFlow(boolean newFlowEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.NEW_CHECKPOINTS_PROCESSING_FLOW_ENABLED)
                .setValue(String.valueOf(newFlowEnabled))
        );
    }

    protected void assertCheckpointsTaskCreatedAndRunTask(
        boolean newFlowEnabled,
        ProcessSegmentCheckpointsPayload newFlowPayload,
        OrderIdDeliveryTrackPayload oldFlowPayload
    ) {
        if (newFlowEnabled) {
            queueTaskChecker.assertQueueTaskCreated(
                QueueType.PROCESS_SEGMENT_CHECKPOINTS,
                newFlowPayload
            );
            processSegmentCheckpointsConsumer.execute(
                TaskFactory.createTask(
                    QueueType.PROCESS_SEGMENT_CHECKPOINTS,
                    newFlowPayload
                )
            );

            queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_CHECKPOINTS_FROM_TRACKER);
        } else {
            queueTaskChecker.assertQueueTaskCreated(
                QueueType.PROCESS_CHECKPOINTS_FROM_TRACKER,
                oldFlowPayload
            );
            processDeliveryTrackerTrackConsumer.execute(TaskFactory.createTask(
                QueueType.PROCESS_CHECKPOINTS_FROM_TRACKER,
                oldFlowPayload
            ));

            queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_SEGMENT_CHECKPOINTS);
        }
    }
}
