package ru.yandex.direct.teststeps.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CreativeSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@Service
@ParametersAreNonnullByDefault
public class AdStepsService {
    private final BannerSteps bannerSteps;
    private final InfoHelper infoHelper;
    private final CreativeSteps creativeSteps;
    private final ShardHelper shardHelper;
    private final CampaignTypedRepository campaignTypedRepository;
    private final AdGroupRepository adGroupRepository;

    @Autowired
    public AdStepsService(BannerSteps bannerSteps, InfoHelper infoHelper, CreativeSteps creativeSteps,
                          ShardHelper shardHelper, CampaignTypedRepository campaignTypedRepository,
                          AdGroupRepository adGroupRepository) {
        this.bannerSteps = bannerSteps;
        this.infoHelper = infoHelper;
        this.creativeSteps = creativeSteps;
        this.shardHelper = shardHelper;
        this.campaignTypedRepository = campaignTypedRepository;
        this.adGroupRepository = adGroupRepository;
    }

    public Long createDefaultActiveTextAd(String login, Long campaignId, Long adGroupId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);

        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(new AdGroup()
                        .withCampaignId(campaignId)
                        .withId(adGroupId))
                .withCampaignInfo(campaignInfo);
        return bannerSteps.createActiveTextBanner(adGroupInfo).getBannerId();
    }

    public Long createActiveTextAd(String login, Long campaignId, Long adGroupId, String title, String titleExtension, String body, String href) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);

        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(new AdGroup()
                        .withCampaignId(campaignId)
                        .withId(adGroupId))
                .withCampaignInfo(campaignInfo);
        return bannerSteps.createActiveTextBanner(adGroupInfo, title, titleExtension, body, href).getBannerId();
    }

    public Long createActiveCpmBannerAd(String login, Long campaignId, Long adGroupId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);

        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(new AdGroup()
                        .withCampaignId(campaignId)
                        .withId(adGroupId))
                .withCampaignInfo(campaignInfo);
        return bannerSteps.createActiveCpmBanner(adGroupInfo).getBannerId();
    }

    public Long createActiveDynamicAd(String login, Long campaignId, Long adGroupId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        CampaignInfo campaignInfo = infoHelper.getCampaignInfo(campaignId, clientInfo);

        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(new AdGroup()
                        .withCampaignId(campaignId)
                        .withId(adGroupId))
                .withCampaignInfo(campaignInfo);
        return bannerSteps.createActiveDynamicBanner(adGroupInfo).getBannerId();
    }

    public Long createActiveCpmPriceAd(String login, Long adGroupId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        int shard = shardHelper.getShardByGroupId(adGroupId);
        AdGroupSimple adGroupSimple = adGroupRepository.getAdGroupSimple(shard, null, List.of(adGroupId))
                .get(adGroupId);
        Long campaignId = adGroupSimple.getCampaignId();
        CpmPriceCampaign cpmPriceCampaign = (CpmPriceCampaign) campaignTypedRepository.getTypedCampaigns(shard,
                List.of(campaignId)).get(0);
        CreativeInfo creative = creativeSteps.addDefaultHtml5CreativeForPriceSales(clientInfo, cpmPriceCampaign);
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, creative.getCreativeId());
        bannerSteps.createActiveCpmBannerRaw(shard, banner, campaignId, adGroupId);
        return banner.getId();
    }

    public void deleteAd(Long adId) {
        int shard = shardHelper.getShardByBannerId(adId);
        bannerSteps.deleteBanners(shard, List.of(adId));
    }

}
