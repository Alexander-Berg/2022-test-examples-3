package ru.yandex.direct.core.entity.strategy.type.autobudgetmaxreachcustomperiod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetMaxReachCustomPeriodSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetMaxReachCustomPeriod> {
    @Override
    protected AutobudgetMaxReachCustomPeriod generateModel() {
        return new AutobudgetMaxReachCustomPeriod()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
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
    protected ModelChanges<AutobudgetMaxReachCustomPeriod> generateModelChanges(AutobudgetMaxReachCustomPeriod model) {
        return new ModelChanges<>(model.getId(), AutobudgetMaxReachCustomPeriod.class)
                .process(BigDecimal.valueOf(6544.32), AutobudgetMaxReachCustomPeriod.AVG_CPM)
                .process(BigDecimal.valueOf(10800.21), AutobudgetMaxReachCustomPeriod.BUDGET)
                .process(false, AutobudgetMaxReachCustomPeriod.AUTO_PROLONGATION)
                .process(LocalDate.now().minusDays(1).atStartOfDay(), AutobudgetMaxReachCustomPeriod.LAST_UPDATE_TIME);
    }
}
