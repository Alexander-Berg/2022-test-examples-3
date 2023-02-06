package ru.yandex.market.delivery.tracker;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEvent;
import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEventLogData;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrackRegistrationTest extends AbstractContextualTest {

    private static final String TRACK_CODE_1 = "TRACK_CODE_1";
    private static final long DS_1_ID = 101;
    private static final long CONSUMER_ID = 10;
    private static final long LOM_CONSUMER_ID = 2;
    private static final long MOCK_CONSUMER_ID = 3;
    private static final long TPL_CONSUMER_ID = 4;
    private static final String ORDER_1_ID = "ORDER_1";
    private static final String MOVEMENT_1_ID = "MOVEMENT_1";

    private final ArgumentCaptor<TrackEventLogData> trackEventCaptor = ArgumentCaptor.forClass(TrackEventLogData.class);

    @Test
    void failRegisterTrackWithoutOrderId() throws Exception {
        String contentAsString = httpOperationWithResult(
            put("/track")
                .param("trackCode", TRACK_CODE_1)
                .param("deliveryServiceId", "12345")
                .param("consumerId", String.valueOf(CONSUMER_ID)),
            status().is5xxServerError()
        );

        assertions()
            .assertThat(contentAsString)
            .contains("Both entityId and orderId were missing (null) for trackCode");
    }

    @Test
    @DatabaseSetup("/database/states/empty_DB.xml")
    void failRegisterTrackOnDsNotFoundInDatabase() throws Exception {

        String contentAsString = httpOperationWithResult(
            put("/track")
                .param("trackCode", TRACK_CODE_1)
                .param("deliveryServiceId", "123")
                .param("consumerId", String.valueOf(CONSUMER_ID))
                .param("orderId", ORDER_1_ID),
            status().is4xxClientError()
        );

        assertions()
            .assertThat(contentAsString)
            .contains("DeliveryService not found id: 123");
    }

    @Test
    @DatabaseSetup("/database/states/ds_exist.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_registered.xml", assertionMode = NON_STRICT_UNORDERED)
    void registerTrackSuccessfully() throws Exception {
        putTrack(TrackEvent.TRACK_REGISTERED, CONSUMER_ID, "response/single_track.json");
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_updated.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateTrackSuccessfully() throws Exception {
        putTrack(TrackEvent.TRACK_UPDATED, CONSUMER_ID, "response/single_track.json");
    }

    @Test
    @DatabaseSetup("/database/states/single_order_track.xml")
    @ExpectedDatabase(
        value = "/database/expected/two_tracks_same_track_code_and_service_different_entity_types.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTrackWithSameTrackCodeAndServiceButDifferentEntityTypeSuccessfully() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            CONSUMER_ID,
            MOVEMENT_1_ID,
            MOVEMENT_1_ID,
            EntityType.MOVEMENT,
            "response/single_movement_track.json"
        );
    }

    @Test
    @DatabaseSetup("/database/states/lom_consumer.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_lom_registered.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerLomTrackCheckBackUrl() throws Exception {
        putTrack(TrackEvent.TRACK_REGISTERED, LOM_CONSUMER_ID, "response/lom_track.json");
    }

    @Test
    @DatabaseSetup("/database/states/perf_testing_mock_consumer.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_perf_testing_registered.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerPerfTestingTrackCheckBackUrl() throws Exception {
        putTrack(TrackEvent.TRACK_REGISTERED, MOCK_CONSUMER_ID, "response/perf_testing_track.json");
    }

    @Test
    @DatabaseSetup("/database/states/tpl_consumer.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_tpl_registered.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTplTrackCheckBackUrl() throws Exception {
        putTrack(TrackEvent.TRACK_REGISTERED, TPL_CONSUMER_ID, "response/tpl_track.json");
    }

    @Test
    @DatabaseSetup("/database/states/ds_exist.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_registered.xml", assertionMode = NON_STRICT_UNORDERED)
    void registerTrackWithOnlyOrderIdSuccessfully() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            CONSUMER_ID,
            null,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/single_track.json"
        );
    }

    @Test
    @DatabaseSetup("/database/states/ds_exist.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_registered.xml", assertionMode = NON_STRICT_UNORDERED)
    void registerTrackWithOnlyOrderIdWithoutEntityTypeSuccessfully() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            CONSUMER_ID,
            null,
            ORDER_1_ID,
            null,
            "response/single_track.json"
        );
    }

    @Test
    @DatabaseSetup("/database/states/ds_exist.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_registered.xml", assertionMode = NON_STRICT_UNORDERED)
    void registerTrackWithOnlyEntityIdSuccessfully() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            CONSUMER_ID,
            ORDER_1_ID,
            null,
            EntityType.ORDER,
            "response/single_track.json"
        );
    }

    @Test
    @DatabaseSetup("/database/states/ds_exist.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_registered.xml", assertionMode = NON_STRICT_UNORDERED)
    void registerTrackWithOnlyEntityIdWithoutEntityTypeSuccessfully() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            CONSUMER_ID,
            ORDER_1_ID,
            null,
            null,
            "response/single_track.json"
        );
    }

    @Test
    @DatabaseSetup("/database/states/ds_exist.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_registered.xml", assertionMode = NON_STRICT_UNORDERED)
    void registerTrackWithoutEntityTypeSuccessfully() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            null,
            "response/single_track.json"
        );
    }

    @Test
    @DatabaseSetup("/database/states/ds_exist.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_movement_track_registered.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTrackForMovement() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            CONSUMER_ID,
            MOVEMENT_1_ID,
            MOVEMENT_1_ID,
            EntityType.MOVEMENT,
            "response/single_movement_track.json"
        );
    }

    @Test
    @DatabaseSetup("/database/states/lom_consumer.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_two_consumers.xml", assertionMode = NON_STRICT_UNORDERED)
    void registerTwoConsumers() throws Exception {
        doPutTrack(CONSUMER_ID, "response/single_track.json");
        doPutTrack(LOM_CONSUMER_ID, "response/single_track.json");

        verifyLogging(trackEventCaptor, 2);

        List<TrackEventLogData> actualTrackEventLogData = trackEventCaptor.getAllValues();

        assertions().assertThat(actualTrackEventLogData)
            .extracting(TrackEventLogData::getTrackCode)
            .containsOnly(TRACK_CODE_1);
        assertions().assertThat(actualTrackEventLogData)
            .extracting(TrackEventLogData::getServiceId)
            .containsOnly(DS_1_ID);
        assertions().assertThat(actualTrackEventLogData)
            .extracting(TrackEventLogData::getOrderId)
            .containsOnly(ORDER_1_ID);
        assertions().assertThat(actualTrackEventLogData)
            .extracting(TrackEventLogData::getComment)
            .allMatch(comment -> comment.contains("id="));
        assertions().assertThat(actualTrackEventLogData)
            .extracting(TrackEventLogData::getEventType)
            .containsExactly(
                TrackEvent.TRACK_REGISTERED.readableName(),
                TrackEvent.TRACK_UPDATED.readableName()
            );
        assertions().assertThat(actualTrackEventLogData)
            .extracting(TrackEventLogData::getConsumerId)
            .containsExactly(
                CONSUMER_ID,
                LOM_CONSUMER_ID
            );
    }

    @Test
    @DatabaseSetup("/database/states/lom_consumer.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_api_version.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTrackWithApiVersion() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version.json",
            ApiVersion.DS
        );
    }

    @Test
    @DatabaseSetup("/database/states/lom_consumer.xml")
    @ExpectedDatabase(
        value = "/database/expected/two_tracks_different_api_version.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTracksDifferentApiVersion() throws Exception {
        doPutTrack(
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version.json",
            ApiVersion.DS
        );
        doPutTrack(
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version_second.json",
            ApiVersion.FF
        );

        verifyLogging(trackEventCaptor, 2);
        List<TrackEventLogData> events = trackEventCaptor.getAllValues();
        assertLogging(
            events.get(0),
            TrackEvent.TRACK_REGISTERED,
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            ApiVersion.DS
        );
        assertLogging(
            events.get(1),
            TrackEvent.TRACK_REGISTERED,
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            ApiVersion.FF
        );
    }

    @Test
    @DatabaseSetup("/database/states/lom_consumer.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_api_version.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTrackApiVersionSetIntoNull() throws Exception {
        doPutTrack(
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version.json",
            ApiVersion.DS
        );
        doPutTrack(
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version.json",
            null
        );

        verifyLogging(trackEventCaptor, 2);
        List<TrackEventLogData> events = trackEventCaptor.getAllValues();
        assertLogging(
            events.get(0),
            TrackEvent.TRACK_REGISTERED,
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            ApiVersion.DS
        );
        assertLogging(
            events.get(1),
            TrackEvent.TRACK_UPDATED,
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            ApiVersion.DS
        );
    }

    @Test
    @DatabaseSetup("/database/states/lom_consumer.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_no_api_version.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTrackApiVersionNullIntoSet() throws Exception {
        doPutTrack(
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version.json",
            null
        );
        doPutTrack(
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version.json",
            ApiVersion.DS
        );

        verifyLogging(trackEventCaptor, 2);
        List<TrackEventLogData> events = trackEventCaptor.getAllValues();
        assertLogging(
            events.get(0),
            TrackEvent.TRACK_REGISTERED,
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            ApiVersion.DS
        );
        assertLogging(
            events.get(1),
            TrackEvent.TRACK_UPDATED,
            LOM_CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            ApiVersion.DS
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_order_track.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_api_version_existing.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTrackApiVersionNullIntoDefault() throws Exception {
        doPutTrack(
            CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version_existing.json",
            null
        );

        verifyLogging(trackEventCaptor);
        assertLogging(
            trackEventCaptor.getValue(),
            TrackEvent.TRACK_UPDATED,
            CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            ApiVersion.DS
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_order_track.xml")
    @ExpectedDatabase(
        value = "/database/expected/two_tracks_null_api_version.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void registerTrackApiVersionNullIntoNonDefault() throws Exception {
        putTrack(
            TrackEvent.TRACK_REGISTERED,
            CONSUMER_ID,
            ORDER_1_ID,
            ORDER_1_ID,
            EntityType.ORDER,
            "response/track_api_version_ff.json",
            ApiVersion.FF
        );
    }

    private void putTrack(TrackEvent event, long consumerId, String resultPath) throws Exception {
        putTrack(event, consumerId, ORDER_1_ID, ORDER_1_ID, EntityType.ORDER, resultPath);
    }

    private void putTrack(
        TrackEvent event,
        long consumerId,
        String entityId,
        String orderId,
        EntityType entityType,
        String resultPath
    ) throws Exception {
        putTrack(event, consumerId, entityId, orderId, entityType, resultPath, ApiVersion.DS);
    }

    private void putTrack(
        TrackEvent event,
        long consumerId,
        String entityId,
        String orderId,
        EntityType entityType,
        String resultPath,
        ApiVersion apiVersion
    ) throws Exception {
        doPutTrack(consumerId, entityId, orderId, entityType, resultPath, apiVersion);

        verifyLogging(trackEventCaptor);
        assertLogging(trackEventCaptor.getValue(), event, consumerId, entityId, orderId, apiVersion);
    }

    private void assertLogging(
        TrackEventLogData eventData,
        TrackEvent event,
        long consumerId,
        String entityId,
        String orderId,
        ApiVersion apiVersion
    ) {
        assertEquals(TRACK_CODE_1, eventData.getTrackCode(), "Check logged trackCode");
        assertEquals(DS_1_ID, eventData.getServiceId(), "Check logged serviceId");
        assertEquals(consumerId, eventData.getConsumerId(), "Check logged consumerId");
        assertEquals(
            Optional.ofNullable(entityId).orElse(orderId),
            eventData.getOrderId(),
            "Check logged orderId"
        );
        assertEquals(apiVersion, eventData.getApiVersion(), "Check logged apiVersion");
        assertEquals(event.readableName(), eventData.getEventType(), "Check logged eventType");
        assertTrue(eventData.getComment().contains("id="), "Comment must contain id");
    }

    private void doPutTrack(long consumerId, String resultPath) throws Exception {
        doPutTrack(consumerId, ORDER_1_ID, ORDER_1_ID, EntityType.ORDER, resultPath, null);
    }

    @ParametersAreNonnullByDefault
    private void doPutTrack(
        long consumerId,
        String entityId,
        String orderId,
        @Nullable EntityType entityType,
        String resultPath,
        @Nullable ApiVersion apiVersion
    ) throws Exception {
        String contentAsString = httpOperationWithResult(
            put("/track")
                .param("trackCode", TRACK_CODE_1)
                .param("deliveryServiceId", String.valueOf(DS_1_ID))
                .param("consumerId", String.valueOf(consumerId))
                .param("entityId", entityId)
                .param("orderId", orderId)
                .param("estimatedArrivalDateFrom", "2018-01-01")
                .param("estimatedArrivalDateTo", "2018-12-31")
                .param("deliveryType", String.valueOf(DeliveryType.DELIVERY.getId()))
                .param("isGlobalOrder", "true")
                .param(
                    "entityType",
                    Optional.ofNullable(entityType)
                        .map(EntityType::getId)
                        .map(String::valueOf)
                        .orElse(null)
                )
                .param(
                    "apiVersion",
                    Optional.ofNullable(apiVersion)
                        .map(Enum::name)
                        .orElse(null)
                ),
            status().is2xxSuccessful()
        );

        assertions()
            .assertThat(contentAsString)
            .is(jsonNonStrictMatching(extractFileContent(resultPath)));
    }
}
