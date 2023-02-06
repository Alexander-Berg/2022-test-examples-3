package ru.yandex.direct.teststeps.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.FeedSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.regions.GeoTree;

import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeSpecificAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;

@Service
@ParametersAreNonnullByDefault
public class AdGroupStepsService {
    private final AdGroupSteps adGroupSteps;
    private final FeedSteps feedSteps;
    private final InfoHelper infoHelper;
    private final CampaignTypedRepository campaignTypedRepository;
    private final ShardHelper shardHelper;
    private final TestAdGroupRepository testAdGroupRepository;
    private final ClientGeoService clientGeoService;
    private final PricePackageService pricePackageService;
    private final MobileAppRepository mobileAppRepository;
    private final MobileContentRepository mobileContentRepository;

    @Autowired
    public AdGroupStepsService(
            AdGroupSteps adGroupSteps,
            FeedSteps feedSteps,
            InfoHelper infoHelper,
            CampaignTypedRepository campaignTypedRepository,
            ShardHelper shardHelper,
            TestAdGroupRepository testAdGroupRepository,
            ClientGeoService clientGeoService,
            PricePackageService pricePackageService,
            MobileAppRepository mobileAppRepository,
            MobileContentRepository mobileContentRepository) {
        this.adGroupSteps = adGroupSteps;
        this.feedSteps = feedSteps;
        this.infoHelper = infoHelper;
        this.campaignTypedRepository = campaignTypedRepository;
        this.shardHelper = shardHelper;
        this.testAdGroupRepository = testAdGroupRepository;
        this.clientGeoService = clientGeoService;
        this.pricePackageService = pricePackageService;
        this.mobileAppRepository = mobileAppRepository;
        this.mobileContentRepository = mobileContentRepository;
    }

    public Long createActiveTextAdGroup(String login, Long campaignId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);

        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(activeTextAdGroup(campaignId))
                .withCampaignInfo(campaignInfo);
        return adGroupSteps.createAdGroup(adGroupInfo).getAdGroupId();
    }

    public Long createActiveCpmBannerAdGroup(String login, Long campaignId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);

        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(campaignId))
                .withCampaignInfo(campaignInfo);
        return adGroupSteps.createAdGroup(adGroupInfo).getAdGroupId();
    }

    public Long createActiveDynamicTextAdGroup(String login, Long campaignId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);

        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(activeDynamicTextAdGroup(campaignId))
                .withCampaignInfo(campaignInfo);
        return adGroupSteps.createAdGroup(adGroupInfo).getAdGroupId();
    }

    public Long createActiveDynamicFeedAdGroup(String login, Long campaignId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);
        FeedInfo feed = feedSteps.createDefaultFeed(clientInfo);
        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(activeDynamicFeedAdGroup(campaignId, feed.getFeedId()))
                .withCampaignInfo(campaignInfo);
        return adGroupSteps.createAdGroup(adGroupInfo).getAdGroupId();
    }

    public Long createActiveDynamicFeedAdGroup(String login, Long campaignId, Long feedId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);
        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(activeDynamicFeedAdGroup(campaignId, feedId))
                .withCampaignInfo(campaignInfo);
        return adGroupSteps.createAdGroup(adGroupInfo).getAdGroupId();
    }

    public Long createActiveCpmPriceAdGroup(String login, Long campaignId, Boolean isDefaultGroup) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        int shard = shardHelper.getShardByCampaignId(campaignId);
        CpmPriceCampaign cpmPriceCampaign = (CpmPriceCampaign) campaignTypedRepository.getTypedCampaigns(shard,
                List.of(campaignId)).get(0);
        CpmYndxFrontpageAdGroup adGroup = isDefaultGroup ?
                activeDefaultAdGroupForPriceSales(cpmPriceCampaign)
                : activeSpecificAdGroupForPriceSales(cpmPriceCampaign);
        GeoTree geoTree = pricePackageService.getGeoTree();
        List<Long> geo = clientGeoService.convertForSave(adGroup.getGeo(), geoTree);
        adGroup.setGeo(geoTree.refineGeoIds(geo));
        adGroupSteps.createAdGroupRaw(adGroup, clientInfo);
        return adGroup.getId();
    }

    public Long createInternalAdGroup(String login, Long campaignId, @Nullable String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);

        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(activeInternalAdGroup(campaignId, 0L))
                .withCampaignInfo(campaignInfo);
        if (name != null) {
            adGroupInfo.getAdGroup().setName(name);
        }
        return adGroupSteps.createAdGroup(adGroupInfo).getAdGroupId();
    }

    public Long createMobileContentAdGroup(String login, Long campaignId, @Nullable String name) {
        var userInfo = infoHelper.getUserInfo(login);
        var clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        int shard = shardHelper.getShardByCampaignId(campaignId);
        var campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);
        var mobileContentCampaign =
                (MobileContentCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(campaignId)).get(0);

        var mobileApp = mobileAppRepository.getMobileApps(
                shard, clientInfo.getClientId(), List.of(mobileContentCampaign.getMobileAppId()));
        var mobileContent = mobileContentRepository.getMobileContent(shard, mobileApp.get(0).getMobileContentId());

        var adGroupInfo = new AdGroupInfo()
                .withAdGroup(createMobileAppAdGroup(campaignId, mobileContent))
                .withCampaignInfo(campaignInfo);

        if (name != null) {
            adGroupInfo.getAdGroup().setName(name);
        }

        return adGroupSteps.createAdGroup(adGroupInfo).getAdGroupId();
    }

    public void deleteAdgroups(String login, Long adGroupId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        int shard = shardHelper.getShardByGroupId(adGroupId);
        adGroupSteps.deleteAdGroups(shard, clientInfo.getClientId(), clientInfo.getUid(), List.of(adGroupId));
    }

    public void updateAdGroupName(Long adGroupId, @Nullable String name) {
        if (name == null) {
            return;
        }
        int shard = shardHelper.getShardByGroupId(adGroupId);
        testAdGroupRepository.updateAdGroupName(shard, adGroupId, name);
    }
}
