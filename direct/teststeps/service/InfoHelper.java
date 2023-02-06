package ru.yandex.direct.teststeps.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@Service
@ParametersAreNonnullByDefault
public class InfoHelper {
    private final UserService userService;
    private final ShardHelper shardHelper;
    private final AdGroupRepository adGroupRepository;
    private final CampaignRepository campaignRepository;

    @Autowired
    public InfoHelper(UserService userService,
                      ShardHelper shardHelper,
                      AdGroupRepository adGroupRepository,
                      CampaignRepository campaignRepository) {

        this.userService = userService;
        this.shardHelper = shardHelper;
        this.adGroupRepository = adGroupRepository;
        this.campaignRepository = campaignRepository;
    }

    public UserInfo getUserInfo(String login) throws IllegalArgumentException {
        Long uid = userService.getUidByLogin(login);
        if (uid == null) {
            throw new IllegalArgumentException("Not found uid for login " + login);
        }
        return new UserInfo()
                .withUser(userService.getUser(uid));
    }

    public ClientInfo getClientInfo(String login, Long uid) {
        Long clientId = userService.getClientIdByLogin(login);
        if (clientId == null) {
            throw new IllegalArgumentException("Not found clientId for login " + login);
        }
        int shard = shardHelper.getShardByClientId(ClientId.fromLong(clientId));
        var userInfo = getUserInfo(login);
        return new ClientInfo()
                .withChiefUserInfo(userInfo)
                .withShard(shard)
                .withClient(new Client()
                        .withClientId(clientId)
                        .withWorkCurrency(CurrencyCode.RUB));
    }

    public CampaignInfo getCampaignInfo(Long campaignId, ClientInfo clientInfo) {
        return new CampaignInfo()
                .withCampaign(new Campaign()
                        .withId(campaignId))
                .withClientInfo(clientInfo);
    }


    public AdGroupInfo getFullAdGroupInfo(Long adGroupId, ClientInfo clientInfo) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(clientInfo.getShard(), singleton(adGroupId));
        if (adGroups.isEmpty()) {
            throw new IllegalArgumentException("adGroup not found: " + adGroupId);
        }
        AdGroup adGroup = adGroups.get(0);

        CampaignInfo campaignInfo = getFullCampaignInfo(adGroup.getCampaignId(), clientInfo);

        return new AdGroupInfo()
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo)
                .withClientInfo(clientInfo);
    }


    public CampaignInfo getFullCampaignInfo(Long campaignId, ClientInfo clientInfo) {
        List<Campaign> campaigns = campaignRepository.getCampaigns(clientInfo.getShard(), singletonList(campaignId));

        if (campaigns.isEmpty()) {
            throw new IllegalArgumentException("campaign not found: " + campaignId);
        }
        Campaign campaign = campaigns.get(0);

        return new CampaignInfo()
                .withCampaign(campaign)
                .withClientInfo(clientInfo);
    }
}
