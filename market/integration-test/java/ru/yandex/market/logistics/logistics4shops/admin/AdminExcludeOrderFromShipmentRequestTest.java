package ru.yandex.market.logistics.logistics4shops.admin;

import java.time.LocalDate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.admin.enums.AdminExcludeOrderFromShipmentRequestStatus;
import ru.yandex.market.logistics.logistics4shops.admin.model.AdminExcludeOrderFromShipmentRequestFilterDto;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/admin/excludeRequest/prepare.xml")
@DisplayName("Контроллер для работы с запросами на перенос заказа из отгрузки через админку")
@ParametersAreNonnullByDefault
class AdminExcludeOrderFromShipmentRequestTest extends AbstractIntegrationTest {
    private static final String URL = "/admin/exclude-order-from-shipment/";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск заявок")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminExcludeOrderFromShipmentRequestFilterDto filter,
        String expectedResponseJsonPath
    ) {
        RestAssuredFactory.assertGetSuccess(URL, expectedResponseJsonPath, toParams(filter));
    }

    @Nonnull
    private static Stream<Arguments> search() {
        return Stream.of(
            Arguments.of(
                "По идентификатору заявки",
                new AdminExcludeOrderFromShipmentRequestFilterDto().setRequestId(1L),
                "admin/excludeRequest/response/1.json"
            ),
            Arguments.of(
                "По идентификатору заказа",
                new AdminExcludeOrderFromShipmentRequestFilterDto().setOrder(101L),
                "admin/excludeRequest/response/2.json"
            ),
            Arguments.of(
                "По идентификатору отгрузки",
                new AdminExcludeOrderFromShipmentRequestFilterDto().setShipment(1001L),
                "admin/excludeRequest/response/4.json"
            ),
            Arguments.of(
                "По статусу",
                new AdminExcludeOrderFromShipmentRequestFilterDto()
                    .setStatus(AdminExcludeOrderFromShipmentRequestStatus.CREATED),
                "admin/excludeRequest/response/5_1.json"
            ),
            Arguments.of(
                "По дате создания от",
                new AdminExcludeOrderFromShipmentRequestFilterDto().setCreatedFrom(LocalDate.of(2022, 1, 2)),
                "admin/excludeRequest/response/5_4_3_1.json"
            ),
            Arguments.of(
                "По дате создания до",
                new AdminExcludeOrderFromShipmentRequestFilterDto().setCreatedTo(LocalDate.of(2022, 1, 2)),
                "admin/excludeRequest/response/1_2.json"
            ),
            Arguments.of(
                "По всем полям",
                new AdminExcludeOrderFromShipmentRequestFilterDto()
                    .setRequestId(3L)
                    .setOrder(102L)
                    .setShipment(1000L)
                    .setStatus(AdminExcludeOrderFromShipmentRequestStatus.SUCCESS)
                    .setCreatedTo(LocalDate.of(2022, 1, 3))
                    .setCreatedFrom(LocalDate.of(2022, 1, 3)),
                "admin/excludeRequest/response/3.json"
            ),
            Arguments.of(
                "По пустому фильтру",
                new AdminExcludeOrderFromShipmentRequestFilterDto(),
                "admin/excludeRequest/response/all.json"
            )
        );
    }
}
