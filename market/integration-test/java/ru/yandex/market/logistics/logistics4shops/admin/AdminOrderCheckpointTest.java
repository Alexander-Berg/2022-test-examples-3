package ru.yandex.market.logistics.logistics4shops.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.admin.model.AdminOrderCheckpointFilterDto;
import ru.yandex.market.logistics.test.integration.matchers.JsonMatcher;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/admin/orderCheckpoint/prepare.xml")
@DisplayName("Контроллер для работы с чекпоинтами заказа через админку")
@ParametersAreNonnullByDefault
class AdminOrderCheckpointTest extends AbstractIntegrationTest {
    private static final String URL = "/admin/order-checkpoints/";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск чекпоинтов")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminOrderCheckpointFilterDto filter,
        String expectedResponseJsonPath
    ) {
        RestAssured
            .given()
            .params(toParams(filter))
            .get(URL)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body(new JsonMatcher(extractFileContent(expectedResponseJsonPath)));
    }

    @Nonnull
    private static Stream<Arguments> search() {
        return Stream.of(
            Arguments.of(
                "По идентификатору заказа",
                new AdminOrderCheckpointFilterDto().setOrderId(1L),
                "admin/orderCheckpoint/response/search_1.json"
            ),
            Arguments.of(
                "По пустому фильтру",
                new AdminOrderCheckpointFilterDto(),
                "admin/orderCheckpoint/response/search_all.json"
            )
        );
    }
}
