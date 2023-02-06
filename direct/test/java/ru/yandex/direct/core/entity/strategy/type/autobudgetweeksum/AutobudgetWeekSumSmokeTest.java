package ru.yandex.direct.core.entity.strategy.type.autobudgetweeksum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetWeekSumSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetWeekSum> {
    @Override
    protected AutobudgetWeekSum generateModel() {
        return new AutobudgetWeekSum()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withGoalId(15L)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withBid(BigDecimal.valueOf(132.5))
                .withLastBidderRestartTime(LocalDate.now().atStartOfDay())
                .withMetrikaCounters(List.of())
                .withSum(BigDecimal.valueOf(9999.99));
    }

    @Override
    protected ModelChanges<AutobudgetWeekSum> generateModelChanges(AutobudgetWeekSum model) {
        return new ModelChanges<>(model.getId(), AutobudgetWeekSum.class)
                .process(25L, AutobudgetWeekSum.GOAL_ID)
                .process(BigDecimal.valueOf(4444.4), AutobudgetWeekSum.BID)
                .process(BigDecimal.valueOf(15555.2), AutobudgetWeekSum.SUM)
                .process(LocalDate.now().plusDays(1).atStartOfDay(), AutobudgetWeekSum.LAST_BIDDER_RESTART_TIME);
    }
}
