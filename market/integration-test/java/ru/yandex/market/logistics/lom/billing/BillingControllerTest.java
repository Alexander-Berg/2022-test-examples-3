package ru.yandex.market.logistics.lom.billing;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тест АПИ биллинга")
@DatabaseSetup("/billing/before/billing_service_products.xml")
@DatabaseSetup("/billing/before/transactions.xml")
class BillingControllerTest extends AbstractContextualTest {

    public static final LocalDateTime DATE = LocalDateTime.of(2019, 10, 9, 0, 0);

    @Test
    @DisplayName("Успешный запрос транзакций разного вида")
    void getTransactions() throws Exception {
        performFindTransactions(DATE.plusDays(1L), jsonContent("billing/response/all_transactions.json"));
    }

    @Test
    @DisplayName("Фильтр по признаку billable сущности")
    @DatabaseSetup(
        value = "/billing/before/transactions_billing_disabled.xml",
        type = DatabaseOperation.UPDATE
    )
    void transactionsFilteredByBalanceContractId() throws Exception {
        performFindTransactions(DATE.plusDays(1L), content().json("[]"));
    }

    @Test
    @DisplayName("Успешный запрос транзакций с фильтром по часу")
    void getTransactionsTillEight() throws Exception {
        performFindTransactions(DATE.plusHours(8L), jsonContent("billing/response/till_8_transactions.json"));
    }

    @Test
    @DisplayName("Пустой ответ")
    void getTransactionsEmptyResponse() throws Exception {
        performFindTransactions(DATE.minusHours(8L), content().json("[]"));
    }

    @Test
    @DisplayName("Невалидная дата")
    void getTransactionsInvalidDate() throws Exception {
        mockMvc.perform(get("/billing/transactions")
            .param("from_date", "error_format")
            .param("to_date", DATE.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid value=[error_format] for key=[from_date] specified"));
    }

    @Test
    @DisplayName("Отсутствует дата")
    void getTransactionsNoDate() throws Exception {
        mockMvc.perform(get("/billing/transactions")
            .param("to_date", DATE.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Required LocalDateTime parameter 'from_date' is not present"));
    }

    private void performFindTransactions(LocalDateTime localDateTime, ResultMatcher resultMatcher) throws Exception {
        mockMvc.perform(get("/billing/transactions")
            .param("from_date", DATE.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .param("to_date", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        )
            .andExpect(status().isOk())
            .andExpect(resultMatcher);
    }
}
