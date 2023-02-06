package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsDayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaPerCampStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

import static ru.yandex.direct.core.testing.steps.campaign.repository0.strategy.WriterUtils.checkArgument;

public class AverageCpaPerCampStrategyWriter implements StrategyWriter {

    @Override
    public DbStrategy write(Strategy strategy) {
        checkArgument(strategy, AverageCpaPerCampStrategy.class);
        AverageCpaPerCampStrategy aStrategy = (AverageCpaPerCampStrategy) strategy;

        return (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
                .withPlatform(CampaignsPlatform.CONTEXT)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetDailyChangeCount(0L)
                .withDayBudgetStopTime(LocalDateTime.now())
                .withDayBudgetShowMode(CampaignsDayBudgetShowMode.DEFAULT_)
                .withStrategyData(new StrategyData()
                        .withAvgCpa(BigDecimal.valueOf(aStrategy.getAverageCpa()))
                        .withGoalId(aStrategy.getGoalId())
                        .withSum(BigDecimal.valueOf(aStrategy.getMaxWeekSum()))
                        .withBid(BigDecimal.valueOf(aStrategy.getMaxBid()))
                        .withUnknownFields(aStrategy.getUnknownFields())
                        .withVersion(VERSION));
    }
}
