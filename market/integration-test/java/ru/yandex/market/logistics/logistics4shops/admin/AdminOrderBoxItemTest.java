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
import ru.yandex.market.logistics.logistics4shops.admin.model.AdminOrderBoxItemFilterDto;
import ru.yandex.market.logistics.test.integration.matchers.JsonMatcher;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/admin/orderItems/prepare.xml")
@DisplayName("Контроллер для работы с товарами через админку")
@ParametersAreNonnullByDefault
class AdminOrderBoxItemTest extends AbstractIntegrationTest {
    private static final String URL = "/admin/box-items/";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск товаров")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminOrderBoxItemFilterDto filter,
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
                new AdminOrderBoxItemFilterDto().setItemId(10001L),
                "admin/orderItems/response/10001.json"
            ),
            Arguments.of(
                "По внешнему идентификатору",
                new AdminOrderBoxItemFilterDto().setExternalItemId(123452L),
                "admin/orderItems/response/10002.json"
            ),
            Arguments.of(
                "По идентификатору коробки",
                new AdminOrderBoxItemFilterDto().setBoxId(1100L),
                "admin/orderItems/response/10003_10004.json"
            ),
            Arguments.of(
                "По всем параметрам",
                new AdminOrderBoxItemFilterDto().setBoxId(1100L).setItemId(10004L).setExternalItemId(153453L),
                "admin/orderItems/response/10004.json"
            ),
            Arguments.of(
                "По пустому фильтру",
                new AdminOrderBoxItemFilterDto(),
                "admin/orderItems/response/all.json"
            ),
            Arguments.of(
                "По коробке без товаров",
                new AdminOrderBoxItemFilterDto().setBoxId(1001L),
                "admin/orderItems/response/empty.json"
            )
        );
    }
}
