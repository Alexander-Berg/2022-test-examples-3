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
import ru.yandex.market.logistics.lom.model.filter.BalancePaymentReportFilter;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тест отчета Движение по ПП и АВ")
class BalancePaymentReportTest extends AbstractContextualTest {

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Успешный поиск данных для отчета")
    @DatabaseSetup("/report/balance_payments_data.xml")
    void successSearch() throws Exception {
        searchBalancePaymentsReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/balance_payments_response_1.json"));
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Успешный поиск данных для отчета с платежом в другом периоде")
    @DatabaseSetup("/report/balance_payments_data.xml")
    void successSearchAdditionalPayment() throws Exception {
        searchBalancePaymentsReportData(validFilterBuilder().balanceOrderIds(Set.of(102L)).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/balance_payments_response_2.json"));
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Заказ с двумя сегментами СД, смотрим на последнюю милю")
    @DatabaseSetup("/report/balance_payments_two_ds_data.xml")
    void successTwoDsPaymentsReportData() throws Exception {
        searchBalancePaymentsReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/balance_payments_response_1.json"));
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Успешный поиск данных для отчета с пустым списком платежей из биллинга")
    @DatabaseSetup("/report/balance_payments_data.xml")
    void successSearchEmptyPayments() throws Exception {
        searchBalancePaymentsReportData(validFilterBuilder().balanceOrderIds(Set.of()).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/balance_payments_response_1.json"));
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Успешный поиск данных для отчета с заказом через СЦ")
    @DatabaseSetup("/report/balance_payments_sorting_center_data.xml")
    void successSearchWithSc() throws Exception {
        searchBalancePaymentsReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/balance_payments_response_1.json"));
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Успешный поиск данных для отчета с незавершенным платежом")
    @DatabaseSetup("/report/balance_payments_no_complete_payment_data.xml")
    void successSearchNoCompletePayment() throws Exception {
        searchBalancePaymentsReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/balance_payments_response_1.json"));
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Успешный поиск данных для отчета с возвратом заказа")
    @DatabaseSetup("/report/balance_payments_returned_data.xml")
    void successSearchReturn() throws Exception {
        searchBalancePaymentsReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/balance_payments_response_returned.json"));
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Успешный поиск данных для отчета c фио получателя")
    @DatabaseSetup("/report/balance_payments_with_recipient.xml")
    void successSearchWithRecipient() throws Exception {
        searchBalancePaymentsReportData(validFilterBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/report/response/balance_payments_with_recipient_response.json"));
    }

    @ParameterizedTest
    @MethodSource("filterArgument")
    @DisplayName("Невалидный фильтр")
    void invalidFilter(BalancePaymentReportFilter filter, String message) throws Exception {
        searchBalancePaymentsReportData(filter)
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
            Pair.of(validFilterBuilder().dateTo(null), "Field: 'dateTo', message: 'must not be null'"),
            Pair.of(
                validFilterBuilder().balanceOrderIds(Sets.newHashSet(null, 1L)),
                "Field: 'balanceOrderIds[]', message: 'must not be null'"
            )
        )
            .map(pair -> Arguments.of(pair.getLeft().build(), pair.getRight()));
    }

    @Nonnull
    private static BalancePaymentReportFilter.BalancePaymentReportFilterBuilder validFilterBuilder() {
        return BalancePaymentReportFilter.builder()
            .senderIds(Set.of(1L))
            .balanceOrderIds(Set.of(1L))
            .dateFrom(ZonedDateTime.of(2019, 12, 3, 0, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE))
            .dateTo(ZonedDateTime.of(2019, 12, 7, 12, 0, 0, 0, DateTimeUtils.MOSCOW_ZONE))
            .platformClientId(3L);
    }

    @Nonnull
    private ResultActions searchBalancePaymentsReportData(BalancePaymentReportFilter filter) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/report/balance-payments-data", filter));
    }
}
