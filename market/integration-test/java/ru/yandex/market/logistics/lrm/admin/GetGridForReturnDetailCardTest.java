package ru.yandex.market.logistics.lrm.admin;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

@ParametersAreNonnullByDefault
@DisplayName("Получение гридов для детальной карточки возврата")
@DatabaseSetup("/database/admin/get-grid-for-return-detail/before/prepare.xml")
class GetGridForReturnDetailCardTest extends AbstractAdminIntegrationTest {

    private static final long RETURN_WITH_ALL_DATA = 1L;
    private static final long RETURN_WITH_NO_DATA = 2L;

    private static final String GET_BOXES_PATH = "/admin/returns/boxes";
    private static final String GET_ITEMS_PATH = "/admin/returns/items";
    private static final String GET_STATUS_HISTORY_PATH = "/admin/returns/status-history";
    private static final String GET_CONTROL_POINTS_PATH = "/admin/returns/control-points";
    private static final String GET_BUSINESS_PROCESSES_PATH = "/admin/business-process-states/";
    private static final String GET_ORDER_ITEMS_INFO_PATH = "/admin/returns/order-items-info";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успешное получение грида для детальной карточки")
    void getGridForReturnDetailCard(
        @SuppressWarnings("unused") String displayName,
        String path,
        Long returnId,
        String expectedJson,
        @Nullable Set<Long> logisticsPointIds,
        @Nullable Set<Long> partnerIds
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given()
                .params("returnId", returnId)
                .get(path),
            expectedJson
        );

        verifyLmsGetLogisticsPoints(logisticsPointIds);
        verifyLmsGetPartners(partnerIds);
    }

    @Nonnull
    private static Stream<Arguments> getGridForReturnDetailCard() {
        return Stream.of(
            Arguments.of(
                "Получение грузомест: у возврата есть грузоместа",
                GET_BOXES_PATH,
                RETURN_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-detail/has_boxes.json",
                Set.of(123L),
                null
            ),
            Arguments.of(
                "Получение грузомест: у возврата нет грузомест",
                GET_BOXES_PATH,
                RETURN_WITH_NO_DATA,
                "json/admin/get-grid-for-return-detail/no_boxes.json",
                null,
                null
            ),
            Arguments.of(
                "Получение товаров: у возврата есть товары",
                GET_ITEMS_PATH,
                RETURN_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-detail/has_items.json",
                null,
                null
            ),
            Arguments.of(
                "Получение товаров: у возврата нет товаров",
                GET_ITEMS_PATH,
                RETURN_WITH_NO_DATA,
                "json/admin/get-grid-for-return-detail/no_items.json",
                null,
                null
            ),
            Arguments.of(
                "Получение фоновых процессов: у возврата есть процессы",
                GET_BUSINESS_PROCESSES_PATH,
                RETURN_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-detail/has_business_processes.json",
                null,
                null
            ),
            Arguments.of(
                "Получение фоновых процессов: у возврата нет процессов",
                GET_BUSINESS_PROCESSES_PATH,
                RETURN_WITH_NO_DATA,
                "json/admin/get-grid-for-return-detail/no_business_processes.json",
                null,
                null
            ),
            Arguments.of(
                "Получение фоновых процессов: возврата не существует",
                GET_BUSINESS_PROCESSES_PATH,
                1234567L,
                "json/admin/get-grid-for-return-detail/no_business_processes.json",
                null,
                null
            ),
            Arguments.of(
                "Получение истории статусов: у возврата есть история статусов",
                GET_STATUS_HISTORY_PATH,
                RETURN_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-detail/has_status_history.json",
                null,
                null
            ),
            Arguments.of(
                "Получение истории статусов: у возврата нет истории статусов",
                GET_STATUS_HISTORY_PATH,
                RETURN_WITH_NO_DATA,
                "json/admin/get-grid-for-return-detail/no_status_history.json",
                null,
                null
            ),
            Arguments.of(
                "Получение контрольных точек: у возврата есть контрольные точки",
                GET_CONTROL_POINTS_PATH,
                RETURN_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-detail/has_control_points.json",
                Set.of(200L),
                Set.of(300L, 400L)
            ),
            Arguments.of(
                "Получение контрольных точек: у возврата нет контрольных точек",
                GET_CONTROL_POINTS_PATH,
                RETURN_WITH_NO_DATA,
                "json/admin/get-grid-for-return-detail/no_control_points.json",
                null,
                null
            ),
            Arguments.of(
                "Получение доп информации товаров: у возврата есть доп информация о товарах",
                GET_ORDER_ITEMS_INFO_PATH,
                RETURN_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-detail/has_order_items_info.json",
                null,
                null
            ),
            Arguments.of(
                "Получение доп информации товаров: у возврата нет доп информации о товарах",
                GET_ORDER_ITEMS_INFO_PATH,
                RETURN_WITH_NO_DATA,
                "json/admin/get-grid-for-return-detail/no_order_items_info.json",
                null,
                null
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение грида для несуществующего возврата")
    void getGridForNonExistingReturn(
        @SuppressWarnings("unused") String displayName,
        String path
    ) {
        RestAssuredTestUtils.assertNotFoundError(
            RestAssured.given()
                .params("returnId", 123456789L)
                .get(path),
            "Failed to find RETURN with ids [123456789]"
        );
    }

    @Nonnull
    private static Stream<Arguments> getGridForNonExistingReturn() {
        return Stream.of(
            Arguments.of(
                "Получение коробок для несуществующего возврата",
                GET_BOXES_PATH
            ),
            Arguments.of(
                "Получение товаров для несуществующего возврата",
                GET_ITEMS_PATH
            ),
            Arguments.of(
                "Получение истории статусов возврата для несуществующего возврата",
                GET_STATUS_HISTORY_PATH
            ),
            Arguments.of(
                "Получение дополнительной информации о товарах возврата для несуществующего возврата",
                GET_ORDER_ITEMS_INFO_PATH
            )
        );
    }
}
