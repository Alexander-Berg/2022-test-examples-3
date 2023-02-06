package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpvcustomperiod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpvCustomPeriod;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgCpvCustomPeriodSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgCpvCustomPeriod> {

    @Override
    protected AutobudgetAvgCpvCustomPeriod generateModel() {
        return new AutobudgetAvgCpvCustomPeriod()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withAvgCpv(BigDecimal.ONE)
                .withAutoProlongation(true)
                .withBudget(BigDecimal.valueOf(746.199))
                .withDailyChangeCount(72L)
                .withStart(LocalDate.MIN)
                .withFinish(LocalDate.MAX)
                .withMetrikaCounters(List.of())
                .withLastUpdateTime(LocalDate.now().atStartOfDay());
    }

    @Override
    protected ModelChanges<AutobudgetAvgCpvCustomPeriod> generateModelChanges(AutobudgetAvgCpvCustomPeriod model) {
        return new ModelChanges<>(model.getId(), AutobudgetAvgCpvCustomPeriod.class)
                .process(BigDecimal.TEN, AutobudgetAvgCpvCustomPeriod.AVG_CPV)
                .process(BigDecimal.valueOf(10800.21), AutobudgetMaxReachCustomPeriod.BUDGET)
                .process(false, AutobudgetMaxReachCustomPeriod.AUTO_PROLONGATION)
                .process(LocalDate.now().minusDays(1).atStartOfDay(), AutobudgetMaxReachCustomPeriod.LAST_UPDATE_TIME);
    }

}
