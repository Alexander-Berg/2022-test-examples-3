package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidForRetargeting;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRetargetingConditionValidationServiceIndoorTest {
    @Autowired
    private UpdateRetargetingConditionValidationService2 validationUnderTest;

    @Autowired
    private Steps steps;

    @Autowired
    RetargetingRepository retargetingRepository;

    @Test
    public void validConditionWithNotOnlyDemographics_Error() {
        CampaignInfo cpmBannerCampaign = steps.campaignSteps().createActiveCpmBannerCampaign();
        ClientInfo clientInfo = cpmBannerCampaign.getClientInfo();
        int shard = clientInfo.getShard();
        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmIndoorVideoCreative(clientInfo, videoCreativeId);

        AdGroupInfo cpmIndoorAdGroup = steps.adGroupSteps().createActiveCpmIndoorAdGroup(cpmBannerCampaign);

        RetConditionInfo retConditionInfoCpm = steps.retConditionSteps().createCpmRetCondition(clientInfo);

        Retargeting retargeting = defaultRetargeting(cpmBannerCampaign.getCampaignId(),
                cpmIndoorAdGroup.getAdGroupId(), retConditionInfoCpm.getRetConditionId());
        retargetingRepository.add(shard, singletonList(retargeting));

        ModelChanges<RetargetingCondition> modelChanges =
                retargetingConditionModelChanges(retConditionInfoCpm.getRetConditionId());

        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> preMassValidation =
                new ValidationResult<>(singletonList(modelChanges));
        AppliedChanges<RetargetingCondition> appliedChanges = modelChanges.applyTo(retConditionInfoCpm.getRetCondition());

        ValidationResult<List<RetargetingCondition>, Defect> actual = validationUnderTest.validate(
                preMassValidation, singletonList(retConditionInfoCpm.getRetCondition()),
                singletonList(appliedChanges), clientInfo.getClientId(), shard);

        Assert.assertThat(actual.flattenErrors(),
                contains(validationError(path(index(0)),
                        retargetingConditionIsInvalidForRetargeting())));
    }

    private static ModelChanges<RetargetingCondition> retargetingConditionModelChanges(Long id) {
        return new ModelChanges<>(id, RetargetingCondition.class);
    }
}
