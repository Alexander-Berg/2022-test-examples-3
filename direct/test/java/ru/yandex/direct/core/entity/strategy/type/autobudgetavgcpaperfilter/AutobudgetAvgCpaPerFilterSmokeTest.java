package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpaperfilter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgCpaPerFilterSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgCpaPerFilter> {
    @Override
    protected AutobudgetAvgCpaPerFilter generateModel() {
        return new AutobudgetAvgCpaPerFilter()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withGoalId(15L)
                .withBid(BigDecimal.valueOf(132.5))
                .withFilterAvgCpa(BigDecimal.valueOf(11111.5))
                .withLastBidderRestartTime(LocalDate.now().atStartOfDay())
                .withSum(BigDecimal.valueOf(9999.99))
                .withMetrikaCounters(List.of())
                .withIsPayForConversionEnabled(true);
    }

    @Override
    protected ModelChanges<AutobudgetAvgCpaPerFilter> generateModelChanges(AutobudgetAvgCpaPerFilter model) {
        return new ModelChanges<>(model.getId(), AutobudgetAvgCpaPerFilter.class)
                .process(26L, AutobudgetAvgCpaPerFilter.GOAL_ID)
                .process(false, AutobudgetAvgCpaPerFilter.IS_PAY_FOR_CONVERSION_ENABLED)
                .process(BigDecimal.valueOf(222.2), AutobudgetAvgCpaPerFilter.SUM);
    }
}
