package ru.yandex.direct.core.entity.strategy.type.autobudgetmedia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMedia;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetMediaSmokeTest extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetMedia> {
    @Override
    protected AutobudgetMedia generateModel() {
        return new AutobudgetMedia()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_MEDIA)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withMetrikaCounters(List.of())
                .withDate(LocalDate.now());
    }

    @Override
    protected ModelChanges<AutobudgetMedia> generateModelChanges(AutobudgetMedia model) {
        return new ModelChanges<>(model.getId(), AutobudgetMedia.class)
                .process(LocalDate.now().minusDays(1), AutobudgetMedia.DATE);
    }
}
