package ru.yandex.market.logistics.logistics4shops.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.admin.model.AdminOrderBoxFilterDto;
import ru.yandex.market.logistics.test.integration.matchers.JsonMatcher;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/admin/orderBoxes/prepare.xml")
@DisplayName("Контроллер для работы с коробками заказа через админку")
@ParametersAreNonnullByDefault
class AdminOrderBoxTest extends AbstractIntegrationTest {
    private static final String URL = "/admin/boxes/";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск коробок")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminOrderBoxFilterDto filter,
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
                "По идентификатору",
                new AdminOrderBoxFilterDto().setBoxId(1000L),
                "admin/orderBoxes/response/1000.json"
            ),
            Arguments.of(
                "По шрихкоду",
                new AdminOrderBoxFilterDto().setBarcode("100-2"),
                "admin/orderBoxes/response/1001.json"
            ),
            Arguments.of(
                "По пустому шрихкоду",
                new AdminOrderBoxFilterDto().setBarcode("     "),
                "admin/orderBoxes/response/empty.json"
            ),
            Arguments.of(
                "По идентификатору заказа",
                new AdminOrderBoxFilterDto().setOrderId(100L),
                "admin/orderBoxes/response/1002_1001_1000.json"
            ),
            Arguments.of(
                "По пустому фильтру",
                new AdminOrderBoxFilterDto(),
                "admin/orderBoxes/response/all.json"
            ),
            Arguments.of(
                "По всем полям",
                new AdminOrderBoxFilterDto()
                    .setBoxId(1100L)
                    .setBarcode("101-1")
                    .setOrderId(101L),
                "admin/orderBoxes/response/1100.json"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получить карточку коробки")
    void get(@SuppressWarnings("unused") String displayName, long boxId, String responsePath) {
        RestAssured
            .get(URL + boxId)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body(new JsonMatcher(extractFileContent(responsePath)));
    }

    @Test
    @DisplayName("Получить несуществующую коробку")
    void getFail() {
        RestAssured
            .get(URL + "100")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", equalTo("Failed to find [ORDER_BOX] with id [100]"));
    }

    @Nonnull
    private static Stream<Arguments> get() {
        return Stream.of(
            Arguments.of("Нет товаров", 1100, "admin/orderBoxes/response/no_items.json"),
            Arguments.of("Есть товары", 1000, "admin/orderBoxes/response/with_items.json")
        );
    }
}
