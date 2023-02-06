package ru.yandex.direct.core.entity.strategy.type.autobudgetavgclick;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgClickTypeRepositorySupportTest
        extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgClick> {

    @Override
    protected AutobudgetAvgClick generateModel() {
        return new AutobudgetAvgClick()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                .withAvgBid(BigDecimal.ONE)
                .withClientId(1L)
                .withWalletId(2L)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withMetrikaCounters(List.of())
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    @Override
    protected ModelChanges<AutobudgetAvgClick> generateModelChanges(AutobudgetAvgClick model) {
        return ModelChanges.build(model, AutobudgetAvgClick.AVG_BID, BigDecimal.TEN);
    }
}
