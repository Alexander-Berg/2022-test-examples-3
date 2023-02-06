package ru.yandex.direct.core.testing.data;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.EntryStream;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupForBannerOperation;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMobileContent;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithStrategy;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

@Deprecated // не используйте в тестах контейнеры, такие тесты сложно поддерживать
public class TestBannerValidationContainers {

    public static BannersAddOperationContainer defaultBannerValidationContainer() {
        return newBannerValidationContainer().build();
    }

    public static BannersAddOperationContainer defaultBannerValidationContainer(ClientInfo clientInfo) {
        return newBannerValidationContainer()
                .withClientInfo(clientInfo)
                .build();
    }

    public static BannersUpdateOperationContainer defaultBannerUpdateValidationContainer(ClientInfo clientInfo) {
        return newBannerValidationContainer()
                .withClientInfo(clientInfo)
                .buildUpdate();
    }

    public static BannersAddOperationContainer defaultBannerValidationContainer(AdGroupInfo adGroupInfo) {
        return newBannerValidationContainer()
                .withAdGroupInfo(adGroupInfo)
                .build();
    }

    public static Builder newBannerValidationContainer() {
        return new Builder();
    }

    public static class Builder {

        private int shard = 1;
        private Long operatorUid = 2L;
        private RbacRole operatorRole = RbacRole.CLIENT;
        private ClientId clientId = ClientId.fromLong(3L);
        private Long clientUid = 4L;
        private Long chiefUid = 5L;
        private Long clientRegionId = null;
        private Set<String> clientEnabledFeatures = emptySet();
        private ModerationMode moderationMode = ModerationMode.DEFAULT;
        private boolean isOperatorInternal = false;
        private boolean isPartOfComplexOperation = false;
        private boolean validateTrackingHrefMacros = true;
        private Map<Integer, AdGroupForBannerOperation> indexToAdGroupForOperationMap = emptyMap();
        private Map<Integer, ContentPromotionAdgroupType> indexToContentPromotionAdgroupTypeMap = emptyMap();
        private Map<Integer, CommonCampaign> indexToCampaignMap = emptyMap();
        private Map<Long, CampaignWithStrategy> campaignIdToCampaignWithStrategyMap = emptyMap();
        private Map<Long, CampaignWithMobileContent> campaignIdToCampaignWithMobileContentMap = emptyMap();
        private Map<Long, List<MobileAppTracker>> mobileAppIdToTrackersMap = emptyMap();
        private IdentityHashMap<Banner, Integer> bannerToIndexMap = new IdentityHashMap<>();
        private IdentityHashMap<ModelChanges<Banner>, Integer> modelChangesToIndexMap = new IdentityHashMap<>();
        private Map<Long, BannersBannerType> bannerTypes = new HashMap<>();

        public Builder withShard(int shard) {
            this.shard = shard;
            return this;
        }

        public Builder withOperatorUid(Long operatorUid) {
            this.operatorUid = operatorUid;
            return this;
        }

        public Builder withOperatorRole(RbacRole operatorRole) {
            this.operatorRole = operatorRole;
            return this;
        }

        public Builder withClientId(ClientId clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder withClientUid(Long clientUid) {
            this.clientUid = clientUid;
            return this;
        }

        public Builder withChiefUid(Long chiefUid) {
            this.chiefUid = chiefUid;
            return this;
        }

        public Builder withClientRegionId(Long clientRegionId) {
            this.clientRegionId = clientRegionId;
            return this;
        }

        public Builder withClientEnabledFeatures(Set<String> clientEnabledFeatures) {
            this.clientEnabledFeatures = clientEnabledFeatures;
            return this;
        }

        public Builder withModerationMode(ModerationMode moderationMode) {
            this.moderationMode = moderationMode;
            return this;
        }

        public Builder withOperatorInternal(boolean operatorInternal) {
            isOperatorInternal = operatorInternal;
            return this;
        }

        public Builder withPartOfComplexOperation(boolean partOfComplexOperation) {
            isPartOfComplexOperation = partOfComplexOperation;
            return this;
        }

        public Builder withValidateTrackingHrefMacros(boolean validateTrackingHrefMacros) {
            this.validateTrackingHrefMacros = validateTrackingHrefMacros;
            return this;
        }

        public Builder withIndexToAdGroupForOperationMap(
                Map<Integer, AdGroupForBannerOperation> indexToAdGroupForOperationMap) {
            this.indexToAdGroupForOperationMap = indexToAdGroupForOperationMap;
            return this;
        }

        public Builder withIndexToContentPromotionAdgroupTypeMap(
                Map<Integer, ContentPromotionAdgroupType> indexToContentPromotionAdgroupTypeMap) {
            this.indexToContentPromotionAdgroupTypeMap = indexToContentPromotionAdgroupTypeMap;
            return this;
        }

        public Builder withIndexToCampaignMap(Map<Integer, CommonCampaign> indexToCampaignMap) {
            this.indexToCampaignMap = indexToCampaignMap;
            return this;
        }

        public Builder withCampaignIdToCampaignWithStrategyMap(
                Map<Long, CampaignWithStrategy> campaignIdToCampaignWithStrategyMap) {
            this.campaignIdToCampaignWithStrategyMap = campaignIdToCampaignWithStrategyMap;
            return this;
        }

        public Builder withCampaignIdToCampaignWithMobileContentMap(
                Map<Long, CampaignWithMobileContent> campaignIdToCampaignWithMobileContentMap) {
            this.campaignIdToCampaignWithMobileContentMap = campaignIdToCampaignWithMobileContentMap;
            return this;
        }

        public Builder withBannerToIndexMap(Map<Banner, Integer> bannerToIndexMap) {
            this.bannerToIndexMap = EntryStream.of(bannerToIndexMap).toCustomMap(IdentityHashMap::new);
            return this;
        }

        public Builder withMobileAppIdToTrackersMap(Map<Long, List<MobileAppTracker>> mobileAppIdToTrackersMap) {
            this.mobileAppIdToTrackersMap = mobileAppIdToTrackersMap;
            return this;
        }

        public Builder withModelChangesToIndexMap(Map<ModelChanges<Banner>, Integer> modelChangesToIndexMap) {
            this.modelChangesToIndexMap = EntryStream.of(modelChangesToIndexMap).toCustomMap(IdentityHashMap::new);
            return this;
        }

        public Builder withClientInfo(ClientInfo clientInfo) {
            return withShard(clientInfo.getShard())
                    .withOperatorUid(clientInfo.getUid())
                    .withOperatorRole(clientInfo.getClient().getRole())
                    .withClientId(clientInfo.getClientId())
                    .withClientUid(clientInfo.getUid());
        }

        public Builder withAdGroupInfo(AdGroupInfo adGroupInfo) {
            return withShard(adGroupInfo.getShard())
                    .withOperatorUid(adGroupInfo.getUid())
                    .withOperatorRole(adGroupInfo.getClientInfo().getClient().getRole())
                    .withClientId(adGroupInfo.getClientId())
                    .withClientUid(adGroupInfo.getUid());
        }

        public Builder withBannerType(Long id, BannersBannerType bannerType) {
            bannerTypes.put(id, bannerType);
            return this;
        }

        public BannersAddOperationContainerImpl build() {
            var container = new BannersAddOperationContainerImpl(shard, operatorUid, operatorRole, clientId, clientUid,
                    chiefUid, clientRegionId, clientEnabledFeatures, moderationMode, isOperatorInternal,
                    isPartOfComplexOperation, validateTrackingHrefMacros);
            container.setIndexToAdGroupMap(indexToAdGroupForOperationMap);
            container.setIndexToContentPromotionAdgroupTypeMap(() -> indexToContentPromotionAdgroupTypeMap);
            container.setIndexToCampaignMap(indexToCampaignMap);
            container.setCampaignIdToCampaignWithStrategyMap(campaignIdToCampaignWithStrategyMap);
            container.setCampaignIdToCampaignWithMobileContentMap(campaignIdToCampaignWithMobileContentMap);
            container.setMobileAppIdToTrackersMap(mobileAppIdToTrackersMap);
            container.setBannerToIndexMap(bannerToIndexMap);
            return container;
        }

        public BannersUpdateOperationContainer buildUpdate() {
            var container = new BannersUpdateOperationContainerImpl(shard, operatorUid, operatorRole, clientId,
                    clientUid, chiefUid, clientRegionId, clientEnabledFeatures, moderationMode,
                    isOperatorInternal, isPartOfComplexOperation, validateTrackingHrefMacros);
            container.setIndexToAdGroupMap(indexToAdGroupForOperationMap);
            container.setIndexToContentPromotionAdgroupTypeMap(() -> indexToContentPromotionAdgroupTypeMap);
            container.setIndexToCampaignMap(indexToCampaignMap);
            container.setCampaignIdToCampaignWithStrategyMap(campaignIdToCampaignWithStrategyMap);
            container.setCampaignIdToCampaignWithMobileContentMap(campaignIdToCampaignWithMobileContentMap);
            container.setMobileAppIdToTrackersMap(mobileAppIdToTrackersMap);
            container.setBannerToIndexMap(bannerToIndexMap);
            bannerTypes.forEach((x, y) -> container.setBannerType(x, y));
            return container;
        }

    }
}
