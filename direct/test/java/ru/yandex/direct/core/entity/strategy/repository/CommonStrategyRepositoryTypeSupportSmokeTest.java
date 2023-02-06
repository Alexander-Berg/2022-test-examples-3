package ru.yandex.direct.core.entity.strategy.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick;
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class CommonStrategyRepositoryTypeSupportSmokeTest
        extends StrategyRepositoryTypeSupportSmokeTest<CommonStrategy> {

    @Override
    protected CommonStrategy generateModel() {
        return new AutobudgetAvgClick()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                .withAvgBid(BigDecimal.ONE)
                .withClientId(1L)
                .withWalletId(2L)
                .withStatusArchived(false)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withMetrikaCounters(List.of())
                .withIsPublic(false)
                .withName("Some name");
    }

    @Override
    protected ModelChanges<CommonStrategy> generateModelChanges(CommonStrategy model) {
        return new ModelChanges<>(model.getId(), CommonStrategy.class)
                .process(true, CommonStrategy.STATUS_ARCHIVED)
                .process(true, CommonStrategy.IS_PUBLIC)
                .process(StrategyAttributionModel.LAST_CLICK, CommonStrategy.ATTRIBUTION_MODEL);
    }
}
