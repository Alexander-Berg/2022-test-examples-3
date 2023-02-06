package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.RetargetingCampaignInfo;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.retargeting.service.DeleteRetargetingValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.interests;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.audienceTargetNotFound;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.cantRemoveAudienceTargetFromArchivedCampaign;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.duplicatedRetargetingId;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.unableToDelete;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DeleteRetargetingValidationServiceTest {

    @Autowired
    private DeleteRetargetingValidationService validationService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private Steps steps;

    private RetargetingInfo retargetingInfo1;

    private long retargetingId1;
    private long retargetingId2;
    private long campaignId;
    private long operationUid;
    private int shard;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        retargetingInfo1 = steps.retargetingSteps().createDefaultRetargeting();
        RetargetingInfo retargetingInfo2 =
                steps.retargetingSteps().createDefaultRetargeting(retargetingInfo1.getAdGroupInfo());

        operationUid = retargetingInfo1.getUid();
        shard = retargetingInfo1.getShard();
        campaignId = retargetingInfo1.getCampaignId();
        clientInfo = retargetingInfo1.getClientInfo();

        retargetingId1 = retargetingInfo1.getRetargetingId();
        retargetingId2 = retargetingInfo2.getRetargetingId();
    }

    @Test
    public void validate_ValidIds_ResultIsSuccessful() {
        ValidationResult<List<Long>, Defect> vr = validate(retargetingId1, retargetingId2);
        assertThat(vr.hasAnyErrors(), is(false));
    }

    @Test
    public void validate_duplicatedIds_DuplicatedRetargetingIdDefect() {
        ValidationResult<List<Long>, Defect> vr = validate(retargetingId1, retargetingId1);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), duplicatedRetargetingId())));
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(1)), duplicatedRetargetingId())));
    }

    @Test
    public void validate_archivedAdGroup_CantRemoveRetargetingFromArchivedCampaignDefect() {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.Yes)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();

        ValidationResult<List<Long>, Defect> vr = validate(retargetingId1);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0)), cantRemoveAudienceTargetFromArchivedCampaign(campaignId))));
    }

    @Test
    public void validate_RemoveFromRbacStub_AudienceTargetNotFoundDefect() {
        operationUid = steps.clientSteps().createDefaultClient().getUid();

        ValidationResult<List<Long>, Defect> vr = validate(retargetingId1);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), audienceTargetNotFound())));
    }

    @Test
    public void validate_cpmPriceCampaign_UnableToDeleteDefect() {
        var pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients(clientInfo)
                .getPricePackage();
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        var adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);

        var retargetingCondition = defaultRetCondition(clientInfo.getClientId());
        retargetingCondition
                .withRules(List.of(new Rule().withType(RuleType.OR).withGoals(emptyList())))
                .withType(interests);
        steps.retConditionSteps().createRetCondition(retargetingCondition, clientInfo);

        var retargeting = defaultRetargeting(campaign.getId(), adGroup.getId(), retargetingCondition.getId())
                .withPriceContext(pricePackage.getPrice());
        var retargetingId = retargetingRepository.add(shard, singletonList(retargeting)).get(0);

        ValidationResult<List<Long>, Defect> vr = validate(retargetingId);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), unableToDelete())));
    }

    private ValidationResult<List<Long>, Defect> validate(Long... idArray) {
        List<Long> ids = Arrays.asList(idArray);

        Map<Long, RetargetingCampaignInfo> retargetinsInfo =
                retargetingRepository.getRetargetingToCampaignMappingForDelete(shard, ids);

        return validationService.validate(ids, retargetinsInfo, operationUid);
    }
}
