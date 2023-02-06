package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithSimplifiedStrategyView;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.dafaultAverageCpaPayForConversionStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategyForSimpleView;

@CoreTest
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class CampaignWithSimplifiedStrategyViewUpdateOperationSupportTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    public static final long ID = 1L;
    public static final RestrictedCampaignsUpdateOperationContainer CONTAINER =
            RestrictedCampaignsUpdateOperationContainer.create(1, 1L, ClientId.fromLong(1L), 1L, 1L);

    @Autowired
    private CampaignWithSimplifiedStrategyViewUpdateOperationSupport support;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    @Parameterized.Parameter(0)
    public String name;

    @Parameterized.Parameter(1)
    public ModelChanges<CampaignWithSimplifiedStrategyView> modelChanges;

    @Parameterized.Parameter(2)
    public Boolean expectedFlag;

    private CampaignWithSimplifiedStrategyView campaign;

    @Parameterized.Parameters(name = "{0}")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {"add unsupported field for simple view",
                        new ModelChanges<>(1L, TextCampaign.class)
                                .process((DbStrategy) defaultStrategy().withDayBudget(BigDecimal.TEN),
                                CampaignWithSimplifiedStrategyView.STRATEGY),
                        false},
                {
                        "change attribution model to unsupported for simple view",
                        new ModelChanges<>(ID, TextCampaign.class)
                                .process(CampaignAttributionModel.LAST_CLICK,
                                CampaignWithSimplifiedStrategyView.ATTRIBUTION_MODEL),
                        false
                },
                {
                        "change strategy name to unsupported for simple view",
                        new ModelChanges<>(1L, TextCampaign.class)
                                .process((DbStrategy) defaultStrategy().withStrategyName(StrategyName.AUTOBUDGET_ROI),
                                CampaignWithSimplifiedStrategyView.STRATEGY),
                        false
                },
                {
                        "change strategy goal id",
                        new ModelChanges<>(1L, TextCampaign.class)
                                .process(defaultStrategyForSimpleView(124L),
                                CampaignWithSimplifiedStrategyView.STRATEGY),
                        true
                },
                {
                        "change to pay for conversion",
                        new ModelChanges<>(1L, TextCampaign.class)
                                .process(dafaultAverageCpaPayForConversionStrategy(),
                                CampaignWithSimplifiedStrategyView.STRATEGY),
                        true
                }

        });
    }

    @Before
    public void before() {
        campaign = new TextCampaign()
                .withId(1L)
                .withIsSimplifiedStrategyViewEnabled(true)
                .withAttributionModel(campaignConstantsService.getDefaultAttributionModel())
                .withStrategy(defaultStrategyForSimpleView(123L));

    }

    @Test
    public void onAppliedChangesValidated() {
        AppliedChanges<CampaignWithSimplifiedStrategyView> campaignAppliedChanges = modelChanges.applyTo(campaign);
        support.onAppliedChangesValidated(CONTAINER, List.of(campaignAppliedChanges));
        assertThat(campaignAppliedChanges.getNewValue(
                CampaignWithSimplifiedStrategyView.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED))
                .isEqualTo(expectedFlag);
    }
}
