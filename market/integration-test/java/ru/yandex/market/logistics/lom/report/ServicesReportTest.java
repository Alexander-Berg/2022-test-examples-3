package ru.yandex.market.logistics.lom.report;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.filter.ServicesReportFilter;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты подготовки данных для отчета Услуги")
@DatabaseSetup("/billing/before/billing_service_products.xml")
@DatabaseSetup("/report/services_report_data.xml")
class ServicesReportTest extends AbstractContextualTest {

    @Test
    @JpaQueriesCount(6)
    @DisplayName("Успешный поиск данных для отчета")
    void successSearch() throws Exception {
        searchServicesReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/services_report_response_1.json"));
    }

    @Test
    @JpaQueriesCount(6)
    @DisplayName("Проверка фильтра по транзакциям")
    void successSearchTxNotPresent() throws Exception {
        ZonedDateTime dateTo = ZonedDateTime.of(2019, 12, 10, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE);
        searchServicesReportData(validFilterBuilder().dateTo(dateTo).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/services_report_response_2.json"));
    }

    @Test
    @JpaQueriesCount(6)
    @DisplayName("Заказ с двумя сегментами СД, смотрим на последнюю милю")
    @DatabaseSetup("/report/services_report_two_ds_data.xml")
    void successTwoDs() throws Exception {
        searchServicesReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/services_report_response_3.json"));
    }

    @Test
    @JpaQueriesCount(1)
    @DisplayName("Проверка фильтра по транзакциям")
    void successSearchEmpty() throws Exception {
        ZonedDateTime dateFrom = ZonedDateTime.of(2019, 11, 29, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE);
        ZonedDateTime dateTo = ZonedDateTime.of(2019, 12, 1, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE);
        searchServicesReportData(validFilterBuilder().dateFrom(dateFrom).dateTo(dateTo).build())
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @MethodSource("filterArgument")
    @DisplayName("Невалидный фильтр")
    void invalidFilter(ServicesReportFilter filter, String message) throws Exception {
        searchServicesReportData(filter)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Following validation errors occurred:\n" + message));
    }

    @Nonnull
    private static Stream<Arguments> filterArgument() {
        return Stream.of(
            Pair.of(
                validFilterBuilder().senderIds(Set.of()),
                "Field: 'senderIds', message: 'must not be empty'"
            ),
            Pair.of(
                validFilterBuilder().senderIds(Sets.newHashSet(null, 1L)),
                "Field: 'senderIds[]', message: 'must not be null'"
            ),
            Pair.of(
                validFilterBuilder().platformClientId(null),
                "Field: 'platformClientId', message: 'must not be null'"
            ),
            Pair.of(validFilterBuilder().senderIds(null), "Field: 'senderIds', message: 'must not be empty'"),
            Pair.of(validFilterBuilder().dateFrom(null), "Field: 'dateFrom', message: 'must not be null'"),
            Pair.of(validFilterBuilder().dateTo(null), "Field: 'dateTo', message: 'must not be null'")
        )
            .map(pair -> Arguments.of(pair.getLeft().build(), pair.getRight()));
    }

    @Nonnull
    private ResultActions searchServicesReportData(ServicesReportFilter filter) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/report/services-data", filter));
    }

    @Nonnull
    private static ServicesReportFilter.ServicesReportFilterBuilder validFilterBuilder() {
        return ServicesReportFilter.builder()
            .senderIds(Set.of(1L))
            .dateFrom(ZonedDateTime.of(2019, 12, 8, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE))
            .dateTo(ZonedDateTime.of(2019, 12, 12, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE))
            .platformClientId(3L);
    }

}
