package ru.yandex.direct.grid.model.entity.campaign.strategy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetPeriod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тест на формат заполнения объекта budget, который используется фронтом на гридах
 */
public class GdStrategyExtractorHelperTest {

    @Test
    public void calcBudgetForStrategiesWithDailyBudgetOnly_emptySum(){
        StrategyData strategyData = new StrategyData();
        strategyData.setAvgBid(BigDecimal.ONE);
        strategyData.setName("autobudget_avg_click");

        var budget =
                GdStrategyExtractorHelper.calcBudgetForStrategiesWithWeeklyBudgetOnly(strategyData);
        assertNotNull(budget);
        assertEquals(GdCampaignBudgetPeriod.WEEK, budget.getPeriod());
        assertEquals(BigDecimal.ZERO, budget.getSum());
    }

    @Test
    public void calcBudgetForStrategiesWithDailyBudgetOnly_nonEmptySum(){
        var weeklyBudgetSum = BigDecimal.TEN;

        StrategyData strategyData = new StrategyData();
        strategyData.setAvgBid(BigDecimal.ONE);
        strategyData.setName("autobudget_avg_click");
        strategyData.setSum(weeklyBudgetSum);

        var budget =
                GdStrategyExtractorHelper.calcBudgetForStrategiesWithWeeklyBudgetOnly(strategyData);
        assertNotNull(budget);
        assertEquals(GdCampaignBudgetPeriod.WEEK, budget.getPeriod());
        assertEquals(weeklyBudgetSum, budget.getSum());
    }

    @Test
    public void calcBudgetForStrategiesWithDailyBudgetOnly_emptyStrategyData(){
        var budget =
                GdStrategyExtractorHelper.calcBudgetForStrategiesWithWeeklyBudgetOnly(null);
        assertNotNull(budget);
        assertEquals(GdCampaignBudgetPeriod.WEEK, budget.getPeriod());
        assertEquals(BigDecimal.ZERO, budget.getSum());
    }
}
