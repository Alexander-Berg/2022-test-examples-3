package ru.yandex.direct.teststeps.service;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campoperationqueue.CampOperationQueueRepository;
import ru.yandex.direct.core.entity.campoperationqueue.model.CampQueueOperation;
import ru.yandex.direct.core.entity.campoperationqueue.model.CampQueueOperationName;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsDayBudgetShowMode;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmBannerCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultInternalAutobudgetCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultInternalDistribCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultInternalFreeCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultSmartCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpcPerFilter;


@Service
@ParametersAreNonnullByDefault
public class CampaignStepsService {

    private final TypedCampaignStepsUnstubbed typedCampaignStepsUnstubbed;
    private final CampaignSteps campaignStepsUnstubbed;
    private final CampOperationQueueRepository campOperationQueueRepository;
    private final InfoHelper infoHelper;
    private final ShardHelper shardHelper;
    private final PricePackageRepository pricePackageRepository;

    @Autowired
    public CampaignStepsService(TypedCampaignStepsUnstubbed typedCampaignStepsUnstubbed,
                                CampaignSteps campaignStepsUnstubbed,
                                CampOperationQueueRepository campOperationQueueRepository,
                                InfoHelper infoHelper, ShardHelper shardHelper,
                                PricePackageRepository pricePackageRepository) {
        this.typedCampaignStepsUnstubbed = typedCampaignStepsUnstubbed;
        this.campaignStepsUnstubbed = campaignStepsUnstubbed;
        this.campOperationQueueRepository = campOperationQueueRepository;
        this.infoHelper = infoHelper;
        this.shardHelper = shardHelper;
        this.pricePackageRepository = pricePackageRepository;
    }

    public Long createDefaultTextCampaign(String login, @Nullable String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());

        TextCampaign campaign = defaultTextCampaignWithSystemFields()
                .withStatusActive(false)
                .withStatusShow(false)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withUid(userInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());

        if (userInfo.getUser().getAgencyClientId() != null) {
            campaign.setAgencyId(userInfo.getUser().getAgencyClientId());
            campaign.setAgencyUid(userInfo.getUser().getAgencyUserId());
        }

        if (name != null) {
            campaign.setName(name);
        }

        TypedCampaignInfo textCampaign = typedCampaignStepsUnstubbed.createTextCampaign(userInfo,
                clientInfo,
                campaign);

        return textCampaign.getId();
    }

    public Long createDefaultDynamicCampaign(String login, @Nullable String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());

        DynamicCampaign campaign = defaultDynamicCampaignWithSystemFields()
                .withStatusActive(false)
                .withStatusShow(false)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withUid(userInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());

        if (name != null) {
            campaign.setName(name);
        }

        TypedCampaignInfo textCampaign = typedCampaignStepsUnstubbed.createDynamicCampaign(userInfo,
                clientInfo,
                campaign);

        return textCampaign.getId();
    }

    public Long createDefaultCpmBannerCampaign(String login, @Nullable String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());

        CpmBannerCampaign campaign = defaultCpmBannerCampaignWithSystemFields()
                .withStatusActive(false)
                .withStatusShow(false)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withUid(userInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());

        if (name != null) {
            campaign.setName(name);
        }

        TypedCampaignInfo campaignInfo =
                typedCampaignStepsUnstubbed.createCpmBannerCampaign(userInfo, clientInfo, campaign);

        return campaignInfo.getId();
    }

    public Long createDefaultMobileContentCampaign(String login, @Nullable String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());

        MobileContentCampaign campaign = defaultMobileContentCampaignWithSystemFields()
                .withStatusActive(false)
                .withStatusShow(false)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withUid(userInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());

        if (name != null) {
            campaign.setName(name);
        }

        TypedCampaignInfo campaignInfo =
                typedCampaignStepsUnstubbed.createMobileContentCampaign(userInfo, clientInfo, campaign);

        return campaignInfo.getId();
    }

    public Long createDefaultSmartCampaign(String login, @Nullable String name, @Nullable List<Long> metrikaCounters) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());

        SmartCampaign campaign = defaultSmartCampaignWithSystemFields()
                .withStrategy(defaultAutobudgetAvgCpcPerFilter(0L))
                .withStatusActive(false)
                .withStatusShow(false)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withUid(userInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());

        if (name != null) {
            campaign.setName(name);
        }

        if (metrikaCounters != null) {
            campaign.setMetrikaCounters(metrikaCounters);
        }

        TypedCampaignInfo campaignInfo =
                typedCampaignStepsUnstubbed.createSmartCampaign(userInfo, clientInfo, campaign);

        return campaignInfo.getId();
    }

    public long createCpmPriceCampaign(String login, Long packageId, @Nullable String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        PricePackage pricePackage = pricePackageRepository.getPricePackages(List.of(packageId)).get(packageId);
        CpmPriceCampaign campaign = defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                // выставил некоторые параметры, чтобы кампания создавалась в максимально похожем состоянии с тем,
                // как она создаётся через интерфейс, возможно не всё из этого нужно
                .withUid(userInfo.getUid())
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.NEW)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusShow(true)
                .withStatusActive(false);
        if (name != null) {
            campaign.setName(name);
        }
        TypedCampaignInfo cpmPriceCampaign = typedCampaignStepsUnstubbed.createCpmPriceCampaign(userInfo,
                clientInfo, campaign);
        return cpmPriceCampaign.getId();
    }

    public long createFreeInternalCampaign(String login, Long placeId, Boolean isMobile, String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        InternalFreeCampaign campaign = defaultInternalFreeCampaignWithSystemFields(clientInfo)
                .withName(name)
                .withPlaceId(placeId)
                .withIsMobile(isMobile)
                .withStatusActive(false)
                .withStatusShow(false)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW);
        TypedCampaignInfo internalFreeCampaign = typedCampaignStepsUnstubbed
                .creatDefaultInternalFreeCampaign(userInfo, clientInfo, campaign);
        return internalFreeCampaign.getId();
    }

    public long createDistribInternalCampaign(String login, Long placeId, Boolean isMobile,
                                              Long rotationGoalId, String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        InternalDistribCampaign campaign = defaultInternalDistribCampaignWithSystemFields(clientInfo)
                .withName(name)
                .withPlaceId(placeId)
                .withIsMobile(isMobile)
                .withRotationGoalId(rotationGoalId)
                .withStatusActive(false)
                .withStatusShow(false)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW);
        TypedCampaignInfo internalDistribCampaign = typedCampaignStepsUnstubbed
                .creatDefaultInternalDistribCampaign(userInfo, clientInfo, campaign);
        return internalDistribCampaign.getId();
    }

    public long createAutobudgetInternalCampaign(String login, Long placeId, Boolean isMobile, String name) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        InternalAutobudgetCampaign campaign = defaultInternalAutobudgetCampaignWithSystemFields(clientInfo)
                .withName(name)
                .withPlaceId(placeId)
                .withIsMobile(isMobile)
                .withStatusActive(false)
                .withStatusShow(false)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW);
        TypedCampaignInfo internalAutobudgetCampaign = typedCampaignStepsUnstubbed
                .creatDefaultInternalAutobudgetCampaign(userInfo, clientInfo, campaign);
        return internalAutobudgetCampaign.getId();
    }

    public void setStatusModerate(Long campaignId, CampaignStatusModerate campaignStatusModerate) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.setStatusModerate(shard, campaignId, campaignStatusModerate);
    }

    public void setStatusBsSynced(Long campaignId, CampaignStatusBsSynced campaignStatusBsSynced) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.setStatusBsSynced(shard, campaignId, campaignStatusBsSynced);
    }

    public void archiveCampaign(Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.archiveCampaign(shard, campaignId);
    }

    public void unarchiveCampaign(Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.unarchiveCampaign(shard, campaignId);
    }

    public void makeCampaignFullyModerated(Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.makeCampaignFullyModerated(shard, campaignId);
    }

    public void makeCampaignReadyForDelete(Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.makeCampaignReadyForDelete(shard, campaignId);
    }

    public void makeCampaignReadyForDeleteInGrut(Long campaignId) {
        campaignStepsUnstubbed.makeCampaignReadyForDeleteInGrut(campaignId);
    }

    public void makeNewCampaignReadyForSendingToBS(Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.makeNewCampaignReadyForSendingToBS(shard, campaignId);
    }

    public void makeCampaignActive(Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.makeCampaignActive(shard, campaignId);
    }

    public void suspendCampaign(Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.campaignsSuspend(shard, campaignId);
    }

    public void setStrategy(Long campaignId, StrategyName strategyName) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.setStrategy(new CampaignInfo()
                        .withCampaign(new Campaign()
                                .withId(campaignId))
                        .withClientInfo(new ClientInfo()
                                .withShard(shard)),
                strategyName);
    }

    public void setDayBudget(Long campaignId, BigDecimal sum, @Nullable CampaignsDayBudgetShowMode dayBudgetShowMode,
                             @Nullable Integer changesCount) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.setDayBudget(new CampaignInfo()
                        .withCampaign(new Campaign()
                                .withId(campaignId))
                        .withClientInfo(new ClientInfo()
                                .withShard(shard)),
                sum, dayBudgetShowMode, changesCount);
    }

    public void deleteCampaign(Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        campaignStepsUnstubbed.deleteCampaign(shard, campaignId);

        // надо добавить задание в очередь на удаление кампании
        var campQueueOperation = new CampQueueOperation()
                .withCid(campaignId)
                .withCampQueueOperationName(CampQueueOperationName.DEL);
        campOperationQueueRepository.addCampaignQueueOperations(shard, List.of(campQueueOperation));
    }

}
