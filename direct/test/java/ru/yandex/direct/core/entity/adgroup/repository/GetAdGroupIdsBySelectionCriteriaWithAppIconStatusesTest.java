package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupAppIconStatus;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.MobileContentSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupAppIconStatus.ACCEPTED;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupAppIconStatus.MODERATION;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupAppIconStatus.REJECTED;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithAppIconModerationStatusNo;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithAppIconModerationStatusReady;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithAppIconModerationStatusSending;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithAppIconModerationStatusSent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentWithAppIconModerationStatusYes;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetAdGroupIdsBySelectionCriteriaWithAppIconStatusesTest {
    private int shard;
    private Long campaignId;
    private Long iconOnModerationAdGroupId1;
    private Long iconOnModerationAdGroupId2;
    private Long iconOnModerationAdGroupId3;
    private Long iconAcceptedAdGroupId;
    private Long iconRejectedAdGroupId;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository repository;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo());

        shard = clientInfo.getShard();

        CampaignInfo mobileAppCampaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        campaignId = mobileAppCampaign.getCampaignId();

        MobileContentSteps mobileContentSteps = steps.mobileContentSteps();

        MobileContent mobileContentWithAppIconModerationStatusReady = mobileContentWithAppIconModerationStatusReady();
        mobileContentSteps.createMobileContent(shard, new MobileContentInfo().withClientInfo(clientInfo)
                .withMobileContent(mobileContentWithAppIconModerationStatusReady));

        MobileContent mobileContentWithAppIconModerationStatusSending =
                mobileContentWithAppIconModerationStatusSending();
        mobileContentSteps.createMobileContent(shard, new MobileContentInfo().withClientInfo(clientInfo)
                .withMobileContent(mobileContentWithAppIconModerationStatusSending));

        MobileContent mobileContentWithAppIconModerationStatusSent = mobileContentWithAppIconModerationStatusSent();
        mobileContentSteps.createMobileContent(shard, new MobileContentInfo().withClientInfo(clientInfo)
                .withMobileContent(mobileContentWithAppIconModerationStatusSent));

        MobileContent mobileContentWithAppIconModerationStatusYes = mobileContentWithAppIconModerationStatusYes();
        mobileContentSteps.createMobileContent(shard, new MobileContentInfo().withClientInfo(clientInfo)
                .withMobileContent(mobileContentWithAppIconModerationStatusYes));

        MobileContent mobileContentWithAppIconModerationStatusNo = mobileContentWithAppIconModerationStatusNo();
        mobileContentSteps.createMobileContent(shard, new MobileContentInfo().withClientInfo(clientInfo)
                .withMobileContent(mobileContentWithAppIconModerationStatusNo));

        AdGroupSteps adGroupSteps = steps.adGroupSteps();

        iconOnModerationAdGroupId1 = adGroupSteps.createAdGroup(createMobileAppAdGroup(campaignId,
                mobileContentWithAppIconModerationStatusReady), mobileAppCampaign).getAdGroupId();

        iconOnModerationAdGroupId2 = adGroupSteps.createAdGroup(createMobileAppAdGroup(campaignId,
                mobileContentWithAppIconModerationStatusSending), mobileAppCampaign).getAdGroupId();

        iconOnModerationAdGroupId3 = adGroupSteps.createAdGroup(
                createMobileAppAdGroup(campaignId, mobileContentWithAppIconModerationStatusSent),
                mobileAppCampaign).getAdGroupId();

        iconAcceptedAdGroupId = adGroupSteps.createAdGroup(
                createMobileAppAdGroup(campaignId, mobileContentWithAppIconModerationStatusYes),
                mobileAppCampaign).getAdGroupId();

        iconRejectedAdGroupId = adGroupSteps.createAdGroup(
                createMobileAppAdGroup(campaignId, mobileContentWithAppIconModerationStatusNo),
                mobileAppCampaign).getAdGroupId();
    }

    @Test
    public void getAdGroupsWithAppIconOnModeration() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignId).withAdGroupAppIconStatuses(MODERATION),
                maxLimited());

        assertThat("вернулись id ожидаемых групп с иконками приложений на модерации", adGroupIds,
                contains(iconOnModerationAdGroupId1, iconOnModerationAdGroupId2, iconOnModerationAdGroupId3));
    }

    @Test
    public void getAdGroupsWithAcceptedAppIcon() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignId).withAdGroupAppIconStatuses(ACCEPTED),
                maxLimited());

        assertThat("вернулись id ожидаемых групп с иконками приложений, принятых на модерации", adGroupIds,
                contains(iconAcceptedAdGroupId));
    }

    @Test
    public void getAdGroupsWithRejectedAppIcon() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignId).withAdGroupAppIconStatuses(REJECTED),
                maxLimited());

        assertThat("вернулись id ожидаемых групп с иконками приложений, отклоненных на модерации", adGroupIds,
                contains(iconRejectedAdGroupId));
    }

    @Test
    public void getAdGroupsWithAllAppIconModerationStatuses() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignId)
                        .withAdGroupAppIconStatuses(MODERATION, ACCEPTED, REJECTED), maxLimited());

        assertThat("вернулись id ожидаемых групп с иконками приложений со всеми статусами модерации", adGroupIds,
                contains(iconOnModerationAdGroupId1, iconOnModerationAdGroupId2, iconOnModerationAdGroupId3,
                        iconAcceptedAdGroupId, iconRejectedAdGroupId));
    }

    @Test
    public void getAdGroupsWithNullAsAppIconModerationStatus() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignId)
                        .withAdGroupAppIconStatuses((Set<AdGroupAppIconStatus>) null), maxLimited());

        assertThat("вернулись id ожидаемых групп с иконками приложений со всеми статусами модерации", adGroupIds,
                contains(iconOnModerationAdGroupId1, iconOnModerationAdGroupId2, iconOnModerationAdGroupId3,
                        iconAcceptedAdGroupId, iconRejectedAdGroupId));
    }
}
