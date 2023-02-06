package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.SmartCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithWwManagedOrderUpdateOperationSupportTest {
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
        counterId = RandomNumberUtils.nextPositiveInteger();
        metrikaClientStub.addUserCounter(defaultUser.getUid(), counterId);
        steps.featureSteps().setCurrentClient(defaultUser.getClientId());
    }

    @Test
    public void updateTextCampaignWithWwManagedOrderFlag_HasError() {
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(defaultUser.getClientInfo());
        ModelChanges<CommonCampaign> mc = ModelChanges.build(campaign.getCampaignId(), CommonCampaign.class,
                CommonCampaign.IS_WW_MANAGED_ORDER, true);
        RestrictedCampaignsUpdateOperation operation =
                getRestrictedCampaignUpdateOperation(mc);
        MassResult<Long> result = operation.apply();
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CommonCampaign.IS_WW_MANAGED_ORDER)),
                CampaignDefects.inconsistentCampaignType())));

    }

    @Test
    public void updateSmartCampaignWithWwManagedOrderFlag_FeatureDisabled_HasError() {
        SmartCampaignInfo campaign = steps.smartCampaignSteps().createDefaultCampaign(defaultUser.getClientInfo());
        ModelChanges<CommonCampaign> mc = ModelChanges.build(campaign.getCampaignId(), CommonCampaign.class,
                CommonCampaign.IS_WW_MANAGED_ORDER, true);
        RestrictedCampaignsUpdateOperation operation =
                getRestrictedCampaignUpdateOperation(mc);
        MassResult<Long> result = operation.apply();

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CommonCampaign.IS_WW_MANAGED_ORDER)),
                DefectIds.INVALID_VALUE)));
    }

    @Test
    public void updateSmartCampaignWithWwManagedOrderFlag_FeatureEnabled_NoError() {
        steps.featureSteps().addClientFeature(defaultUser.getClientId(), FeatureName.IS_WW_MANAGED_ORDER_AVAILABLE,
                true);
        SmartCampaignInfo campaign = steps.smartCampaignSteps().createDefaultCampaign(defaultUser.getClientInfo());
        ModelChanges<SmartCampaign> mc = ModelChanges.build(campaign.getCampaignId(), SmartCampaign.class,
                SmartCampaign.IS_WW_MANAGED_ORDER, true);
        mc.process(List.of((long) counterId), SmartCampaign.METRIKA_COUNTERS);
        RestrictedCampaignsUpdateOperation operation =
                getRestrictedCampaignUpdateOperation(mc);
        MassResult<Long> result = operation.apply();
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    private RestrictedCampaignsUpdateOperation getRestrictedCampaignUpdateOperation(ModelChanges<?
            extends BaseCampaign> mc) {
        return campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc),
                defaultUser.getUid(),
                defaultUser.getUidAndClientId(), new CampaignOptions());
    }
}
