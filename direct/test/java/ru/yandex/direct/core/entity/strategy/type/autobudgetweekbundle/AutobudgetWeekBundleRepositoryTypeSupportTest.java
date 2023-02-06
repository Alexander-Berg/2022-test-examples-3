package ru.yandex.direct.core.entity.strategy.type.autobudgetweekbundle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetWeekBundleRepositoryTypeSupportTest
        extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetWeekBundle> {

    @Autowired
    private AutobudgetWeekBundleRepositoryTypeSupport repositoryTypeSupport;

    @Test
    public void shouldCreateMapper() {
        var mapper = repositoryTypeSupport.createMapper();
        assertThat(mapper.getReadableModelProperties())
                .contains(AutobudgetWeekBundle.LIMIT_CLICKS);
        assertThat(mapper.getWritableModelProperties())
                .contains(AutobudgetWeekBundle.LIMIT_CLICKS);
    }

    @Override
    protected AutobudgetWeekBundle generateModel() {
        return new AutobudgetWeekBundle()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withMetrikaCounters(List.of())
                .withLimitClicks(10L);
    }

    @Override
    protected ModelChanges<AutobudgetWeekBundle> generateModelChanges(AutobudgetWeekBundle model) {
        return ModelChanges.build(model, AutobudgetWeekBundle.LIMIT_CLICKS, 1L);
    }
}
