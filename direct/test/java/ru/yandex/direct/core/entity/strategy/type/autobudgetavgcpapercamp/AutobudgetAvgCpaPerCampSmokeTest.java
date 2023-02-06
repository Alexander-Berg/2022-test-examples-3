package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpapercamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerCamp;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgCpaPerCampSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgCpaPerCamp> {
    @Override
    protected AutobudgetAvgCpaPerCamp generateModel() {
        return new AutobudgetAvgCpaPerCamp()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withGoalId(15L)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withAvgCpa(BigDecimal.valueOf(678.9))
                .withBid(BigDecimal.valueOf(132.5))
                .withLastBidderRestartTime(LocalDate.now().atStartOfDay())
                .withSum(BigDecimal.valueOf(9999.99))
                .withMetrikaCounters(List.of())
                .withIsPayForConversionEnabled(true);
    }

    @Override
    protected ModelChanges<AutobudgetAvgCpaPerCamp> generateModelChanges(AutobudgetAvgCpaPerCamp model) {
        return new ModelChanges<>(model.getId(), AutobudgetAvgCpaPerCamp.class)
                .process(44L, AutobudgetAvgCpaPerCamp.GOAL_ID)
                .process(false, AutobudgetAvgCpaPerCamp.IS_PAY_FOR_CONVERSION_ENABLED)
                .process(LocalDate.now().minusDays(1).atStartOfDay(), AutobudgetAvgCpaPerCamp.LAST_BIDDER_RESTART_TIME)
                .process(new BigDecimal("333.33"), AutobudgetAvgCpaPerCamp.AVG_CPA);
    }
}
