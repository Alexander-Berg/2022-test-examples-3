package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultSmartCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithWwManagedOrderAddOperationSupportTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private UserInfo defaultUser;
    private int counterId;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();

        steps.featureSteps().setCurrentClient(defaultUser.getClientId());

        counterId = RandomNumberUtils.nextPositiveInteger();
        metrikaClientStub.addUserCounter(defaultUser.getUid(), counterId);
    }

    @Test
    public void addTextCampaignWithWwManagedOrderFlag_HasError() {
        TextCampaign textCampaign = defaultTextCampaign().withIsWwManagedOrder(true);
        RestrictedCampaignsAddOperation operation =
                getRestrictedCampaignAddOperation(textCampaign);
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CommonCampaign.IS_WW_MANAGED_ORDER)),
                CampaignDefects.inconsistentCampaignType())));

    }

    @Test
    public void addSmartCampaignWithWwManagedOrderFlag_FeatureDisabled_HasError() {
        SmartCampaign smartCampaign = defaultSmartCampaign()
                .withMetrikaCounters(List.of((long) counterId))
                .withIsWwManagedOrder(true);
        RestrictedCampaignsAddOperation operation = getRestrictedCampaignAddOperation(smartCampaign);
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CommonCampaign.IS_WW_MANAGED_ORDER)),
                DefectIds.INVALID_VALUE)));
    }

    @Test
    public void addSmartCampaignWithWwManagedOrderFlag_FeatureEnabled_NoError() {
        steps.featureSteps().addClientFeature(defaultUser.getClientId(), FeatureName.IS_WW_MANAGED_ORDER_AVAILABLE,
                true);
        SmartCampaign smartCampaign = defaultSmartCampaign()
                .withMetrikaCounters(List.of((long) counterId))
                .withIsWwManagedOrder(true);
        RestrictedCampaignsAddOperation operation = getRestrictedCampaignAddOperation(smartCampaign);
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    private RestrictedCampaignsAddOperation getRestrictedCampaignAddOperation(BaseCampaign textCampaign) {
        return campaignOperationService.createRestrictedCampaignAddOperation(List.of(textCampaign),
                defaultUser.getUid(),
                defaultUser.getUidAndClientId(), new CampaignOptions());
    }
}
