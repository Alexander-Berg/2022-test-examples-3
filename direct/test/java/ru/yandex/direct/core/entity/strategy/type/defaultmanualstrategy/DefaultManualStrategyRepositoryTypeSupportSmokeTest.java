package ru.yandex.direct.core.entity.strategy.type.defaultmanualstrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class DefaultManualStrategyRepositoryTypeSupportSmokeTest
        extends StrategyRepositoryTypeSupportSmokeTest<DefaultManualStrategy> {

    @Override
    protected DefaultManualStrategy generateModel() {
        return new DefaultManualStrategy()
                .withId(nextStrategyId())
                .withType(StrategyName.DEFAULT_)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withEnableCpcHold(false)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
                .withDayBudgetDailyChangeCount(1)
                .withMetrikaCounters(List.of())
                .withDayBudget(new BigDecimal("1.00"));
    }

    @Override
    protected ModelChanges<DefaultManualStrategy> generateModelChanges(DefaultManualStrategy model) {
        return ModelChanges.build(model, DefaultManualStrategy.DAY_BUDGET, new BigDecimal("2.00"));
    }
}
