package ru.yandex.direct.core.testing.steps.campaign.repository0.strategy;

import java.math.BigDecimal;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategyMode;
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignMappings;

public class ManualStrategyReader implements StrategyReader {
    @Override
    public ManualStrategy read(DbStrategy dbStrategy) {
        ManualStrategy manualStrategy = new ManualStrategy()
                .withSeparateBids(dbStrategy.isDifferentPlaces())
                .withManualStrategyMode(detectMode(dbStrategy))
                .withPlatform(dbStrategy.getPlatform())
                .withUnknownFields(dbStrategy.getStrategyData().getUnknownFields());

        if (dbStrategy.getDayBudget().compareTo(BigDecimal.ZERO) > 0) {
            manualStrategy.withDayBudget(new DayBudget()
                    .withDayBudget(dbStrategy.getDayBudget())
                    .withShowMode(CampaignMappings.dayBudgetShowModeFromDb(dbStrategy.getDayBudgetShowMode())));
        }

        return manualStrategy;
    }

    private ManualStrategyMode detectMode(DbStrategy dbStrategy) {
        StrategyName strategyName = dbStrategy.getStrategyName();

        if (strategyName == StrategyName.NO_PREMIUM) {
            return ManualStrategyMode.HIGHEST_POSITION_GUARANTEE;
        } else {
            return ManualStrategyMode.HIGHEST_POSITION_ALL;
        }
    }
}
