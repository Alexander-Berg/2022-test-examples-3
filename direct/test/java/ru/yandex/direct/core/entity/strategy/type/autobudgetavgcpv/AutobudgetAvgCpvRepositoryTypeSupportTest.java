package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpv;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpv;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgCpvRepositoryTypeSupportTest
        extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgCpv> {

    @Override
    protected AutobudgetAvgCpv generateModel() {
        return new AutobudgetAvgCpv()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CPV)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withAvgCpv(BigDecimal.ONE)
                .withMetrikaCounters(List.of())
                .withSum(BigDecimal.ONE);
    }

    @Override
    protected ModelChanges<AutobudgetAvgCpv> generateModelChanges(AutobudgetAvgCpv model) {
        return new ModelChanges<>(model.getId(), AutobudgetAvgCpv.class)
                .process(BigDecimal.TEN, AutobudgetAvgCpv.AVG_CPV)
                .process(BigDecimal.TEN, AutobudgetAvgCpv.SUM);
    }
}
