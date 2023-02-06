package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestGroups.clientTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationModerationTest extends AdGroupsAddOperationTestBase {

    @Test
    public void createsDraftAdGroupInModeratedCampaignWhenSaveDraftIsTrue() {
        AdGroup adGroup = clientTextAdGroup(campaignId)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, true);

        AdGroup expectedGroup = new TextAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void createsReadyAdGroupInModeratedCampaignWhenSaveDraftIsFalse() {
        AdGroup adGroup = clientTextAdGroup(campaignId)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, false);

        AdGroup expectedGroup = new TextAdGroup()
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.NO);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void createsDraftAdGroupInDraftCampaignWhenSaveDraftIsTrue() {
        CampaignInfo draftCampInfo = createDraftCampaign();

        AdGroup adGroup = clientTextAdGroup(draftCampInfo.getCampaignId())
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, true);

        AdGroup expectedGroup = new TextAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void createsDraftAdGroupInDraftCampaignWhenSaveDraftIsFalse() {
        CampaignInfo draftCampInfo = createDraftCampaign();

        AdGroup adGroup = clientTextAdGroup(draftCampInfo.getCampaignId())
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, false);

        AdGroup expectedGroup = new TextAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void statusPostModerateIsNotChangedWhenInputStatusIsRejectedAndSaveDraftIsTrue() {
        AdGroup adGroup = clientTextAdGroup(campaignId)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.REJECTED);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, true);

        AdGroup expectedGroup = new TextAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.REJECTED);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void statusPostModerateIsNotChangedWhenInputStatusIsRejectedAndSaveDraftIsFalse() {
        AdGroup adGroup = clientTextAdGroup(campaignId)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.REJECTED);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, false);

        AdGroup expectedGroup = new TextAdGroup()
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.REJECTED);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void sendRejectedCampaignToModerationWhenSaveDraftIsFalse() {
        CampaignInfo rejCampInfo = createRejectedCampaign();

        AdGroup adGroup = clientTextAdGroup(rejCampInfo.getCampaignId())
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        addAndCheckResultIsSuccessful(adGroup, false);

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singleton(rejCampInfo.getCampaignId()));
        assertThat(campaigns.get(0).getStatusModerate(), equalTo(CampaignStatusModerate.READY));
    }

    @Test
    public void doesntSendRejectedCampaignToModerationWhenSaveDraftIsTrue() {
        CampaignInfo rejCampInfo = createRejectedCampaign();

        AdGroup adGroup = clientTextAdGroup(rejCampInfo.getCampaignId())
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        addAndCheckResultIsSuccessful(adGroup, true);

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singleton(rejCampInfo.getCampaignId()));
        assertThat(campaigns.get(0).getStatusModerate(), equalTo(CampaignStatusModerate.NO));
    }
}
