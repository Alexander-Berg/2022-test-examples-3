package ru.yandex.market.logistics.lom.report;

import java.time.LocalDate;
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

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.filter.WithdrawReportFilter;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты подготовки данных для отчета Заборы")
@DatabaseSetup("/billing/before/billing_service_products.xml")
public class WithdrawReportTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешный поиск данных для отчета")
    @DatabaseSetup("/report/withdraw_report_data.xml")
    public void successSearch() throws Exception {
        searchWithdrawsReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/withdraws_report_response_1.json"));
    }

    @Test
    @DisplayName("Успешный поиск данных для отчета с корректировкой в нужном периоде")
    @DatabaseSetup("/report/withdraw_report_correction_another_period.xml")
    public void successSearchWithCorrection() throws Exception {
        searchWithdrawsReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/withdraws_report_response_2.json"));
    }

    @Test
    @DisplayName("Запрос отчета по датам, в которые не было транзакций")
    @DatabaseSetup("/report/withdraw_report_correction_another_period.xml")
    public void successSearchNoTxInPeriod() throws Exception {
        searchWithdrawsReportData(
            validFilterBuilder()
                .dateFrom(LocalDate.of(2019, 12, 8))
                .dateTo(LocalDate.of(2019, 12, 9))
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @MethodSource("filterArgument")
    @DisplayName("Невалидный фильтр")
    public void invalidFilter(WithdrawReportFilter filter, String message) throws Exception {
        searchWithdrawsReportData(filter)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Following validation errors occurred:\n" + message));
    }

    @Nonnull
    private static Stream<Arguments> filterArgument() {
        return Stream.of(
            Pair.of(
                validFilterBuilder().marketIds(Set.of()),
                "Field: 'marketIds', message: 'must not be empty'"
            ),
            Pair.of(
                validFilterBuilder().marketIds(null),
                "Field: 'marketIds', message: 'must not be empty'"
            ),
            Pair.of(
                validFilterBuilder().marketIds(Sets.newHashSet(1L, null)),
                "Field: 'marketIds[]', message: 'must not be null'"
            ),
            Pair.of(validFilterBuilder().dateFrom(null), "Field: 'dateFrom', message: 'must not be null'"),
            Pair.of(validFilterBuilder().dateTo(null), "Field: 'dateTo', message: 'must not be null'"),
            Pair.of(
                validFilterBuilder().platformClientId(null),
                "Field: 'platformClientId', message: 'must not be null'"
            )
        )
            .map(pair -> Arguments.of(pair.getLeft().build(), pair.getRight()));
    }

    @Nonnull
    private static WithdrawReportFilter.WithdrawReportFilterBuilder validFilterBuilder() {
        return WithdrawReportFilter.builder()
            .marketIds(Set.of(1L))
            .dateFrom(LocalDate.of(2019, 12, 10))
            .dateTo(LocalDate.of(2019, 12, 12))
            .platformClientId(3L);
    }

    @Nonnull
    private ResultActions searchWithdrawsReportData(WithdrawReportFilter filter) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/report/withdraws-data", filter));
    }
}
