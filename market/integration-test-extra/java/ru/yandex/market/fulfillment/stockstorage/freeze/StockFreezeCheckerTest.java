package ru.yandex.market.fulfillment.stockstorage.freeze;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.service.health.StockFreezeChecker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тест для {@link StockFreezeChecker}.
 */
public class StockFreezeCheckerTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup("classpath:database/states/freeze_consistency_monitoring_state.xml")
    public void checkFreezeConsistency() throws Exception {
        mockMvc.perform(get("/health/stockFreeze"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("0;OK"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/freeze_inconsistency_monitoring_state.xml")
    public void checkFreezeInconsistency() throws Exception {
        mockMvc.perform(get("/health/stockFreeze"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("2;Found freeze inconsistency for: " +
                        "[StockFreezeInconsistency{stockId=10007, skuId=10001, sumOfFreeze=0, amountAtStock=10}, " +
                        "StockFreezeInconsistency{stockId=10005, skuId=10001, sumOfFreeze=200, amountAtStock=500}]"));
    }
}
