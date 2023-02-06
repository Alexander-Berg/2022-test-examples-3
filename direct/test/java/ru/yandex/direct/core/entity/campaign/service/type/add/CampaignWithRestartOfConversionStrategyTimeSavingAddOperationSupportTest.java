package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.pricerecalculation.CommonCampaignPriceRecalculationService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;

@RunWith(MockitoJUnitRunner.class)
public class CampaignWithRestartOfConversionStrategyTimeSavingAddOperationSupportTest {

    @Mock
    private FeatureService featureService;

    @Mock
    private CommonCampaignPriceRecalculationService commonCampaignPriceRecalculationService;

    @InjectMocks
    private CampaignWithCustomStrategyAddOperationSupport support;

    private RestrictedCampaignsAddOperationContainer addParameters;

    public CampaignWithRestartOfConversionStrategyTimeSavingAddOperationSupportTest() {
    }

    @Before
    public void before() {
        int shard = 1;
        Long uid = 2L;
        ClientId clientId = ClientId.fromLong(3L);

        addParameters = RestrictedCampaignsAddOperationContainer.create(
                shard,
                uid,
                clientId,
                uid,
                uid);
    }

    @Test
    public void conversionStrategy_FeatureDisabled_RestartTimeNotUpdate() {
        doReturn(false)
                .when(featureService)
                .isEnabledForClientId(eq(addParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        CampaignWithCustomStrategy campaign = new TextCampaign().withId(RandomNumberUtils.nextPositiveLong())
                .withStrategy(defaultAverageCpaStrategy(RandomNumberUtils.nextPositiveLong()))
                .withAttributionModel(CampaignAttributionModel.FIRST_CLICK);

        support.onModelsValidated(addParameters, List.of(campaign));

        assertThat(campaign.getStrategy().getStrategyData().getLastBidderRestartTime()).isNull();
    }

    @Test
    public void conversionStrategy_RestartTimeNotUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(addParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        CampaignWithCustomStrategy campaign = new TextCampaign().withId(RandomNumberUtils.nextPositiveLong())
                .withStrategy(defaultAverageCpaStrategy(RandomNumberUtils.nextPositiveLong()))
                .withAttributionModel(CampaignAttributionModel.FIRST_CLICK);

        support.onModelsValidated(addParameters, List.of(campaign));

        assertThat(campaign.getStrategy().getStrategyData().getLastBidderRestartTime()).isNotNull();
    }
}
