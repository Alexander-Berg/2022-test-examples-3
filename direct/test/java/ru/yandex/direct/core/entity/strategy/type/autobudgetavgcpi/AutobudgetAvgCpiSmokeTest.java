package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgCpiSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgCpi> {
    @Override
    protected AutobudgetAvgCpi generateModel() {
        return new AutobudgetAvgCpi()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CPI)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withGoalId(15L)
                .withBid(BigDecimal.valueOf(132.5))
                .withAvgCpi(BigDecimal.valueOf(13.6))
                .withLastBidderRestartTime(LocalDate.now().atStartOfDay())
                .withSum(BigDecimal.valueOf(9999.99))
                .withIsPayForConversionEnabled(true);
    }

    @Override
    protected ModelChanges<AutobudgetAvgCpi> generateModelChanges(AutobudgetAvgCpi model) {
        return new ModelChanges<>(model.getId(), AutobudgetAvgCpi.class)
                .process(false, AutobudgetAvgCpi.IS_PAY_FOR_CONVERSION_ENABLED)
                .process(BigDecimal.valueOf(16.6), AutobudgetAvgCpi.AVG_CPI)
                .process(111L, AutobudgetAvgCpi.GOAL_ID);
    }
}
