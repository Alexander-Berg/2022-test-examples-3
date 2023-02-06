package ru.yandex.direct.core.entity.strategy.type.autobudgetcrr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetCrrSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetCrr> {
    @Override
    protected AutobudgetCrr generateModel() {
        return new AutobudgetCrr()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_CRR)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withGoalId(15L)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withCrr(110L)
                .withLastBidderRestartTime(LocalDate.now().atStartOfDay())
                .withSum(BigDecimal.valueOf(9999.99))
                .withMetrikaCounters(List.of())
                .withIsPayForConversionEnabled(true);
    }

    @Override
    protected ModelChanges<AutobudgetCrr> generateModelChanges(AutobudgetCrr model) {
        return new ModelChanges<>(model.getId(), AutobudgetCrr.class)
                .process(false, AutobudgetCrr.IS_PAY_FOR_CONVERSION_ENABLED)
                .process(220L, AutobudgetCrr.CRR)
                .process(BigDecimal.valueOf(1112), AutobudgetCrr.SUM);
    }
}
