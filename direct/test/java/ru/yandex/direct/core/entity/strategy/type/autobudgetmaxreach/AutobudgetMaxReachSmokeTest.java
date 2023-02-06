package ru.yandex.direct.core.entity.strategy.type.autobudgetmaxreach;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressions;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetMaxReachSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetMaxReach> {
    @Override
    protected AutobudgetMaxReach generateModel() {
        return new AutobudgetMaxReach()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_MAX_REACH)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withAvgCpm(BigDecimal.valueOf(543.21))
                .withMetrikaCounters(List.of())
                .withSum(BigDecimal.valueOf(9999.99));
    }

    @Override
    protected ModelChanges<AutobudgetMaxReach> generateModelChanges(AutobudgetMaxReach model) {
        return new ModelChanges<>(model.getId(), AutobudgetMaxReach.class)
                .process(BigDecimal.valueOf(64.32), AutobudgetMaxImpressions.AVG_CPM)
                .process(BigDecimal.valueOf(10000.21), AutobudgetMaxImpressions.SUM);
    }
}
