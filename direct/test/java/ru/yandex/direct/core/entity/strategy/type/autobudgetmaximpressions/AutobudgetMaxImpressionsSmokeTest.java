package ru.yandex.direct.core.entity.strategy.type.autobudgetmaximpressions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressions;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetMaxImpressionsSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetMaxImpressions> {
    @Override
    protected AutobudgetMaxImpressions generateModel() {
        return new AutobudgetMaxImpressions()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS)
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
    protected ModelChanges<AutobudgetMaxImpressions> generateModelChanges(AutobudgetMaxImpressions model) {
        return new ModelChanges<>(model.getId(), AutobudgetMaxImpressions.class)
                .process(BigDecimal.valueOf(654.32), AutobudgetMaxImpressions.AVG_CPM)
                .process(BigDecimal.valueOf(1000000.21), AutobudgetMaxImpressions.SUM);
    }
}
