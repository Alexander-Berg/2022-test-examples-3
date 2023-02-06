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
import ru.yandex.market.logistics.logistics4shops.admin.enums.AdminOrderCheckpointStatus;
import ru.yandex.market.logistics.logistics4shops.admin.model.AdminLostCheckpointFilterDto;
import ru.yandex.market.logistics.test.integration.matchers.JsonMatcher;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/admin/lostCheckpoint/prepare.xml")
@DisplayName("Контроллер для работы с потерянными чп через админку")
@ParametersAreNonnullByDefault
class AdminLostCheckpointTest extends AbstractIntegrationTest {
    private static final String URL = "/admin/lost-checkpoints/";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск чекпоинтов")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminLostCheckpointFilterDto filter,
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
                new AdminLostCheckpointFilterDto().setExternalOrderId("102"),
                "admin/lostCheckpoint/after/102.json"
            ),
            Arguments.of(
                "По статусу",
                new AdminLostCheckpointFilterDto().setStatus(AdminOrderCheckpointStatus.ACCEPTED),
                "admin/lostCheckpoint/after/accepted.json"
            ),
            Arguments.of(
                "По идентификатору заказа и статусу",
                new AdminLostCheckpointFilterDto()
                    .setExternalOrderId("102")
                    .setStatus(AdminOrderCheckpointStatus.ITEMS_REMOVED),
                "admin/lostCheckpoint/after/102_items_removed.json"
            ),
            Arguments.of(
                "Пустой фильтр",
                new AdminLostCheckpointFilterDto(),
                "admin/lostCheckpoint/after/all.json"
            )
        );
    }
}
