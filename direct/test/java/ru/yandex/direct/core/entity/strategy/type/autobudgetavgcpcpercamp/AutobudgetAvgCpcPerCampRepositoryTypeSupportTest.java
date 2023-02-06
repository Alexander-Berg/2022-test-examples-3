package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpcpercamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerCamp;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgCpcPerCampRepositoryTypeSupportTest
        extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgCpcPerCamp> {

    @Override
    protected AutobudgetAvgCpcPerCamp generateModel() {
        return new AutobudgetAvgCpcPerCamp()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withAvgBid(BigDecimal.ONE)
                .withBid(BigDecimal.ONE)
                .withMetrikaCounters(List.of())
                .withSum(BigDecimal.ONE);
    }

    @Override
    protected ModelChanges<AutobudgetAvgCpcPerCamp> generateModelChanges(AutobudgetAvgCpcPerCamp model) {
        return new ModelChanges<>(model.getId(), AutobudgetAvgCpcPerCamp.class)
                .process(BigDecimal.TEN, AutobudgetAvgCpcPerCamp.AVG_BID)
                .process(BigDecimal.TEN, AutobudgetAvgCpcPerCamp.SUM)
                .process(BigDecimal.TEN, AutobudgetAvgCpcPerCamp.BID);
    }
}
