package ru.yandex.direct.grid.processing.service.showcondition.converter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdDynamicCampaign;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaign;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.showcondition.GdKeyword;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowCondition;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionAutobudgetPriority;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFeatures;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.service.showcondition.converter.ShowConditionFeatureCalculator.FEATURE_CALCULATOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class ShowConditionFeatureCalculatorTest {
    private static final List<GdShowCondition> TEST_CONDITIONS = ImmutableList.of(
            new GdKeyword()
                    .withPrice(BigDecimal.TEN)
                    .withAdGroup(new GdTextAdGroup()
                            .withCampaign(new GdDynamicCampaign()
                                    .withType(GdCampaignType.DYNAMIC)
                                    .withFlatStrategy(new GdCampaignStrategyManual()
                                            .withPlatform(GdCampaignPlatform.SEARCH)))),
            new GdKeyword()
                    .withPriceContext(BigDecimal.TEN)
                    .withAdGroup(new GdTextAdGroup()
                            .withCampaign(new GdTextCampaign()
                                    .withType(GdCampaignType.TEXT)
                                    .withFlatStrategy(new GdCampaignStrategyManual()
                                            .withPlatform(GdCampaignPlatform.CONTEXT)))),
            new GdKeyword()
                    .withAutobudgetPriority(GdShowConditionAutobudgetPriority.MEDIUM)
                    .withAdGroup(new GdTextAdGroup()
                            .withCampaign(new GdMobileContentCampaign()
                                    .withType(GdCampaignType.MOBILE_CONTENT)
                                    .withFlatStrategy(new GdCampaignStrategyManual()
                                            .withPlatform(GdCampaignPlatform.BOTH))))
    );

    @Test
    public void testCalculatorEmpty() {
        GdShowConditionFeatures expected = new GdShowConditionFeatures()
                .withHasPrice(false)
                .withHasPriceContext(false)
                .withHasAutobudgetPriority(false)
                .withCommonCampaignTypes(Collections.emptySet())
                .withCommonPlatform(Collections.emptySet());
        assertThat(FEATURE_CALCULATOR.apply(Collections.emptyList()))
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testCalculatorNotEmpty() {
        GdShowConditionFeatures expected = new GdShowConditionFeatures()
                .withHasPrice(true)
                .withHasPriceContext(true)
                .withHasAutobudgetPriority(true)
                .withCommonCampaignTypes(
                        ImmutableSet.of(GdCampaignType.DYNAMIC, GdCampaignType.MOBILE_CONTENT, GdCampaignType.TEXT))
                .withCommonPlatform(
                        ImmutableSet
                                .of(GdCampaignPlatform.BOTH, GdCampaignPlatform.CONTEXT, GdCampaignPlatform.SEARCH));
        assertThat(FEATURE_CALCULATOR.apply(TEST_CONDITIONS))
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testCalculatorNotEmpty_NoAutobudget() {
        GdShowConditionFeatures expected = new GdShowConditionFeatures()
                .withHasPrice(true)
                .withHasPriceContext(true)
                .withHasAutobudgetPriority(false)
                .withCommonCampaignTypes(
                        ImmutableSet.of(GdCampaignType.DYNAMIC, GdCampaignType.TEXT))
                .withCommonPlatform(
                        ImmutableSet.of(GdCampaignPlatform.CONTEXT, GdCampaignPlatform.SEARCH));
        assertThat(FEATURE_CALCULATOR.apply(TEST_CONDITIONS.subList(0, TEST_CONDITIONS.size() - 1)))
                .is(matchedBy(beanDiffer(expected)));
    }
}
