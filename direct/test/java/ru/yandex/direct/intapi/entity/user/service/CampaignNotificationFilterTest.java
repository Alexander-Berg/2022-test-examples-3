package ru.yandex.direct.intapi.entity.user.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardKey;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignNotificationFilterTest {
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Steps steps;

    private CampaignInfo textCampaignInfo;
    private CampaignInfo geoCampaignInfo;
    private ContentPromotionAdGroupInfo edaAdGroupInfo;
    private ContentPromotionAdGroupInfo serviceAdGroupInfo;
    private ContentPromotionAdGroupInfo videoAdGroupInfo;

    private CampaignNotificationFilter campaignNotificationFilter;

    @Before
    public void setUp() {
        textCampaignInfo = steps.campaignSteps().createActiveTextCampaign();

        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();

        geoCampaignInfo = steps.campaignSteps().createCampaign(
                TestCampaigns.newGeoCampaign(campaignInfo.getClientId(), campaignInfo.getUid()),
                campaignInfo.getClientInfo());

        edaAdGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(
                campaignInfo.getClientInfo(),
                ContentPromotionAdgroupType.EDA);

        serviceAdGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(
                campaignInfo.getClientInfo(),
                ContentPromotionAdgroupType.SERVICE);

        videoAdGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(
                campaignInfo.getClientInfo(),
                ContentPromotionAdgroupType.VIDEO);

        campaignNotificationFilter = new CampaignNotificationFilter(shardHelper, adGroupRepository);
    }

    @Test
    public void testCampaignFilter() {
        Set<Long> campaignIds = campaignNotificationFilter.shouldSendNotification(
                resolveCampaigns(Arrays.asList(
                        textCampaignInfo.getCampaignId(),
                        geoCampaignInfo.getCampaignId(),
                        edaAdGroupInfo.getCampaignId(),
                        serviceAdGroupInfo.getCampaignId(),
                        videoAdGroupInfo.getCampaignId())));

        Set<Long> target = Set.of(
                textCampaignInfo.getCampaignId(),
                videoAdGroupInfo.getCampaignId());

        assertThat(campaignIds).isEqualTo(target);
    }

    private List<Campaign> resolveCampaigns(Collection<Long> campaignsIds) {
        return shardHelper.groupByShard(campaignsIds, ShardKey.CID)
                .stream()
                .map(e -> campaignRepository.getCampaigns(e.getKey(), listToSet(e.getValue())))
                .flatMap(Collection::stream).toList();
    }

}
