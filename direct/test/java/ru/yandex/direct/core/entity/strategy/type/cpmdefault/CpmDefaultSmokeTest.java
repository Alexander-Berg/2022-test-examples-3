package ru.yandex.direct.core.entity.strategy.type.cpmdefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.CpmDefault;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class CpmDefaultSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<CpmDefault> {
    @Override
    protected CpmDefault generateModel() {
        return new CpmDefault()
                .withId(nextStrategyId())
                .withType(StrategyName.CPM_DEFAULT)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withDayBudget(new BigDecimal("100.00"))
                .withDayBudgetDailyChangeCount(1)
                .withDayBudgetShowMode(StrategyDayBudgetShowMode.STRETCHED)
                .withIsPublic(false)
                .withMetrikaCounters(List.of())
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    @Override
    protected ModelChanges<CpmDefault> generateModelChanges(CpmDefault model) {
        return new ModelChanges<>(model.getId(), CpmDefault.class);
    }
}
