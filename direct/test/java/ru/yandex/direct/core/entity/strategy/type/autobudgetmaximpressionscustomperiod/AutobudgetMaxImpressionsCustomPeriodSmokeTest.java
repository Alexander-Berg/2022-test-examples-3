package ru.yandex.direct.core.entity.strategy.type.autobudgetmaximpressionscustomperiod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressionsCustomPeriod;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetMaxImpressionsCustomPeriodSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetMaxImpressionsCustomPeriod> {
    @Override
    protected AutobudgetMaxImpressionsCustomPeriod generateModel() {
        return new AutobudgetMaxImpressionsCustomPeriod()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withAvgCpm(BigDecimal.valueOf(543.21))
                .withAutoProlongation(true)
                .withBudget(BigDecimal.valueOf(746.199))
                .withDailyChangeCount(72L)
                .withStart(LocalDate.MIN)
                .withFinish(LocalDate.MAX)
                .withMetrikaCounters(List.of())
                .withLastUpdateTime(LocalDate.now().atStartOfDay());
    }

    @Override
    protected ModelChanges<AutobudgetMaxImpressionsCustomPeriod> generateModelChanges(AutobudgetMaxImpressionsCustomPeriod model) {
        return new ModelChanges<>(model.getId(), AutobudgetMaxImpressionsCustomPeriod.class)
                .process(BigDecimal.valueOf(654.32), AutobudgetMaxImpressionsCustomPeriod.AVG_CPM)
                .process(BigDecimal.valueOf(1000.21), AutobudgetMaxImpressionsCustomPeriod.BUDGET)
                .process(false, AutobudgetMaxImpressionsCustomPeriod.AUTO_PROLONGATION);
    }
}
