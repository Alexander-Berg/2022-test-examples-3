package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroup.CAMPAIGN_ID;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNoRights;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNotFound;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexTextAddValidationRightsTest extends ComplexTextAddValidationTestBase {

    @Autowired
    private RbacService rbacService;

    @Autowired
    private TestAdGroupRepository testAdGroupRepository;

    @Autowired
    private TestKeywordRepository testKeywordRepository;

    @Test
    public void hasOneErrorWhenAdGroupHasOtherClientCampaignId() {
        CampaignInfo otherClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        ComplexTextAdGroup adGroup = fullAdGroup(otherClientCampaignInfo.getCampaignId());

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(adGroup);
        Path errPath = path(index(0), field(CAMPAIGN_ID.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, campaignNotFound())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void noChangesWhenAdGroupHasOtherClientCampaignId() {
        CampaignInfo otherClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        int otherClientShard = otherClientCampaignInfo.getShard();
        ClientId otherClientId = otherClientCampaignInfo.getClientId();
        ComplexTextAdGroup adGroup = fullAdGroup(otherClientCampaignInfo.getCampaignId());
        checkState(adGroup.getKeywords().size() > 0);

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(adGroup);
        Path errPath = path(index(0), field(CAMPAIGN_ID.name()));
        assumeThat(vr, hasDefectDefinitionWith(validationError(errPath, campaignNotFound())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assumeThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));

        Set<Long> otherClientAdGroupIds =
                testAdGroupRepository.getClientAdGroupIds(otherClientShard, otherClientId);
        assertThat(otherClientAdGroupIds, emptyIterable());

        List<String> otherClientPhrases =
                testKeywordRepository.getClientPhrases(otherClientShard, otherClientId);
        assertThat(otherClientPhrases, emptyIterable());
    }

    @Test
    public void noChangesWhenAdGroupHasOtherClientCampaignIdAndValidAdGroupPresents() {
        CampaignInfo otherClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        int otherClientShard = otherClientCampaignInfo.getShard();
        ClientId otherClientId = otherClientCampaignInfo.getClientId();
        ComplexTextAdGroup adGroupForOtherClient = fullAdGroup(otherClientCampaignInfo.getCampaignId());
        ComplexTextAdGroup adGroupForThisClient = fullAdGroup(campaign.getCampaignId());
        checkState(adGroupForOtherClient.getKeywords().size() > 0);
        checkState(adGroupForThisClient.getKeywords().size() > 0);

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(adGroupForThisClient, adGroupForOtherClient);
        Path errPath = path(index(1), field(CAMPAIGN_ID.name()));
        assumeThat(vr, hasDefectDefinitionWith(validationError(errPath, campaignNotFound())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assumeThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));

        Set<Long> otherClientAdGroupIds =
                testAdGroupRepository.getClientAdGroupIds(otherClientShard, otherClientId);
        assertThat(otherClientAdGroupIds, emptyIterable());

        List<String> otherClientPhrases =
                testKeywordRepository.getClientPhrases(otherClientShard, otherClientId);
        assertThat(otherClientPhrases, emptyIterable());

        Set<Long> thisClientAdGroupIds =
                testAdGroupRepository.getClientAdGroupIds(campaign.getShard(), campaign.getClientId());
        assertThat(thisClientAdGroupIds, emptyIterable());

        List<String> thisClientPhrases =
                testKeywordRepository.getClientPhrases(campaign.getShard(), campaign.getClientId());
        assertThat(thisClientPhrases, emptyIterable());
    }

    @Test
    public void hasOneErrorWhenClientCanNotWriteToAdGroupCampaign() {
        ComplexTextAdGroup adGroup = fullAdGroup(campaign.getCampaignId());
        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER).getChiefUserInfo();

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(adGroup);
        Path errPath = path(index(0), field(CAMPAIGN_ID.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, campaignNoRights())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }
}
