package ru.yandex.direct.core.entity.strategy.type.autobudgetroi;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetRoiSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetRoi> {
    @Override
    protected AutobudgetRoi generateModel() {
        return new AutobudgetRoi()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_ROI)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withGoalId(15L)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withBid(BigDecimal.valueOf(132.5))
                .withSum(BigDecimal.valueOf(9999.99))
                .withProfitability(BigDecimal.valueOf(1.5))
                .withMetrikaCounters(List.of())
                .withReserveReturn(10L)
                .withRoiCoef(BigDecimal.valueOf(20));
    }

    @Override
    protected ModelChanges<AutobudgetRoi> generateModelChanges(AutobudgetRoi model) {
        return new ModelChanges<>(model.getId(), AutobudgetRoi.class)
                .process(BigDecimal.valueOf(30), AutobudgetRoi.ROI_COEF)
                .process(15L, AutobudgetRoi.RESERVE_RETURN)
                .process(BigDecimal.valueOf(2.1), AutobudgetRoi.PROFITABILITY)
                .process(BigDecimal.valueOf(5555.5), AutobudgetRoi.SUM);
    }
}
