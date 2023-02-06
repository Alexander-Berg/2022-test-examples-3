package ru.yandex.market.logistics.lrm.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

@ParametersAreNonnullByDefault
@DisplayName("Получение возвратного сегмента по его идентификатору")
class GetReturnSegmentByIdTest extends AbstractAdminIntegrationTest {

    private static final long ALL_FIELDS_FILLED_ID = 2L;
    private static final long ONLY_REQUIRED_FIELDS_FILLED_ID = 1L;

    private static final String GET_SEGMENT_DETAIL_CARD_PATH = "admin/returns/boxes/segments/%d";
    private static final String GET_SEGMENT_SHIPMENT_PATH = GET_SEGMENT_DETAIL_CARD_PATH + "/shipment";
    private static final String GET_SEGMENT_LOGISTIC_POINT_PATH = GET_SEGMENT_DETAIL_CARD_PATH + "/logistic-point";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Возвратный сегмент с заданным идентификатором не найден")
    void returnSegmentNotFound(
        @SuppressWarnings("unused") String displayName,
        String path
    ) {
        RestAssuredTestUtils.assertNotFoundError(
            RestAssured.get(path.formatted(10000L)),
            "Failed to find RETURN_SEGMENT with ids [10000]"
        );
    }

    @Nonnull
    private static Stream<Arguments> returnSegmentNotFound() {
        return Stream.of(
            Arguments.of(
                "Получение детальной карточки сегмента",
                GET_SEGMENT_DETAIL_CARD_PATH
            ),
            Arguments.of(
                "Получение данных об отгрузке сегмента",
                GET_SEGMENT_SHIPMENT_PATH
            ),
            Arguments.of(
                "Получение данных о логистической точке сегмента",
                GET_SEGMENT_LOGISTIC_POINT_PATH
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/database/admin/get-return-segment-by-id/before/prepare.xml")
    @DisplayName("Получение возвратного сегмента по идентификатору")
    void getReturnSegmentDetailsById(
        @SuppressWarnings("unused") String displayName,
        String path,
        Long segmentId,
        String expectedJsonPath
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.get(path.formatted(segmentId)),
            expectedJsonPath
        );
    }

    @Nonnull
    private static Stream<Arguments> getReturnSegmentDetailsById() {
        return Stream.of(
            Arguments.of(
                "Получение сегмента с заполненными только обязательными полями",
                GET_SEGMENT_DETAIL_CARD_PATH,
                ONLY_REQUIRED_FIELDS_FILLED_ID,
                "json/admin/get-return-segment-by-id/segment/only_required_fields.json"
            ),
            Arguments.of(
                "Получение данных об отгрузке для сегмента с незаполненной отгрузкой",
                GET_SEGMENT_SHIPMENT_PATH,
                ONLY_REQUIRED_FIELDS_FILLED_ID,
                "json/admin/get-return-segment-by-id/shipment/empty_fields.json"
            ),
            Arguments.of(
                "Получение данных о логистической точке для сегмента с незаполненной лог точкой",
                GET_SEGMENT_LOGISTIC_POINT_PATH,
                ONLY_REQUIRED_FIELDS_FILLED_ID,
                "json/admin/get-return-segment-by-id/logistic-point/empty_fields.json"
            ),
            Arguments.of(
                "Получение сегмента со всеми заполненными полями",
                GET_SEGMENT_DETAIL_CARD_PATH,
                ALL_FIELDS_FILLED_ID,
                "json/admin/get-return-segment-by-id/segment/all_fields.json"
            ),
            Arguments.of(
                "Получение данных об отгрузке со всеми заполненными полями",
                GET_SEGMENT_SHIPMENT_PATH,
                ALL_FIELDS_FILLED_ID,
                "json/admin/get-return-segment-by-id/shipment/all_fields.json"
            ),
            Arguments.of(
                "Получение данных о логистической точке со всеми заполненными полями",
                GET_SEGMENT_LOGISTIC_POINT_PATH,
                ALL_FIELDS_FILLED_ID,
                "json/admin/get-return-segment-by-id/logistic-point/all_fields.json"
            )
        );
    }
}
