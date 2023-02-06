package ru.yandex.direct.core.entity.campaign.service.operation;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpiStrategy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignUpdateOperationStrategyRestartTimeMobileContentTest {
    public static final int GOAL_ID_INSTALLS = 4;
    public static final int GOAL_ID_OTHER_MOBILE_GOAL = 5;
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";

    @Autowired
    CampaignOperationService campaignOperationService;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    MetrikaClientStub metrikaClientStub;

    @Autowired
    private Steps steps;

    private MobileContentCampaignInfo campaignInfo;

    @Before
    public void before() {
        var defaultUser = steps.userSteps().createDefaultUser();

        // для поддержки GOAL_ID_OTHER_MOBILE_GOAL
        steps.featureSteps().addClientFeature(defaultUser.getClientId(),
                FeatureName.RMP_STAT_TRACKER_INSTALL_ENABLED, true);

        MobileAppInfo mobileAppInfo =
                steps.mobileAppSteps().createMobileApp(defaultUser.getClientInfo(), STORE_URL);
        var mobileAppId = mobileAppInfo.getMobileAppId();

        MobileContentCampaign campaign = defaultMobileContentCampaignWithSystemFields(defaultUser.getClientInfo())
                .withMobileAppId(mobileAppId)
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withStrategy(defaultAutobudgetAvgCpiStrategy((long) GOAL_ID_INSTALLS));

        campaignInfo = steps.mobileContentCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), campaign);
    }

    @Test
    public void updateGoalIdInConversionStrategy_AddRestartTime() {
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED, true);
        ModelChanges<MobileContentCampaign> mc = new ModelChanges<>(campaignInfo.getId(), MobileContentCampaign.class);
        mc.process(defaultAutobudgetAvgCpiStrategy((long) GOAL_ID_OTHER_MOBILE_GOAL),
                MobileContentCampaign.STRATEGY);

        LocalDateTime now = now().minusSeconds(1);
        var options = new CampaignOptions();
        var updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc),
                        campaignInfo.getUid(),
                        UidAndClientId.of(campaignInfo.getUid(), campaignInfo.getClientId()),
                        options);

        MassResult<Long> apply = updateOperation.apply();
        MobileContentCampaign actualCampaign =
                (MobileContentCampaign) campaignTypedRepository.getTypedCampaignsMap(campaignInfo.getShard(),
                        List.of(campaignInfo.getId())).get(campaignInfo.getId());
        assertThat(actualCampaign.getStrategy().getStrategyData().getLastBidderRestartTime()).isAfterOrEqualTo(now);
    }
}
