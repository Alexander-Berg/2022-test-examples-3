package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
@DatabaseSetup("/repository/logistic-point-availability/before/schedule_prepare_data.xml")
class LogisticPointAvailabilityScheduleSearchTest extends AbstractContextualTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("Поиск слотов отгрузки конфигурации доступности склада для магазинов")
    void search(
        @SuppressWarnings("unused") String caseName,
        Map<String, String> requestParameters,
        String expectedResultJsonPath
    ) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/logistic-point-availability/schedule")
                .params(toParams(requestParameters))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectedResultJsonPath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Поиск по id конфигурации с непустым расписанием",
                Map.of("logisticPointAvailabilityId", "1"),
                "controller/admin/logistic-point-availability/schedule/search_result_all_data_found.json"
            ),
            Arguments.of(
                "Поиск по id конфигурации с пустым расписанием",
                Map.of("logisticPointAvailabilityId", "2"),
                "controller/admin/logistic-point-availability/schedule/search_result_no_data_found.json"
            ),
            Arguments.of(
                "Поиск без указания id конфигурации",
                null,
                "controller/admin/logistic-point-availability/schedule/search_result_no_data_found.json"
            )
        );
    }
}
