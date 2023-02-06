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
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.result.MassResult;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaign;
import static ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpiStrategy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignAddOperationStrategyRestartTimeMobileContentTest {
    public static final int GOAL_ID_INSTALLS = 4;
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";

    @Autowired
    CampaignOperationService campaignOperationService;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    MetrikaClientStub metrikaClientStub;

    @Autowired
    private Steps steps;

    private UserInfo defaultUser;
    private Long mobileAppId;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
        MobileAppInfo mobileAppInfo =
                steps.mobileAppSteps().createMobileApp(defaultUser.getClientInfo(), STORE_URL);
        mobileAppId = mobileAppInfo.getMobileAppId();
    }

    @Test
    public void addCampaignWithConversionStrategy_AddRestartTime() {
        steps.featureSteps().addClientFeature(defaultUser.getClientId(),
                FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED, true);

        MobileContentCampaign campaign = defaultMobileContentCampaign()
                .withMobileAppId(mobileAppId)
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withStrategy(defaultAutobudgetAvgCpiStrategy((long) GOAL_ID_INSTALLS));

        LocalDateTime now = now().minusSeconds(1);
        RestrictedCampaignsAddOperation addOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(List.of(campaign),
                        defaultUser.getUid(), UidAndClientId.of(defaultUser.getUid(), defaultUser.getClientId()),
                        new CampaignOptions());

        MassResult<Long> result = addOperation.prepareAndApply();
        Long id = result.get(0).getResult();
        MobileContentCampaign actualCampaign =
                (MobileContentCampaign) campaignTypedRepository.getTypedCampaignsMap(defaultUser.getShard(),
                        List.of(id)).get(id);
        assertThat(actualCampaign.getStrategy().getStrategyData().getLastBidderRestartTime()).isAfterOrEqualTo(now);
    }
}
