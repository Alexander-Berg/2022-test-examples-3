package ru.yandex.market.delivery.tracker.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.delivery.tracker.AbstractContextualTest;
import ru.yandex.market.delivery.tracker.configuration.ClockTestConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/database/states/admin_tracks_setup.xml")
class AdminTrackerControllerTest extends AbstractContextualTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getTracksSource")
    void getTracksTest(
        @SuppressWarnings("unused") String caseName,
        MockHttpServletRequestBuilder request,
        String responseFile
    ) throws Exception {
        String contentAsString = httpOperationWithResult(request, status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent(responseFile)));
    }

    @Nonnull
    private static Stream<Arguments> getTracksSource() {
        return Stream.of(
            Arguments.of("No parameters", get("/admin/track"), "response/admin_all_tracks.json"),
            Arguments.of(
                "By track code",
                get("/admin/track").param("trackCode", "TRACK_CODE_1"),
                "response/admin_tracks_1.json"
            ),
            Arguments.of(
                "By entity id",
                get("/admin/track").param("entityId", "ORDER_1"),
                "response/admin_tracks_12.json"
            ),
            Arguments.of(
                "By DSBS entity id",
                get("/admin/track").param("trackCode", "TRACK_CODE_8"),
                "response/admin_tracks_8.json"
            )
        );
    }

    @Test
    @DisplayName("Получение трека заказа по идентификатору")
    void getOrderTrackById() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/1"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_track_by_id_1.json")));
    }

    @Test
    @DisplayName("Получение трека перемещения по идентификатору")
    void getMovementTrackById() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/7"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_track_by_id_7.json")));
    }

    @Test
    @DisplayName("Не найден трек")
    void getTrackByIdNotFound() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/42"), status().isNotFound());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_track_by_id_not_found.json")));
    }

    @Test
    @DisplayName("Получение чекпоинтов трека заказа")
    void getCheckpointsByOrderTrack() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/1/checkpoints"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_track_1_checkpoints.json")));
    }

    @Test
    @DisplayName("Получение чекпоинтов трека перемещения")
    void getCheckpointsByMovementTrack() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/7/checkpoints"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_track_7_checkpoints.json")));
    }

    @Test
    @DisplayName("Получение чекпоинтов, трек не найден")
    void getCheckpointsByTrackNotFound() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/42/checkpoints"), status().isNotFound());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_track_by_id_not_found.json")));
    }

    @Test
    @DisplayName("Получение чекпоинта заказа по идентификатору")
    void getOrderCheckpointsById() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/checkpoints/101"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_checkpoint_1.json")));
    }

    @Test
    @DisplayName("Получение чекпоинта перемещения по идентификатору")
    void getMovementCheckpointsById() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/checkpoints/103"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_checkpoint_103.json")));
    }

    @Test
    @DisplayName("Чекпоинт не найден")
    void getCheckpointsByIdNotFound() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/checkpoints/42"), status().isNotFound());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_checkpoint_by_id_not_found.json")));
    }

    @Test
    @DisplayName("Получение DTO для создания чекпоинта с неизвестным типом трека")
    void getUnknownCheckpointNewDto() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/1/checkpoints/new"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_checkpoint_new_unknown.json")));
    }

    @Test
    @DisplayName("Получение DTO для создания Delivery чекпоинта заказа")
    void getDeliveryFfOrderCheckpointNewDto() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/2/checkpoints/new"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_checkpoint_new_delivery_order.json")));
    }

    @Test
    @DisplayName("Получение DTO для создания Delivery чекпоинта перемещения")
    void getDeliveryMovementCheckpointNewDto() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/6/checkpoints/new"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_checkpoint_new_delivery_movement.json")));
    }

    @Test
    @DisplayName("Получение DTO для создания fulfillment чекпоинта заказа")
    void getFulfillmentOrderCheckpointNewDto() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/3/checkpoints/new"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_checkpoint_new_fulfillment_order.json")));
    }

    @Test
    @DisplayName("Получение DTO для создания fulfillment чекпоинта заказа для партнёра типа DS")
    void getFulfillmentOrderDsPartnerCheckpointNewDto() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/9/checkpoints/new"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent(
                "response/admin_checkpoint_new_fulfillment_order_ds_partner.json"
            )));
    }

    @Test
    @DisplayName("Получение DTO для создания fulfillment чекпоинта перемещения")
    void getFulfillmentMovementCheckpointNewDto() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/7/checkpoints/new"), status().isOk());

        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_checkpoint_new_fulfillment_movement.json")));
    }

    @Test
    @DisplayName("Создание нового чекпоинта заказа")
    @ExpectedDatabase(
        value = "/database/expected/admin_created_order_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderCheckpoint() throws Exception {
        String contentAsString = httpOperationWithResult(
            post("/admin/track/checkpoints")
                .content(extractFileContent("request/admin_order_checkpoint_track_1.json"))
                .contentType(MediaType.APPLICATION_JSON),
            status().isOk()
        );

        assertions()
            .assertThat(contentAsString)
            .isEqualTo("102");
    }

    @Test
    @DisplayName("Создание нового чекпоинта заказа c заданной датой")
    @ExpectedDatabase(
            value = "/database/expected/admin_created_order_checkpoint_with_date.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderCheckpointWithDate() throws Exception {
        String contentAsString = httpOperationWithResult(
            post("/admin/track/checkpoints")
                .content(extractFileContent("request/admin_order_checkpoint_track_with_date_1.json"))
                .contentType(MediaType.APPLICATION_JSON),
            status().isOk()
        );

        assertions()
            .assertThat(contentAsString)
            .isEqualTo("102");
    }

    @Test
    @DisplayName("Создание нового чекпоинта перемещения")
    @ExpectedDatabase(
        value = "/database/expected/admin_created_movement_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createMovementCheckpoint() throws Exception {
        String contentAsString = httpOperationWithResult(
            post("/admin/track/checkpoints")
                .content(extractFileContent("request/admin_movement_checkpoint_track_7.json"))
                .contentType(MediaType.APPLICATION_JSON),
            status().isOk()
        );

        assertions()
            .assertThat(contentAsString)
            .isEqualTo("103");
    }

    @Test
    @DisplayName("Валидация создания нового чекпоинта")
    void createCheckpointInvalidRequest() throws Exception {
        String contentAsString = httpOperationWithResult(
            post("/admin/track/checkpoints")
                .content(extractFileContent("request/admin_invalid_checkpoint_track_1.json"))
                .contentType(MediaType.APPLICATION_JSON),
            status().isBadRequest()
        );

        String invalidObject = "Field error in object 'adminUnknownCheckpointCreateDto' ";
        assertions()
            .assertThat(contentAsString)
            .contains(invalidObject + "on field 'deliveryCheckpointStatus': rejected value [null]; codes [NotNull")
            .contains(invalidObject + "on field 'message': rejected value []; codes [NotEmpty")
            .contains(invalidObject + "on field 'trackId': rejected value [null]; codes [NotNull");
    }

    /**
     * This test expects {@link ClockTestConfiguration#getClock()} to return a clock at 2018-01-01 15:27:00.0.
     */
    @Test
    @DisplayName("Возобновление удаленного трека")
    @ExpectedDatabase(
        value = "/database/expected/admin_tracks_resume.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void resumeTrack() throws Exception {
        httpOperationWithResult(
            post("/admin/track/start")
                .content(extractFileContent("request/admin_resume_track_1.json"))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    /**
     * This test expects {@link ClockTestConfiguration#getClock()} to return a clock at 2018-01-01 15:27:00.0.
     *
     * Expects no update to the database as the state of the requested track is not DELETED or STOPPED.
     */
    @Test
    @DisplayName("Возобновление неостановленного трека")
    @ExpectedDatabase(
        value = "/database/expected/admin_tracks_no_change.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void resumeTrackThatDoesNotNeedToBeResumed() throws Exception {
        httpOperationWithResult(
            post("/admin/track/start")
                .content(extractFileContent("request/admin_resume_track_2.json"))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    /**
     * This test expects {@link ClockTestConfiguration#getClock()} to return a clock at 2018-01-01 15:27:00.0.
     *
     * Expects:
     * - track 1 to remain the same: it is not STOPPED or DELETED
     * - track 3 to switch status to STARTED and stop_tracking_ts to 2018-01-02
     * - tracks 2, 4, and 5 to remain the same: they are not listed in the request
     */
    @Test
    @DisplayName("Возобновление нескольких треков")
    @ExpectedDatabase(
        value = "/database/expected/admin_tracks_resume_multiple.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void startTrackMultiple() throws Exception {
        httpOperationWithResult(
            post("/admin/track/start-multiple")
                .content(extractFileContent("request/admin_resume_track_multiple.json"))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Выбор DSBS трека по id")
    void getDSBSMovementTrackById() throws Exception {
        String contentAsString = httpOperationWithResult(get("/admin/track/8"), status().isOk());
        assertions()
            .assertThat(contentAsString)
            .is(jsonStrictMatching(extractFileContent("response/admin_track_by_id_8.json")));
    }
}
