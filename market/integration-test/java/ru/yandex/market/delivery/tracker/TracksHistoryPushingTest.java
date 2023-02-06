package ru.yandex.market.delivery.tracker;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.model.Partner;
import ru.yandex.market.delivery.tracker.domain.model.TrackerEntity;
import ru.yandex.market.delivery.tracker.domain.model.TrackerHistory;
import ru.yandex.market.delivery.tracker.domain.model.TrackerStatus;
import ru.yandex.market.delivery.tracker.domain.model.request.PushTracksHistoryRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TracksHistoryPushingTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/expected/push_tracks_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithCorrectRequestReturnsOkStatus() throws Exception {
        httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/expected/push_tracks_history_required_only.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithOnlyRequiredDataInRequestReturnsOkStatus() throws Exception {
        httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_required_only.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }

    @Test
    void testPushEmptyHistoryReturnsBadRequestStatus() throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_empty_history.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isBadRequest());

        assertions()
            .assertThat(result)
            .contains("size must be between 1 and 100");
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    void testPushGreaterThanLimitCountOfHistoriesReturnsBadRequestStatus() throws Exception {
        int historyMaxSize = 100;

        PushTracksHistoryRequest request = new PushTracksHistoryRequest(
            List.of(new Partner(101L)),
            ApiVersion.FF,
            Collections.nCopies(historyMaxSize + 1, new TrackerHistory(
                new TrackerEntity(
                    "301",
                    EntityType.ORDER,
                    "test_track_code",
                    null
                ),
                List.of(new TrackerStatus(
                    100,
                    new Date(),
                    "message"
                ))
            ))
        );
        String result = httpOperationWithResult(put("/track/push-history")
            .content(objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .writeValueAsString(request))
            .contentType(MediaType.APPLICATION_JSON), status().isBadRequest());

        assertions()
            .assertThat(result)
            .contains("size must be between 1 and 100");
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/states/push_tracks_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithSeveralPartnersReturnsBadRequestStatusWhenPartnersNotFound() throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_with_wrong_partners.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isBadRequest());

        assertions()
            .assertThat(result)
            .contains("Unknown partners id");
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history_without_delivery_track_meta.xml")
    @ExpectedDatabase(
        value = "/database/states/push_tracks_history_without_delivery_track_meta.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryReturnsBadRequestStatusWhenAnyDeliveryTrackMetaNotFound() throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isBadRequest());

        assertions()
            .assertThat(result)
            .contains("Delivery track meta not found, deliveryServicesId=[101]; trackCode=test_track_code");
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/expected/push_tracks_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryReturnsBadRequestStatusWhenDeliveryTrackMetaForOneOfSeveralHistoriesNotFound()
        throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_with_several_histories.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isBadRequest());

        assertions()
            .assertThat(result)
            .contains("Delivery track meta not found, deliveryServicesId=[101]; trackCode=test_track_code_2");
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/states/push_tracks_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithWrongApiVersionReturnsBadRequestStatus()
            throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
                .content(
                        extractFileContent("request/ds_push_tracks_history.json")
                )
                .contentType(MediaType.APPLICATION_JSON), status().isBadRequest());

        assertions()
                .assertThat(result)
                .contains("Delivery track meta not found, deliveryServicesId=[101]; trackCode=test_track_code");
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/expected/push_tracks_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithSeveralPartnersReturnsOkStatusWhenOneDeliveryTrackMetaFound() throws Exception {
        httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_with_several_partners.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history_without_api_vers_for_one_of_delivery_tracks.xml")
    @ExpectedDatabase(
        value = "/database/expected/push_tracks_history_without_api_vers_for_one_of_delivery_tracks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithSeveralPartnersReturnsOkStatusWhenApiVersionOfOneOfDeliveryTracksNotSet()
        throws Exception {
        httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_with_several_partners.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }

    @Test
    @DatabaseSetup(
        "/database/states/push_tracks_history_without_api_vers_for_one_of_deliv_tracks_and_with_deliv_service_type.xml"
    )
    @ExpectedDatabase(
        value = "/database/states/" +
                "push_tracks_history_without_api_vers_for_one_of_deliv_tracks_and_with_deliv_service_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithSeveralPartnersCausesIntServErrWhenApiVersOfOneOfDelivTracksNotSetAndDelivServiceHasType()
        throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_with_several_partners.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isInternalServerError());

        assertions()
            .assertThat(result)
            .contains("Several partners have the same delivery track");
    }

    @Test
    @DatabaseSetup(
        "/database/states/push_tracks_history_with_several_delivery_tracks_with_same_track_code_and_api_vers.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/states/push_tracks_history_with_several_delivery_tracks_with_same_track_code_and_api_vers.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithSeveralPartnersCausesInternalServerErrorWhenSeveralDeliveryTracksFound()
        throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
            .content(
                extractFileContent("request/ff_push_tracks_history_with_several_partners.json")
            )
            .contentType(MediaType.APPLICATION_JSON), status().isInternalServerError());

        assertions()
            .assertThat(result)
            .contains("Several partners have the same delivery track");
    }

    @Test
    @DatabaseSetup(
        "/database/states/push_tracks_history_with_several_delivery_tracks_with_same_track_code_and_diff_api_ver.xml"
    )
    @ExpectedDatabase(
        value = "/database/expected/" +
                "push_tracks_history_with_several_delivery_tracks_with_same_track_code_and_diff_api_ver.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithSeveralPartnersWithDifferentApiVersionsReturnsOkStatusWhenOneDeliveryTrackFound()
        throws Exception {
        httpOperationWithResult(put("/track/push-history")
            .content(
                extractFileContent("request/ff_push_tracks_history_with_several_partners.json")
            )
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history_with_delivery_track_with_external_delivery_service_id.xml")
    @ExpectedDatabase(
        value = "/database/expected/push_tracks_history_with_delivery_track_with_external_delivery_service_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithExternalDeliveryServiceIdReturnsOkStatusWhenExternalDeliveryServiceIdIsNotNullInDB()
        throws Exception {
        httpOperationWithResult(put("/track/push-history")
            .content(
                extractFileContent("request/ff_push_tracks_history_with_external_delivery_service_id.json")
            )
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/states/push_tracks_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithExternalDeliveryServiceIdReturnsBadRequestStatusWhenExternalDeliveryServiceIdIsNullInDB()
        throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
            .content(
                extractFileContent("request/ff_push_tracks_history_with_external_delivery_service_id.json")
            )
            .contentType(MediaType.APPLICATION_JSON), status().isBadRequest());

        assertions()
            .assertThat(result)
            .contains("Delivery track meta not found, deliveryServicesId=[101]; trackCode=test_track_code");
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history_without_track_request_meta.xml")
    @ExpectedDatabase(
        value = "/database/states/push_tracks_history_without_track_request_meta.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryReturnsBadRequestWhenTrackRequestMetaNotFound() throws Exception {
        String result = httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isBadRequest());

        assertions()
            .assertThat(result)
            .contains("Track request meta not found");
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/expected/push_tracks_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithNullEntityIdReturnsOkStatus() throws Exception {
        httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_null_entity_id.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }

    @Test
    @DatabaseSetup("/database/states/push_tracks_history.xml")
    @ExpectedDatabase(
        value = "/database/expected/push_tracks_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPushHistoryWithEmptyEntityIdReturnsOkStatus() throws Exception {
        httpOperationWithResult(put("/track/push-history")
            .content(extractFileContent("request/ff_push_tracks_history_empty_entity_id.json"))
            .contentType(MediaType.APPLICATION_JSON), status().isOk());
    }
}
