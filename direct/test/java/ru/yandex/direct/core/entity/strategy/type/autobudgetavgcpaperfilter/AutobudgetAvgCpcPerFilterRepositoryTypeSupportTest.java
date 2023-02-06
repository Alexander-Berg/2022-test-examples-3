package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpaperfilter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportSmokeTest;
import ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpcperfilter.AutobudgetAvgCpcPerFilterRepositoryTypeSupport;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAvgCpcPerFilterRepositoryTypeSupportTest
        extends StrategyRepositoryTypeSupportSmokeTest<AutobudgetAvgCpcPerFilter> {

    @Autowired
    private AutobudgetAvgCpcPerFilterRepositoryTypeSupport typeSupport;

    @Test
    public void shouldCreateMapper() {
        var mapper = typeSupport.createMapper();
        assertThat(mapper.getReadableModelProperties())
                .contains(AutobudgetAvgCpcPerFilter.FILTER_AVG_BID);
        assertThat(mapper.getWritableModelProperties())
                .contains(AutobudgetAvgCpcPerFilter.FILTER_AVG_BID);
    }

    @Override
    protected AutobudgetAvgCpcPerFilter generateModel() {
        return new AutobudgetAvgCpcPerFilter()
                .withId(nextStrategyId())
                .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(1L).withConversionValue(BigDecimal.ONE)))
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withMetrikaCounters(List.of())
                .withFilterAvgBid(BigDecimal.ONE);
    }

    @Override
    protected ModelChanges<AutobudgetAvgCpcPerFilter> generateModelChanges(AutobudgetAvgCpcPerFilter model) {
        return ModelChanges.build(model, AutobudgetAvgCpcPerFilter.FILTER_AVG_BID, BigDecimal.TEN);
    }
}
