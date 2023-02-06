package ru.yandex.direct.core.entity.strategy.type.periodfixbid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.PeriodFixBid;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class PeriodFixBidSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<PeriodFixBid> {
    @Override
    protected PeriodFixBid generateModel() {
        return new PeriodFixBid()
                .withId(nextStrategyId())
                .withType(StrategyName.PERIOD_FIX_BID)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withAutoProlongation(false)
                .withBudget(BigDecimal.valueOf(746.199))
                .withStart(LocalDate.MIN)
                .withMetrikaCounters(List.of())
                .withFinish(LocalDate.MAX);
    }

    @Override
    protected ModelChanges<PeriodFixBid> generateModelChanges(PeriodFixBid model) {
        return new ModelChanges<>(model.getId(), PeriodFixBid.class)
                .process(LocalDate.of(2000, 1, 1), PeriodFixBid.START)
                .process(true, PeriodFixBid.AUTO_PROLONGATION);
    }
}
