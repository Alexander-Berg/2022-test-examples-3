package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsDayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

import static ru.yandex.direct.core.testing.steps.campaign.repository0.strategy.WriterUtils.checkArgument;

public class AverageCpaStrategyWriter implements StrategyWriter {

    @Override
    public DbStrategy write(Strategy strategy) {
        checkArgument(strategy, AverageCpaStrategy.class);
        AverageCpaStrategy averageCpaStrategy = (AverageCpaStrategy) strategy;

        return (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA)
                .withPlatform(CampaignsPlatform.CONTEXT)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetDailyChangeCount(0L)
                .withDayBudgetStopTime(LocalDateTime.now())
                .withDayBudgetShowMode(CampaignsDayBudgetShowMode.DEFAULT_)
                .withStrategyData(new StrategyData()
                        .withAvgCpa(averageCpaStrategy.getAverageCpa())
                        .withGoalId(averageCpaStrategy.getGoalId())
                        .withSum(averageCpaStrategy.getMaxWeekSum())
                        .withBid(averageCpaStrategy.getMaxBid())
                        .withUnknownFields(averageCpaStrategy.getUnknownFields())
                        .withVersion(VERSION));
    }
}
