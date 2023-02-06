package ru.yandex.market.logistics.lrm.admin.pageable;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

@ParametersAreNonnullByDefault
@DatabaseSetup("/database/admin/pageable/before/prepare.xml")
@DisplayName("Запросы имеют ограничение на максимальное число элементов")
class AdminPageHasLimitedSizeTest extends AbstractIntegrationTest {

    private static final String GET_ROUTES_HISTORY_PATH = "admin/return-routes/search";
    private static final long RETURN_BOX_ID = 1;

    @Test
    @DisplayName("Запросить больше данных, чем установленное ограничение страницы")
    void requestMoreThanPageSize() {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given()
                .params("boxId", RETURN_BOX_ID)
                .get(GET_ROUTES_HISTORY_PATH),
            "json/admin/pageable/10_route_history.json"
        );
    }
}
