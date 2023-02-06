package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
class LogisticPointAvailabilityShopSearchTest extends AbstractContextualTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("Поиск настроек доступа магазинов к конфигурации доступности склада")
    void search(
        @SuppressWarnings("unused") String caseName,
        long logisticPointAvailabilityId,
        String expectedResultJsonPath
    ) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
            String.format("/admin/logistic-point-availability/%d/shop", logisticPointAvailabilityId)
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectedResultJsonPath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Поиск по id конфигурации с непустым списком магазинов",
                1,
                "controller/admin/logistic-point-availability/shops/search_result_all_data_found.json"
            ),
            Arguments.of(
                "Поиск по id конфигурации с пустым списком магазинов",
                3,
                "controller/admin/logistic-point-availability/shops/search_result_no_data_found.json"
            )
        );
    }

    @Test
    @DisplayName("Поиск настроек доступа магазинов к несуществующей конфигурации доступности склада")
    void searchLogisticPointAvailabilityNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/logistic-point-availability/10/shop"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY] with ids [10]"));
    }
}
