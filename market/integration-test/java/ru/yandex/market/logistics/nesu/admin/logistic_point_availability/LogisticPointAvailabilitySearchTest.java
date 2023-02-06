package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopRole;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopShipmentType;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminLogisticPointAvailabilityFilter;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminLogisticPointAvailabilityFilter.AdminLogisticPointAvailabilityFilterBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
class LogisticPointAvailabilitySearchTest extends BaseLogisticPointAvailabilityTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("Поиск конфигураций доступности складов для магазинов")
    void search(
        @SuppressWarnings("unused") String caseName,
        AdminLogisticPointAvailabilityFilterBuilder builder,
        String expectedResultJsonPath,
        Set<Long> logisticPointsIds,
        Set<Long> partnerIds
    ) throws Exception {
        mockMvc.perform(get("/admin/logistic-point-availability").params(toParams(builder.build())))
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectedResultJsonPath));

        verifyGetLogisticPoints(logisticPointsIds);
        verifyGetPartners(partnerIds);
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        Set<Long> logisticPoint1Id = Set.of(LOGISTIC_POINT_1_ID);
        Set<Long> logisticPoint1PartnerId = Set.of(LOGISTIC_POINT_1_PARTNER_ID);

        return Stream.of(
            Arguments.of(
                "Поиск без указания фильтров",
                AdminLogisticPointAvailabilityFilter.builder(),
                "controller/admin/logistic-point-availability/search_result_all_data_found.json",
                Set.of(LOGISTIC_POINT_1_ID, LOGISTIC_POINT_2_ID),
                Set.of(LOGISTIC_POINT_1_PARTNER_ID, LOGISTIC_POINT_2_PARTNER_ID)
            ),
            Arguments.of(
                "Поиск с фильтром по ID логистической точки",
                AdminLogisticPointAvailabilityFilter.builder().logisticPointId(LOGISTIC_POINT_1_ID),
                "controller/admin/logistic-point-availability/search_result_first_third_records_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по ID партнёра логистической точки",
                AdminLogisticPointAvailabilityFilter.builder().partnerId(LOGISTIC_POINT_1_PARTNER_ID),
                "controller/admin/logistic-point-availability/search_result_first_third_records_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по ID локации",
                AdminLogisticPointAvailabilityFilter.builder().locationId(21651),
                "controller/admin/logistic-point-availability/search_result_first_record_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по типу отгрузки",
                AdminLogisticPointAvailabilityFilter.builder().shipmentType(AdminShopShipmentType.WITHDRAW),
                "controller/admin/logistic-point-availability/search_result_first_record_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по типу магазина-партнёра",
                AdminLogisticPointAvailabilityFilter.builder().shopRole(AdminShopRole.DROPSHIP),
                "controller/admin/logistic-point-availability/search_result_first_record_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по максимальному количеству магазинов",
                AdminLogisticPointAvailabilityFilter.builder().partnerLimit(10),
                "controller/admin/logistic-point-availability/search_result_first_record_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по текущему количеству магазинов",
                AdminLogisticPointAvailabilityFilter.builder().partnerCount(1),
                "controller/admin/logistic-point-availability/search_result_first_record_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по доступности",
                AdminLogisticPointAvailabilityFilter.builder().enabled(true),
                "controller/admin/logistic-point-availability/search_result_first_record_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по типу отгрузки и ID локации",
                AdminLogisticPointAvailabilityFilter.builder()
                    .locationId(21651)
                    .shipmentType(AdminShopShipmentType.WITHDRAW),
                "controller/admin/logistic-point-availability/search_result_first_record_found.json",
                logisticPoint1Id,
                logisticPoint1PartnerId
            ),
            Arguments.of(
                "Поиск с фильтром по типу магазина-партнёра и доступности",
                AdminLogisticPointAvailabilityFilter.builder()
                    .shopRole(AdminShopRole.SUPPLIER)
                    .enabled(true),
                "controller/admin/logistic-point-availability/search_result_no_data_found.json",
                Set.of(),
                Set.of()
            ),
            Arguments.of(
                "Поиск с фильтром по ID магазина",
                AdminLogisticPointAvailabilityFilter.builder()
                    .shopIds(Set.of(12L)),
                "controller/admin/logistic-point-availability/search_result_second_record_found.json",
                Set.of(LOGISTIC_POINT_2_ID),
                Set.of(LOGISTIC_POINT_2_PARTNER_ID)
            )
        );
    }
}
