package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.utils.JsonUtils;

@Component
public class StrategyReaderFactory {

    public StrategyReader getStrategyReader(DbStrategy dbStrategy) {
        if (!dbStrategy.isAutoBudget()) {
            return new ManualStrategyReader();
        }

        if (dbStrategy.getStrategyName() == StrategyName.AUTOBUDGET_AVG_CLICK) {
            return new AverageBidStrategyReader();
        }

        if (dbStrategy.getStrategyName() == StrategyName.AUTOBUDGET_AVG_CPA) {
            return new AverageCpaStrategyReader();
        }

        if (dbStrategy.getStrategyName() == StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP) {
            return new AverageCpaPerCampStrategyReader();
        }

        if (dbStrategy.getStrategyName() == StrategyName.AUTOBUDGET_MAX_REACH) {
            return new AutobudgetStrategyReader();
        }

        if (dbStrategy.getStrategyName() == StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD) {
            return new AutobudgetCustomPeriodStrategyReader();
        }

        if (dbStrategy.getStrategyName() == StrategyName.AUTOBUDGET_MAX_IMPRESSIONS) {
            return new AutobudgetStrategyReader();
        }

        if (dbStrategy.getStrategyName() == StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD) {
            return new AutobudgetCustomPeriodStrategyReader();
        }

        throw new IllegalArgumentException("can not detect strategy type by database representation " +
                JsonUtils.toJson(dbStrategy));
    }
}
