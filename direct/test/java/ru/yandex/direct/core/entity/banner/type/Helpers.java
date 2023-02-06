package ru.yandex.direct.core.entity.banner.type;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupForBannerOperation;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerService;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.BannerWithCampaignId;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithStrategy;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.emptyMap;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Component
public class Helpers {

    @Autowired
    public FeatureService featureService;

    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public AdGroupRepository adGroupRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private BannersAddOperationContainerService bannersAddOperationContainerService;

    @Deprecated // не создавайте в тестах контейнер вручную, такие тесты тяжело поддерживать
    public BannersAddOperationContainer createValidationContainer(ClientInfo clientInfo,
                                                                  List<? extends Banner> banners) {
        Set<String> clientFeatures = featureService.getEnabledForClientId(clientInfo.getClientId());
        BannersAddOperationContainerImpl validationContainer = new BannersAddOperationContainerImpl(clientInfo.getShard(),
                clientInfo.getUid(), clientInfo.getClient().getRole(), clientInfo.getClientId(), clientInfo.getUid(),
                rbacService.getChiefByClientId(clientInfo.getClientId()),
                null, clientFeatures, ModerationMode.FORCE_MODERATE, false,
                false, false);

        Set<Long> adGroupIds = StreamEx.of(banners)
                .select(BannerWithAdGroupId.class)
                .map(BannerWithAdGroupId::getAdGroupId)
                .nonNull()
                .toSet();
        Map<Long, AdGroup> adGroups = adGroupIds.isEmpty()
                ? emptyMap()
                : listToMap(adGroupRepository.getAdGroups(clientInfo.getShard(), adGroupIds), AdGroup::getId);
        Map<Integer, AdGroupForBannerOperation> adGroupsByForOperation = EntryStream.of(banners)
                .selectValues(BannerWithAdGroupId.class)
                .mapValues(BannerWithAdGroupId::getAdGroupId)
                .nonNullValues()
                .mapValues(adGroups::get)
                .selectValues(AdGroupForBannerOperation.class)
                .toMap();
        validationContainer.setIndexToAdGroupMap(adGroupsByForOperation);

        Set<Long> campaignIds = StreamEx.of(banners)
                .select(BannerWithCampaignId.class)
                .map(BannerWithCampaignId::getCampaignId)
                .nonNull()
                .toSet();
        Map<Long, CampaignWithStrategy> campaignsWithStrategy = campaignIds.isEmpty()
                ? emptyMap()
                : listToMap(campaignTypedRepository
                        .getSafely(clientInfo.getShard(), campaignIds, CampaignWithStrategy.class),
                CampaignWithStrategy::getId);
        validationContainer.setCampaignIdToCampaignWithStrategyMap(campaignsWithStrategy);
        Map<Long, CommonCampaign> commonCampaignsById = listToMap(campaignTypedRepository
                .getSafely(clientInfo.getShard(), campaignIds, CommonCampaign.class), CommonCampaign::getId);
        Map<Integer, CommonCampaign> commonCampaignsByIndex = EntryStream.of(banners)
                .selectValues(BannerWithCampaignId.class)
                .mapValues(BannerWithCampaignId::getCampaignId)
                .nonNullValues()
                .mapValues(commonCampaignsById::get)
                .toMap();
        validationContainer.setIndexToCampaignMap(commonCampaignsByIndex);

        IdentityHashMap<Banner, Integer> bannerToIndexMap = EntryStream.of(banners)
                .mapValues(b -> (Banner) b)
                .invert()
                .toCustomMap(IdentityHashMap::new);
        validationContainer.setBannerToIndexMap(bannerToIndexMap);
        bannersAddOperationContainerService.fillSitelinkSets(validationContainer, banners);
        bannersAddOperationContainerService.fillVcards(validationContainer, banners);

        return validationContainer;
    }

    public static <T extends Banner> List<ModelChanges<T>> createModelChanges(List<T> banners) {
        return mapList(banners, banner -> new ModelChanges<T>(banner.getId(), (Class<T>) banner.getClass()));
    }

    public static <T extends Banner> List<AppliedChanges<T>> createAppliedChanges(List<T> models,
                                                                                  List<ModelChanges<T>> changes) {
        List<AppliedChanges<T>> result = new ArrayList<>();
        for (int i = 0; i < models.size(); i++) {
            result.add(changes.get(i).applyTo(models.get(i)));
        }
        return result;
    }
}
