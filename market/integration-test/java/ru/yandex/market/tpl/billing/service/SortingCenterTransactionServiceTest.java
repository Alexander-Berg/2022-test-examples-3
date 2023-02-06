package ru.yandex.market.tpl.billing.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SortingCenterTransactionServiceTest extends AbstractFunctionalTest {

    @Test
    @DbUnitDataSet(
            before = "/database/service/sortingcentertransaction/before/drop_offs_filtering_setup.csv",
            after = "/database/service/sortingcentertransaction/after/drop_offs_filtering.csv")
    void dropOffsDoBeingFiltered() throws Exception {
        mockMvc.perform(post("/manual/calc-sorting-centers-transactions-for-period")
                .param("fromDate", "2021-05-25")
                .param("toDate", "2021-05-25")
        )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/sortingcentertransaction/before/duplicates_removing_setup.csv",
            after = "/database/service/sortingcentertransaction/after/duplicates_removing.csv")
    void duplicatesDoBeingRemoved() throws Exception {
        mockMvc.perform(post("/manual/calc-sorting-centers-transactions-for-period")
                .param("fromDate", "2021-05-25")
                .param("toDate", "2021-05-25")
        )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/sortingcentertransaction/before/transactions_calculation_setup.csv",
            after = "/database/service/sortingcentertransaction/after/transactions_calculation.csv")
    void transactionsDoBeingCalculatedCorrectly() throws Exception {
        mockMvc.perform(post("/manual/calc-sorting-centers-transactions-for-period")
                .param("fromDate", "2021-05-25")
                .param("toDate", "2021-05-25")
        )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "/database/service/sortingcentertransaction/hibernate_testing.csv")
    void hibernateQueriesAreCorrect() throws Exception {
        mockMvc.perform(post("/manual/calc-sorting-centers-transactions-for-period")
                .param("fromDate", "2021-06-02")
                .param("toDate", "2021-06-02")
        )
                .andExpect(status().isOk());
    }

}
