package ru.yandex.market.delivery.tracker;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEvent;
import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEventLogData;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEvent.TRACK_DELETED;
import static ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEvent.TRACK_STARTED;
import static ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEvent.TRACK_STOPPED;

class TrackManagingTest extends AbstractContextualTest {

    private static final String TRACK_CODE_1 = "TRACK_CODE_1";
    private static final String ORDER_1_ID = "ORDER_1";

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    void trackSearching() throws Exception {
        httpOperationWithJsonResult(
            get("/track")
                .param("trackCode", TRACK_CODE_1)
                .param("orderId", ORDER_1_ID),
            "response/wrapped_single_track.json",
            status().isOk()
        );
    }

    @Test
    void gettingDeliveryCheckpoints() throws Exception {
        httpOperationWithJsonResult(
            get("/track/available-statuses"),
            "response/available_statuses.json",
            status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/database/states/two_tracks_with_checkpoints.xml")
    void gettingTrackCheckpointsAll() throws Exception {
        httpOperationWithJsonResult(
            put("/track/checkpoints")
                .content("{\"trackIds\": [1, 2, 3]}")
                .contentType(MediaType.APPLICATION_JSON),
            "response/tracks_checkpoints.json",
            status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/database/states/two_tracks_with_checkpoints.xml")
    void gettingTrackCheckpointsPartially() throws Exception {
        httpOperationWithJsonResult(
            put("/track/checkpoints")
                .content("{\"trackIds\": [1, 3]}")
                .contentType(MediaType.APPLICATION_JSON),
            "response/single_track_checkpoint.json",
            status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/database/states/two_tracks_with_checkpoints.xml")
    void gettingTrackCheckpointsEmpty() throws Exception {
        httpOperationWithJsonResult(
            put("/track/checkpoints")
                .content("{\"trackIds\": [3]}")
                .contentType(MediaType.APPLICATION_JSON),
            "response/empty_array.json",
            status().isOk()
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getTrackCheckpointsValidation")
    void gettingTrackCheckpointsFailed(String name, String requestContent, String responseFile) throws Exception {
        httpOperationWithJsonResult(
            put("/track/checkpoints")
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON),
            responseFile,
            status().isBadRequest()
        );
    }

    @Nonnull
    private static Stream<Arguments> getTrackCheckpointsValidation() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                "{}",
                "response/deliveryTrackCheckpointFilter_trackIds_null.json"
            ),
            Arguments.of(
                "Пустой список trackIds",
                "{\"trackIds\": []}",
                "response/deliveryTrackCheckpointFilter_tracksIds_empty.json"
            ),
            Arguments.of(
                "null-значения в списке trackIds",
                "{\"trackIds\": [1, null]}",
                "response/deliveryTrackCheckpointFilter_tracksIds_elements_null.json"
            )
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    void gettingTrackInfo() throws Exception {
        httpOperationWithJsonResult(
            get("/track/1"),
            "response/single_track_info.json",
            status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    void gettingTrackInfoFailed() throws Exception {
        httpOperationWithJsonResult(
            get("/track/2"),
            "response/track_not_found.json",
            status().is4xxClientError()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    void gettingTrackMeta() throws Exception {
        httpOperationWithJsonResult(
            get("/track/1/meta"),
            "response/single_track_meta.json",
            status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    void gettingTrackMetaFailed() throws Exception {
        httpOperationWithJsonResult(
            get("/track/2/meta"),
            "response/track_not_found.json",
            status().is4xxClientError()
        );
    }

    @Test
    @DatabaseSetup("/database/states/stopped_track_with_checkpoint.xml")
    @ExpectedDatabase(value = "/database/expected/single_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void startTracking() throws Exception {
        httpOperationWithJsonResult(
            put("/track/1/start"),
            "response/single_track_meta_started.json",
            status().isOk()
        );
        checkLogging(TRACK_STARTED);
    }

    @Test
    @DatabaseSetup("/database/states/deleted_tracks.xml")
    @ExpectedDatabase(value = "/database/expected/started_tracks.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void startMultipleTracks() throws Exception {
        httpOperationWithResult(put("/track/start")
            .content(extractFileContent("request/start_multiple_tracks.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/stopped_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void stopTracking() throws Exception {
        httpOperationWithJsonResult(
            put("/track/1/stop"),
            "response/stopped_track_meta.json",
            status().isOk()
        );
        checkLogging(TRACK_STOPPED);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/deleted_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deleteTrack() throws Exception {
        httpOperationWithJsonResult(
            put("/track/1/delete"),
            "response/deleted_track_meta.json",
            status().isOk()
        );
        checkLogging(TRACK_DELETED);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/states/single_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void startTrackingFailed() throws Exception {
        httpOperationWithJsonResult(
            put("/track/2/start"),
            "response/track_not_found.json",
            status().is4xxClientError()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/states/single_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void stopTrackingFailed() throws Exception {
        httpOperationWithJsonResult(
            put("/track/2/stop"),
            "response/track_not_found.json",
            status().is4xxClientError()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/states/single_track_with_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deleteTrackFailed() throws Exception {
        httpOperationWithJsonResult(
            put("/track/2/delete"),
            "response/track_not_found.json",
            status().is4xxClientError()
        );
    }

    private void checkLogging(TrackEvent event) {
        ArgumentCaptor<TrackEventLogData> trackEventCaptor = ArgumentCaptor.forClass(TrackEventLogData.class);
        verifyLogging(trackEventCaptor);

        TrackEventLogData actualTrackEventLogData = trackEventCaptor.getValue();

        assertEquals("Check logged trackCode", TRACK_CODE_1, actualTrackEventLogData.getTrackCode());
        assertEquals("Check logged orderId", ORDER_1_ID, actualTrackEventLogData.getOrderId());
        assertEquals("Check logged eventType", event.readableName(), actualTrackEventLogData.getEventType());
    }

    private void httpOperationWithJsonResult(MockHttpServletRequestBuilder httpOperation,
                                             String responseFile,
                                             ResultMatcher... matchers) throws Exception {

        String contentAsString = httpOperationWithResult(httpOperation, matchers);

        assertions()
            .assertThat(contentAsString)
            .is(jsonNonStrictMatching(extractFileContent(responseFile)));
    }
}
