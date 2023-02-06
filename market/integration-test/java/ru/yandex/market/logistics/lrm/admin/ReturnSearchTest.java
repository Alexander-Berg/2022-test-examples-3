package ru.yandex.market.logistics.lrm.admin;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.admin.model.enums.AdminDestinationPointType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnSource;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnStatus;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnType;
import ru.yandex.market.logistics.lrm.admin.model.filter.AdminReturnsSearchFilterDto;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Поиск возвратов в админке по фильтру")
@ParametersAreNonnullByDefault
@DatabaseSetup("/database/admin/search-returns/before/setup.xml")
class ReturnSearchTest extends AbstractAdminIntegrationTest {

    private static final String SEARCH_RETURNS_PATH = "/admin/returns";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск возвратов по фильтру")
    void searchReturnsByFilter(
        @SuppressWarnings("unused") String displayName,
        AdminReturnsSearchFilterDto filter,
        String expectedResponseJsonPath,
        @Nullable Set<Long> logisticsPointFromIds,
        @Nullable Set<Long> partnerIds
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured
                .given()
                .params(toParams(filter))
                .get(SEARCH_RETURNS_PATH),
            expectedResponseJsonPath
        );

        verifyLmsGetLogisticsPoints(logisticsPointFromIds);
        verifyLmsGetPartners(partnerIds);
    }

    @Nonnull
    private static Stream<Arguments> searchReturnsByFilter() {
        return Stream.of(
            Arguments.of(
                "Поиск по пустому фильтру",
                filter(),
                "json/admin/search-returns/empty_filter.json",
                Set.of(111L, 222L, 333L, 555L, 666L, 777L),
                Set.of(1111L, 1222L, 1777L)
            ),
            Arguments.of(
                "Поиск по идентификатору возврата",
                filter().setReturnId(2L),
                "json/admin/search-returns/2_return.json",
                Set.of(222L),
                Set.of(1222L)
            ),
            Arguments.of(
                "Поиск по внешнему идентификатору заказа",
                filter().setOrderExternalId("order-external-id-2"),
                "json/admin/search-returns/2_return.json",
                Set.of(222L),
                Set.of(1222L)
            ),
            Arguments.of(
                "Поиск по статусу",
                filter().setReturnStatus(AdminReturnStatus.CREATED),
                "json/admin/search-returns/only_status.json",
                null,
                null
            ),
            Arguments.of(
                "Поиск по источнику возврата",
                filter().setReturnSource(AdminReturnSource.PICKUP_POINT),
                "json/admin/search-returns/only_return_source.json",
                Set.of(111L, 333L, 555L, 777L),
                Set.of(1111L, 1777L)
            ),
            Arguments.of(
                "Поиск по типу возврата FASHION",
                filter().setReturnType(AdminReturnType.FASHION),
                "json/admin/search-returns/return_type_fashion.json",
                Set.of(111L, 222L, 333L, 555L, 777L),
                Set.of(1111L, 1222L, 1777L)
            ),
            Arguments.of(
                "Поиск по типу возврата CLIENT_COURIER",
                filter().setReturnType(AdminReturnType.CLIENT_COURIER),
                "json/admin/search-returns/return_type_client_courier.json",
                null,
                null
            ),
            Arguments.of(
                "Поиск по дате создания возврата",
                filter().setCreated(LocalDate.of(2021, 11, 11)),
                "json/admin/search-returns/by_created.json",
                Set.of(111L),
                Set.of(1111L)
            ),
            Arguments.of(
                "Поиск по признаку полного возврата",
                filter().setFullReturn(true),
                "json/admin/search-returns/full_returns.json",
                Set.of(222L, 555L, 777L),
                Set.of(1222L, 1777L)
            ),
            Arguments.of(
                "Поиск по логистической точке - источнику возврата",
                filter().setLogisticPointFromId(222L),
                "json/admin/search-returns/2_return.json",
                Set.of(222L),
                Set.of(1222L)
            ),
            Arguments.of(
                "Поиск по строке поиска: одному штрихкоду коробки",
                filter().setFullTextSearch("box-external-id-2"),
                "json/admin/search-returns/2_return.json",
                Set.of(222L),
                Set.of(1222L)
            ),
            Arguments.of(
                "Поиск по строке поиска: по нескольким штрихкодам коробок возврата",
                filter().setFullTextSearch("box-external-id-2 box-external-id-box-1-return-2 "),
                "json/admin/search-returns/2_return.json",
                Set.of(222L),
                Set.of(1222L)
            ),
            Arguments.of(
                "Поиск по строке поиска: пустая строка",
                filter().setFullTextSearch("      "),
                "json/admin/search-returns/empty_filter.json",
                Set.of(111L, 222L, 333L, 555L, 666L, 777L),
                Set.of(1111L, 1222L, 1777L)
            ),
            Arguments.of(
                "Поиск по внешнему идентификатору",
                filter().setExternalId("return-ext-id-5"),
                "json/admin/search-returns/only_return_external_id.json",
                Set.of(555L),
                null
            ),
            Arguments.of(
                "Поиск по внешнему идентификатору: пустая строка",
                filter().setExternalId("      "),
                "json/admin/search-returns/empty_filter.json",
                Set.of(111L, 222L, 333L, 555L, 666L, 777L),
                Set.of(1111L, 1222L, 1777L)
            ),
            Arguments.of(
                "Поиск по партнеру точки назначения возврата",
                filter().setDestinationPartnerId(1222L),
                "json/admin/search-returns/2_return.json",
                Set.of(222L),
                Set.of(1222L)
            ),
            Arguments.of(
                "Поиск по типу точки назначения возврата",
                filter().setDestinationType(AdminDestinationPointType.FULFILLMENT),
                "json/admin/search-returns/destination_fulfillment.json",
                Set.of(111L, 333L, 555L),
                Set.of(1111L)
            ),
            Arguments.of(
                "Поиск по всем полям фильтра",
                filter()
                    .setExternalId("return-ext-id-7")
                    .setOrderExternalId("order-external-id-7")
                    .setReturnStatus(AdminReturnStatus.EXPIRED)
                    .setReturnSource(AdminReturnSource.PICKUP_POINT)
                    .setReturnType(AdminReturnType.FASHION)
                    .setFullReturn(true)
                    .setCreated(LocalDate.of(2021, 11, 12))
                    .setLogisticPointFromId(777L)
                    .setDestinationPartnerId(1777L)
                    .setDestinationType(AdminDestinationPointType.SHOP),
                "json/admin/search-returns/all_filter_conditions.json",
                Set.of(777L),
                Set.of(1777L)
            )
        );
    }

    @Nonnull
    private static AdminReturnsSearchFilterDto filter() {
        return new AdminReturnsSearchFilterDto();
    }

    @Test
    @DisplayName("При ограниченном размере списка")
    void checkTotalCount() {
        RestAssuredTestUtils.assertJsonParameter(
            RestAssured
                .given()
                .params(Map.of("size", "1"))
                .get(SEARCH_RETURNS_PATH),
            "totalCount",
            7
        );

        verifyLmsGetLogisticsPoints(Set.of(777L));
        verifyLmsGetPartners(Set.of(1777L));
    }
}
