package ru.yandex.direct.core.copyentity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.util.BigDecimalComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.copyentity.testing.CopyAssertStrategies;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.service.CalloutService;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.BannerWithImage;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.service.BaseCampaignService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetService;
import ru.yandex.direct.core.entity.image.model.BannerImageFromPool;
import ru.yandex.direct.core.entity.image.repository.BannerImagePoolRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.promoextension.PromoExtensionService;
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.service.SitelinkSetService;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.core.testing.info.CampaignBidModifierInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.Entity;

import static java.util.Comparator.comparing;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.EntityAssertParameters.params;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;

@ParametersAreNonnullByDefault
@Component
public class CopyOperationAssert {
    public enum Mode {
        COPIED, // id меняются
        NOT_COPIED // id не меняются
    }

    static class EntityAssertParameters {
        private final EntityService service;
        private final RecursiveComparisonConfiguration compareStrategy;

        private EntityAssertParameters(EntityService service, RecursiveComparisonConfiguration compareStrategy) {
            this.service = service;
            this.compareStrategy = compareStrategy;
        }

        public static EntityAssertParameters params(
                EntityService service, RecursiveComparisonConfiguration compareStrategy) {
            return new EntityAssertParameters(service, compareStrategy);
        }
    }

    @Autowired
    private BaseCampaignService campaignService;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private VcardService vcardService;
    @Autowired
    private SitelinkSetService sitelinkSetService;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private RelevanceMatchService relMatchService;
    @Autowired
    private DynamicTextAdTargetService dynamicAdTargetService;
    @Autowired
    private CalloutService calloutService;
    @Autowired
    private PromoExtensionService promoExtensionService;

    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private BannerImagePoolRepository bannerImagePoolRepository;

    private ClientId clientId;
    private ClientId clientIdTo;
    private Long uid;

    private Map<Class, EntityAssertParameters> entityServices;

    public void init(ClientId clientIdFrom, ClientId clientIdTo, Long uid) {
        this.clientId = clientIdFrom;
        this.clientIdTo = clientIdTo;
        this.uid = uid;
        entityServices = Map.ofEntries(
                entry(BaseCampaign.class, params(campaignService,
                        CopyAssertStrategies.INSTANCE.getCAMPAIGN_COMPARE_STRATEGY())),
                entry(AdGroup.class, params(adGroupService,
                        CopyAssertStrategies.INSTANCE.getADGROUP_COMPARE_STRATEGY())),
                entry(BannerWithAdGroupId.class, params(bannerService,
                        CopyAssertStrategies.INSTANCE.getBANNER_COMPARE_STRATEGY())),
                entry(Vcard.class, params(vcardService, CopyAssertStrategies.INSTANCE.getVCARD_COMPARE_STRATEGY())),
                entry(SitelinkSet.class, params(sitelinkSetService,
                        CopyAssertStrategies.INSTANCE.getSITELINK_COMPARE_STRATEGY())),
                entry(BidModifier.class, params(bidModifierService,
                        CopyAssertStrategies.INSTANCE.getBID_MODIFIERS_COMPARE_STRATEGY())),
                entry(Keyword.class, params(keywordService,
                        CopyAssertStrategies.INSTANCE.getKEYWORD_COMPARE_STRATEGY())),
                entry(Retargeting.class, params(retargetingService,
                        CopyAssertStrategies.INSTANCE.getRETARGETING_COMPARE_STRATEGY())),
                entry(RelevanceMatch.class, params(relMatchService,
                        CopyAssertStrategies.INSTANCE.getRELEVANCE_MATCH_COMPARE_STRATEGY())),
                entry(DynamicAdTarget.class, params(dynamicAdTargetService,
                        CopyAssertStrategies.INSTANCE.getDYNAMIC_ADTARGET_COMPARE_STRATEGY())),
                entry(Callout.class, params(calloutService,
                        CopyAssertStrategies.INSTANCE.getCALLOUT_COMPARE_STRATEGY())),
                entry(PromoExtension.class, params(promoExtensionService,
                        CopyAssertStrategies.INSTANCE.getPROMO_EXTENSION_COMPARE_STRATEGY()))
        );
    }

    @SuppressWarnings("unchecked")
    public <TEntity extends Entity<TKey>, TKey extends Comparable> void assertEntitiesAreCopied(
            Class<TEntity> entityClass, Set<TKey> copiedEntityIds, List<TEntity> entities, Mode mode) {
        assertThat(entityServices).as("EntityServices does not contain EntityService<%s>", entityClass)
                .containsKey(entityClass);
        EntityAssertParameters params = entityServices.get(entityClass);

        assertThat(copiedEntityIds).hasSize(entities.size());

        List<TEntity> sortedEntities = StreamEx.of(entities)
                .sorted(comparing(Entity::getId))
                .toList();

        var sortedEntityIds = StreamEx.of(copiedEntityIds).sorted().toList();
        List<TEntity> copiedEntities = params.service.get(clientIdTo, uid, sortedEntityIds);

        assertSoftly(softly -> {
            softly.assertThat(copiedEntities).hasSize(entities.size());
            for (int i = 0; i < copiedEntities.size(); i++) {
                var copiedEntity = copiedEntities.get(0);
                var sourceEntity = sortedEntities.get(0);
                softly.assertThat(copiedEntity)
                        .describedAs("%2$s index %1$d should be similar to copied %2$s", i, entityClass)
                        .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                        .usingRecursiveComparison(params.compareStrategy)
                        .isEqualTo(sourceEntity);

                if (mode == Mode.NOT_COPIED) {
                    softly.assertThat(copiedEntity.getId())
                            .as("%2$s index %1$d should have the same id to copied %2$s",
                                    i, entityClass)
                            .isEqualTo(sourceEntity.getId());
                } else {
                    softly.assertThat(copiedEntity.getId())
                            .as("%2$s index %1$d should have different id to copied %2$s",
                                    i, entityClass)
                            .isNotEqualTo(sourceEntity.getId());
                }
            }
        });
    }

    public void assertCampaignIsCopied(Set<Long> copiedCampaignIds, Long sourceCampaignId) {
        assertCampaignIsCopied(copiedCampaignIds, sourceCampaignId, (c) -> { //nothing
        });
    }

    public void assertCampaignsAreCopied(Set<Long> copiedCampaignIds, Set<Long> sourceCampaignIds) {
        var sourceCampaigns = campaignService.get(clientId, uid, sourceCampaignIds);
        assertEntitiesAreCopied(BaseCampaign.class, copiedCampaignIds, sourceCampaigns, COPIED);
    }

    public void assertCampaignIsCopied(Set<Long> copiedCampaignIds, Long sourceCampaignId,
                                       Consumer<BaseCampaign> prepareSourceCampaignsForCompare) {
        assertThat(copiedCampaignIds).isNotEmpty();
        var sourceCampaign = campaignService.get(clientId, uid, List.of(sourceCampaignId)).get(0);
        prepareSourceCampaignsForCompare.accept(sourceCampaign);
        assertEntitiesAreCopied(BaseCampaign.class, copiedCampaignIds, List.of(sourceCampaign), COPIED);
    }

    public void assertAdGroupIsCopied(Set<Long> copiedAdGroupIds, Long sourceAdGroupId) {
        assertThat(copiedAdGroupIds).isNotEmpty();
        var sourceAdGroup = adGroupService.get(clientId, uid, List.of(sourceAdGroupId)).get(0);
        assertEntitiesAreCopied(AdGroup.class, copiedAdGroupIds, List.of(sourceAdGroup), COPIED);
    }


    public void assertCampaignBidModifierInfosAreCopied(Set<Long> copiedBidModifierIds,
                                                        List<CampaignBidModifierInfo> campaignBidModifierInfos) {
        var bidModifiers = StreamEx.of(campaignBidModifierInfos)
                .flatMap(info -> info.getBidModifiers().stream())
                .toList();
        assertCampaignBidModifiersAreCopied(copiedBidModifierIds, bidModifiers);
    }

    public void assertCampaignBidModifiersAreCopied(
            Set<Long> copiedBidModifierIds, List<BidModifier> originalCampaignBidModifiers) {
        assertThat(copiedBidModifierIds)
                .describedAs(
                        "copied campaigns bid modifiers count (%d) not equals to " +
                                "original campaigns bid modifiers count (%d)",
                        copiedBidModifierIds.size(), originalCampaignBidModifiers.size())
                .hasSize(originalCampaignBidModifiers.size());

        List<BidModifier> copiedBidModifiers = bidModifierService.get(clientIdTo, uid, copiedBidModifierIds);

        assertThat(copiedBidModifiers)
                .usingRecursiveFieldByFieldElementComparator(CopyAssertStrategies.INSTANCE.getBID_MODIFIERS_COMPARE_STRATEGY())
                .containsExactlyInAnyOrderElementsOf(originalCampaignBidModifiers);
    }

    public void assertBannerImagePoolsAreCopied(Set<Long> copiedBannerIds,
                                                List<String> sourceImageHashes,
                                                boolean idsTheSame) {
        assertThat(copiedBannerIds).hasSize(sourceImageHashes.size());

        int shardFrom = shardHelper.getShardByClientIdStrictly(clientId);
        var sourceImagesInPool = bannerImagePoolRepository.getBannerImageFromPoolsByHashes(shardFrom, clientId,
                sourceImageHashes);
        var sortedBannerImagesFromPool = StreamEx.of(sourceImagesInPool.values())
                .sorted(comparing(BannerImageFromPool::getId))
                .toList();

        var sortedBannerIds = StreamEx.of(copiedBannerIds).sorted().toList();
        var copiedBanners = bannerService.get(clientIdTo, uid, sortedBannerIds);
        var imageHashesBannerImage = StreamEx.of(copiedBanners)
                .select(BannerWithBannerImage.class)
                .map(BannerWithBannerImage::getImageHash)
                .toList();
        var imageHashesImage = StreamEx.of(copiedBanners).select(BannerWithImage.class)
                .map(BannerWithImage::getImageHash)
                .toList();
        var imageHashes = StreamEx.of(imageHashesBannerImage).append(imageHashesImage.stream()).toList();

        int shardTo = shardHelper.getShardByClientIdStrictly(clientIdTo);
        var imagesInPool = bannerImagePoolRepository.getBannerImageFromPoolsByHashes(shardTo, clientIdTo, imageHashes);
        var copiedImagesInPool = StreamEx.of(imagesInPool.values())
                .sorted(comparing(BannerImageFromPool::getId))
                .toList();

        assertSoftly(softly -> {
            softly.assertThat(copiedImagesInPool).hasSize(sortedBannerImagesFromPool.size());
            for (int i = 0; i < copiedImagesInPool.size(); i++) {
                var copiedBanner = copiedImagesInPool.get(i);
                var sourceBanner = sortedBannerImagesFromPool.get(i);
                softly.assertThat(copiedBanner)
                        .describedAs("BannerImageFromPool index {} should be similar to copied BannerImageFromPool", i)
                        .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                        .usingRecursiveComparison(CopyAssertStrategies.INSTANCE.getBANNER_IMAGE_POOL_COMPARE_STRATEGY())
                        .isEqualTo(sourceBanner);
                if (idsTheSame) {
                    softly.assertThat(copiedImagesInPool.get(0).getId())
                            .describedAs("BannerImageFromPool index {} should have the same id to copied " +
                                    "BannerImageFromPool", i)
                            .isEqualTo(sortedBannerImagesFromPool.get(0).getId());

                } else {
                    softly.assertThat(copiedImagesInPool.get(0).getId())
                            .describedAs("BannerImageFromPool index {} should have different id to copied " +
                                    "BannerImageFromPool", i)
                            .isNotEqualTo(sortedBannerImagesFromPool.get(0).getId());
                }
            }
        });
    }

    public void checkErrors(CopyResult copyResult) {
        assertThat(copyResult.getMassResult().getValidationResult().flattenErrors()).isEmpty();
    }
}
