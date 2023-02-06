package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgCpaAddSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgCpa> {
    @Override
    protected AutobudgetAvgCpa generateModel() {
        return new AutobudgetAvgCpa()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CPA)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withGoalId(15L)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withBid(BigDecimal.valueOf(888.6))
                .withAvgCpa(BigDecimal.valueOf(10.001))
                .withLastBidderRestartTime(LocalDate.now().atStartOfDay())
                .withSum(BigDecimal.valueOf(9999.99))
                .withMetrikaCounters(List.of())
                .withIsPayForConversionEnabled(true);
    }

    @Override
    protected ModelChanges<AutobudgetAvgCpa> generateModelChanges(AutobudgetAvgCpa model) {
        return new ModelChanges<>(model.getId(), AutobudgetAvgCpa.class)
                .process(20L, AutobudgetAvgCpa.GOAL_ID)
                .process(false, AutobudgetAvgCpa.IS_PAY_FOR_CONVERSION_ENABLED);
    }
}
