package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import java.math.BigDecimal;

import ru.yandex.direct.core.entity.campaign.model.CampOptionsDayBudgetNotificationStatus;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsDayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageBidStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

import static ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignMappings.dayBudgetShowModeToDb;
import static ru.yandex.direct.core.testing.steps.campaign.repository0.strategy.WriterUtils.checkArgument;

public class AverageBidStrategyWriter implements StrategyWriter {

    @Override
    public DbStrategy write(Strategy strategy) {
        checkArgument(strategy, AverageBidStrategy.class);
        AverageBidStrategy averageBidStrategy = (AverageBidStrategy) strategy;
        DbStrategy dbStrategy = (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK)
                .withStrategyData(new StrategyData()
                        .withAvgBid(averageBidStrategy.getAverageBid())
                        .withSum(averageBidStrategy.getMaxWeekSum())
                        .withUnknownFields(averageBidStrategy.getUnknownFields())
                        .withVersion(VERSION));

        if (averageBidStrategy.getDayBudget() != null) {
            DayBudget dayBudget = averageBidStrategy.getDayBudget();

            dbStrategy.withDayBudget(dayBudget.getDayBudget())
                    .withDayBudgetShowMode(dayBudgetShowModeToDb(dayBudget.getShowMode()))
                    .withDayBudgetDailyChangeCount(dayBudget.getDailyChangeCount())
                    .withDayBudgetStopTime(dayBudget.getStopTime());

            if (dayBudget.getStopNotificationSent() != null) {
                dbStrategy.withDayBudgetNotificationStatus(
                        dayBudget.getStopNotificationSent() ?
                                CampOptionsDayBudgetNotificationStatus.SENT:
                                CampOptionsDayBudgetNotificationStatus.READY);
            } else {
                dbStrategy.withDayBudgetNotificationStatus(CampOptionsDayBudgetNotificationStatus.SENT);
            }
        } else {
            dbStrategy.withDayBudget(BigDecimal.ZERO)
                    .withDayBudgetShowMode(CampaignsDayBudgetShowMode.DEFAULT_);
        }

        return dbStrategy;
    }
}
