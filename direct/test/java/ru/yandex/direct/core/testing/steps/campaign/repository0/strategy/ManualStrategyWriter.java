package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import java.math.BigDecimal;

import ru.yandex.direct.core.entity.campaign.model.CampOptionsDayBudgetNotificationStatus;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsDayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.StrategyPlace;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;

import static ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignMappings.dayBudgetShowModeToDb;

public class ManualStrategyWriter implements StrategyWriter {

    @Override
    public DbStrategy write(Strategy strategy) {
        WriterUtils.checkArgument(strategy, ManualStrategy.class);
        ManualStrategy manualStrategy = (ManualStrategy) strategy;
        DbStrategy dbStrategy = (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.NO)
                .withPlatform(manualStrategy.getPlatform())
                .withStrategy(manualStrategy.isSeparateBids() ? CampOptionsStrategy.DIFFERENT_PLACES : null);

        switch (manualStrategy.getManualStrategyMode()) {
            case HIGHEST_POSITION_ALL: {
                dbStrategy
                        .withStrategyName(StrategyName.DEFAULT_)
                        .withStrategyData(new StrategyData()
                                .withName("default")
                                .withUnknownFields(manualStrategy.getUnknownFields())
                                .withVersion(VERSION));
                break;
            }
            case HIGHEST_POSITION_GUARANTEE: {
                dbStrategy
                        .withStrategyName(StrategyName.NO_PREMIUM)
                        .withStrategyData(new StrategyData()
                                .withName("default")
                                .withUnknownFields(manualStrategy.getUnknownFields())
                                .withPlace(StrategyPlace.HIGHEST_PLACE)
                                .withVersion(VERSION));
                break;
            }
            default:
                throw new IllegalArgumentException("unknown manual strategy mode: " +
                        manualStrategy.getManualStrategyMode());
        }

        if (manualStrategy.getDayBudget() != null) {
            DayBudget dayBudget = manualStrategy.getDayBudget();

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
                dbStrategy.withDayBudgetNotificationStatus(CampOptionsDayBudgetNotificationStatus.READY);
            }
        } else {
            dbStrategy.withDayBudget(BigDecimal.ZERO)
                    .withDayBudgetShowMode(CampaignsDayBudgetShowMode.DEFAULT_);
        }

        return dbStrategy;
    }
}
