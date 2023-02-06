package ru.yandex.market.logistics.logistics4shops.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.admin.model.AdminOrderFilterDto;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/admin/order/prepare.xml")
@DisplayName("Контроллер для работы с заказами через админку")
@ParametersAreNonnullByDefault
class AdminOrderTest extends AbstractIntegrationTest {

    private static final String URL = "/admin/external-orders/";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск заказов")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminOrderFilterDto filter,
        String expectedResponseJsonPath
    ) {
        RestAssuredFactory.assertGetSuccess(URL, expectedResponseJsonPath, toParams(filter));
    }

    @ValueSource(strings = {"partner", "shop"})
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск заказов по пустым коллекциям")
    void searchEmptyCollections(String collectionName) {
        RestAssuredFactory.assertGetSuccess(URL, "admin/order/response/empty.json", collectionName, "");
    }

    @Test
    @DisplayName("Получить карточку заказа")
    void get() {
        RestAssuredFactory.assertGetSuccess(URL + "1", "admin/order/response/detail.json");
    }

    @Test
    @DisplayName("Получить несуществующий заказ")
    void getFail() {
        RestAssuredFactory.assertGetNotFound(URL + "100", "Failed to find [ORDER] with id [100]");
    }

    @Nonnull
    private static Stream<Arguments> search() {
        return Stream.of(
            Arguments.of(
                "По идентификатору заказа",
                new AdminOrderFilterDto().setOrderId(1L),
                "admin/order/response/1.json"
            ),
            Arguments.of(
                "По внешнему идентификатору заказа",
                new AdminOrderFilterDto().setExternalId("102"),
                "admin/order/response/2.json"
            ),
            Arguments.of(
                "По пустому внешнему идентификатору заказа",
                new AdminOrderFilterDto().setExternalId("  "),
                "admin/order/response/empty.json"
            ),
            Arguments.of(
                "По идентификатору магазина",
                new AdminOrderFilterDto().setShop(List.of(200L)),
                "admin/order/response/1_2.json"
            ),
            Arguments.of(
                "По нескольким идентификаторам магазинов",
                new AdminOrderFilterDto().setShop(List.of(200L, 1000L)),
                "admin/order/response/1_2_5.json"
            ),
            Arguments.of(
                "По идентификатору партнера",
                new AdminOrderFilterDto().setPartner(List.of(400L)),
                "admin/order/response/4_3_2.json"
            ),
            Arguments.of(
                "По нескольким идентификаторам партнеров",
                new AdminOrderFilterDto().setPartner(List.of(400L, 2000L)),
                "admin/order/response/5_4_3_2.json"
            ),
            Arguments.of(
                "По дате создания от",
                new AdminOrderFilterDto().setCreatedFrom(LocalDate.of(2022, 1, 2)),
                "admin/order/response/5_4_3_1.json"
            ),
            Arguments.of(
                "По дате создания до",
                new AdminOrderFilterDto().setCreatedTo(LocalDate.of(2022, 1, 2)),
                "admin/order/response/1_2.json"
            ),
            Arguments.of(
                "По всем полям",
                new AdminOrderFilterDto()
                    .setShop(List.of(200L))
                    .setPartner(List.of(300L))
                    .setOrderId(1L)
                    .setExternalId("101")
                    .setCreatedTo(LocalDate.of(2022, 1, 2))
                    .setCreatedFrom(LocalDate.of(2022, 1, 2)),
                "admin/order/response/1.json"
            ),
            Arguments.of(
                "По пустому фильтру",
                new AdminOrderFilterDto(),
                "admin/order/response/all.json"
            )
        );
    }
}
