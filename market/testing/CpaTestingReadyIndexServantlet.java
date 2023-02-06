package ru.yandex.market.partner.testing;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.Servantlet;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.ds.info.UniShopInformation;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.indexer.db.generation.IdxGenerationService;
import ru.yandex.market.core.indexer.db.meta.GenerationMetaService;
import ru.yandex.market.core.indexer.model.FeedStatus;
import ru.yandex.market.core.indexer.model.IndexerType;
import ru.yandex.market.core.indexer.model.ReturnCode;
import ru.yandex.market.core.moderation.CpaTestingIndex;
import ru.yandex.market.core.moderation.feed.FeedIndexationResults;
import ru.yandex.market.core.moderation.feed.FeedIndexingResult;
import ru.yandex.market.core.moderation.feed.IndexingResultRepository;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.xml.HierarchicalWriter;
import ru.yandex.market.core.xml.MarshallingContext;
import ru.yandex.market.core.xml.MarshallingUtils;
import ru.yandex.market.core.xml.SelfConverter;
import ru.yandex.market.partner.servant.DataSourceable;
import ru.yandex.market.security.model.Uidable;

import static ru.yandex.market.core.param.model.ParamType.CPA_IS_API_PARAMS_READY;
import static ru.yandex.market.core.param.model.ParamType.CPA_IS_PARTNER_INTERFACE;
import static ru.yandex.market.core.param.model.ParamType.PARTNER_SETTINGS_INPUT_CONFIRM;

public class CpaTestingReadyIndexServantlet<Q extends ServRequest & DataSourceable & Uidable>
        implements Servantlet<Q, ServResponse> {

    private final IndexingResultRepository mainIndexingResultRepository;
    private final IndexingResultRepository sandboxIndexingResultRepository;
    private final FeatureService featureService;
    private final FeedService feedService;
    private final IdxGenerationService idxGenerationService;
    private final SandboxRepository sandboxRepository;
    private final ParamService paramService;

    @Autowired
    public CpaTestingReadyIndexServantlet(IndexingResultRepository mainIndexingResultRepository,
                                          IndexingResultRepository sandboxIndexingResultRepository,
                                          FeatureService featureService,
                                          FeedService feedService,
                                          IdxGenerationService idxGenerationService,
                                          SandboxRepository sandboxRepository,
                                          ParamService paramService
    ) {
        this.mainIndexingResultRepository = mainIndexingResultRepository;
        this.sandboxIndexingResultRepository = sandboxIndexingResultRepository;
        this.featureService = featureService;
        this.feedService = feedService;
        this.idxGenerationService = idxGenerationService;
        this.sandboxRepository = sandboxRepository;
        this.paramService = paramService;
    }

    private static boolean isShop(CampaignType type) {
        return type == CampaignType.SHOP;
    }

    @Override
    public void process(final Q request, final ServResponse response) {
        PartnerId partner = request.getPartnerId();
        if (!isValidDatasourceId(partner.toLong())) {
            return;
        }
        Index index = isShop(partner.type()) ? getIndex(partner.getDatasourceId())
                : getSupplierDropshipIndex(partner.getSupplierId());
        response.addData(index);
    }

    /**
     * @return если у магазина есть незаполненная обязательная для размещения информация
     * - (NONE, MISSED_PARAMS, дополнительная информация),<br>
     * в противном случае если фид в тестовом индексе, фид не сломан и в нем есть cpa-предложения - (SANDBOX)<br>,
     * в противном случае если фид в продовом индексе, фид не сломан и в нем есть cpa-предложения - (MAIN),<br>
     * в противном случае если фид в данный момент загружается в тестовый индекс - (NONE, LOADING),<br>
     * в противном случае если фид есть в каком-либо индексе, но он не содержит cpa-предложений
     * - (NONE, NO_CPA_OFFERS),<br>
     * в противном случае если фид есть в каком-либо индексе, но он сломан
     * - (NONE, BROKEN_FEED),<br>
     * в противном случае возвращаем (NONE)
     */
    private Index getIndex(long shopId) {
        CpaTestingIndex index;
        List<FeedStatus> feedStatuses;

        Collection<FeatureCutoffInfo> cutoffs = featureService.getCutoffs(shopId,
                FeatureType.MARKETPLACE_SELF_DELIVERY);
        cutoffs.addAll(featureService.getCutoffs(shopId, FeatureType.MARKETPLACE));

        boolean hasCutoffsRemovingFromIndex = cutoffs.stream()
                .anyMatch(FeatureCutoffInfo::restrictsIndexation);

        Collection<Long> shopFeedIds = feedService.getVirtualDataFeeds(shopId, FeedSiteType.MARKET);

        // если нет катоффов, что выбивают из индекса -
        // значит анализируем продовый индекс магазина
        if (!hasCutoffsRemovingFromIndex) {
            index = CpaTestingIndex.MAIN;
            feedStatuses = idxGenerationService.getFeedStatuses(shopFeedIds, IndexerType.MAIN);
            // фича не в SUCCESS - анализируем индекс в ПШ
        } else {
            index = CpaTestingIndex.SANDBOX;
            feedStatuses = idxGenerationService.getFeedStatuses(shopFeedIds, IndexerType.PLANESHIFT);
        }

        List<FeedStatus> feedsInIndex = feedStatuses.stream()
                .filter(FeedStatus::isInIndex)
                .collect(Collectors.toList());

        /*
        Есть фиды в индексе
        Почему недостаточно проверки на isInIndex? Индексатор дает магазину время исправится перед тем как выбивать
        магазин из индекса.
         */
        if (!feedsInIndex.isEmpty() && hasCpaOffers(feedsInIndex)) {
            return new Index(index);
        }

        // индексация хотя бы одного фида была ошибочна
        if (feedStatuses.stream()
                .map(FeedStatus::getLastFullGenReturnCode)
                .anyMatch(c -> c.worseThan(ReturnCode.WARNING))) {
            return new Index(CpaTestingIndex.NONE, new Details(Reason.BROKEN_FEED));
        }

        // нет CPA офферов в фидах
        if (!feedsInIndex.isEmpty() && !hasCpaOffers(feedsInIndex)) {
            return new Index(CpaTestingIndex.NONE, new Details(Reason.NO_CPA_OFFERS));
        }

        // магазин недонастроен
        if (!isDsbsFullyConfigured(shopId)) {
            return new Index(CpaTestingIndex.NONE, new Details(Reason.MISSED_PARAMS));
        }

        /*
        Магазин грузится в индекс.
        Если фича нет катоффов, выбивающих из индекса, то он точно грузится в индекс.
        Если фича не в саксесс, то смотрим на datasources_in_testing.
         */
        if (!hasCutoffsRemovingFromIndex || isLoadingToPlainshift(shopId)) {
            return new Index(CpaTestingIndex.NONE, new Details(Reason.LOADING));
        }

        return new Index(CpaTestingIndex.NONE);
    }

    /**
     * @return если у поставщика есть незаполненная обязательная для размещения информация
     * - (NONE, MISSED_PARAMS, дополнительная информация),<br>
     * в противном случае если фид в тестовом индексе, фид не сломан и в нем есть предложения - (SANDBOX)<br>,
     * в противном случае если фид в продовом индексе, фид не сломан и в нем есть предложения - (MAIN),<br>
     * в противном случае если фид в данный момент загружается в тестовый индекс - (NONE, LOADING),<br>
     * в противном случае если фид есть в каком-либо индексе, но он сломан
     * - (NONE, BROKEN_FEED),<br>
     * в противном случае возвращаем (NONE)
     */
    private Index getSupplierDropshipIndex(long supplierId) {
        //TODO дополнить при необходимости нормальной проверкой
        // Вся ли необходимая информация заполнена у поставщика
        //return new Index(CpaTestingIndex.NONE, new Details(Reason.MISSED_PARAMS, missedDatasourceInfo));

        Index mainIndex = getIndexResult(mainIndexingResultRepository::loadSupplierResults,
                supplierId, CpaTestingIndex.MAIN);
        if (mainIndex.type == CpaTestingIndex.MAIN ||
                (mainIndex.getDetails() != null && mainIndex.getDetails().getReason() != null)) {
            return getWithDetailsForDropship(mainIndex, supplierId);
        }

        Index sbxIndex = getIndexResult(sandboxIndexingResultRepository::loadSupplierResults, supplierId,
                CpaTestingIndex.SANDBOX);
        return getWithDetailsForDropship(sbxIndex, supplierId);
    }

    private Index getWithDetailsForDropship(Index indx, long supplierId) {
        if (indx.getType() == CpaTestingIndex.NONE
                && (indx.getDetails() == null || indx.getDetails().getReason() == null)) {
            // Корректного фида нет ни в одном индексе
            // Проверяем, может ли загружаться поставщик в тестовый индекс
            // {@link Feature#DROPSHIP} в статусе NEW
            ShopFeature shopFeature = featureService.getFeature(supplierId, FeatureType.DROPSHIP);
            if (ParamCheckStatus.NEW == shopFeature.getStatus()
                    || ParamCheckStatus.SUCCESS == shopFeature.getStatus()) {
                return new Index(CpaTestingIndex.NONE, new Details(Reason.LOADING));
            }
        }
        return indx;
    }

    private Index getIndexResult(Function<Long, FeedIndexationResults> shopResultLoader, long shopId,
                                 CpaTestingIndex indexType) {
        FeedIndexationResults shopResults = shopResultLoader.apply(shopId);
        Reason reason = null;
        if (shopResults.isEmpty()) {
            return new Index(CpaTestingIndex.NONE);
        } else {
            // Фид уже в тестовом индексе. Проверяем его состояние.
            if (shopResults.hasBrokenFeeds(GenerationMetaService.ALLOWED_SITE_TYPES)) {
                reason = Reason.BROKEN_FEED;
            }

            if (shopResults.getResults().stream().map(FeedIndexingResult::getCpaOfferCount).noneMatch(v -> v > 0)) {
                reason = Reason.NO_CPA_OFFERS;
            }

            return reason == null ? new Index(indexType) : new Index(CpaTestingIndex.NONE, new Details(reason));
        }
    }

    private boolean isValidDatasourceId(long datasourceId) {
        return datasourceId > 0;
    }

    private boolean hasCpaOffers(List<FeedStatus> feedStatuses) {
        return feedStatuses.stream()
                .map(FeedStatus::getFullGenerationCpaRealOffersCount)
                .anyMatch(v -> v > 0);
    }

    /**
     * @param shopId - идентификатор партнера
     * @return true - если ДСБС партнер ждет загрузки в ПШ, false в противном случае
     */
    private boolean isLoadingToPlainshift(long shopId) {
        return sandboxRepository.load(shopId)
                .stream()
                .anyMatch(SandboxState::isWaitingForIndex);
    }

    /**
     * @param shopId - идентификатор партнера
     * @return true - если ДСБС партнер полностью настроен, false в противном случае
     */
    private boolean isDsbsFullyConfigured(long shopId) {
        // сконфигурирован способ обработки заказв
        return isOrderProcessingConfigured(shopId) &&
                // есть доставка и фид = можем включить фичу
                featureService.getDescription(FeatureType.MARKETPLACE_SELF_DELIVERY)
                        .getPrecondition()
                        .evaluate(shopId)
                        .canEnable();
    }

    private boolean isOrderProcessingConfigured(long shopId) {
        MultiMap<ParamType, ParamValue> params = paramService.getParams(shopId, Set.of(CPA_IS_PARTNER_INTERFACE,
                CPA_IS_API_PARAMS_READY,
                PARTNER_SETTINGS_INPUT_CONFIRM));

        boolean areParamsConfigured = getParamBooleanValue(params, CPA_IS_PARTNER_INTERFACE) ||
                getParamBooleanValue(params, CPA_IS_API_PARAMS_READY);

        return areParamsConfigured && getParamBooleanValue(params, PARTNER_SETTINGS_INPUT_CONFIRM);
    }

    private boolean getParamBooleanValue(MultiMap<ParamType, ParamValue> shopParams, ParamType paramType) {
        ParamValue head = shopParams.head(paramType);
        return head != null ? head.getValueAsBoolean() : false;
    }

    enum Reason {
        LOADING,
        BROKEN_FEED,
        MISSED_PARAMS,
        NO_CPA_OFFERS
    }

    /**
     * DTO для ответа.
     */
    static class Index {
        private CpaTestingIndex type;
        private Details details;

        Index(CpaTestingIndex type) {
            this.type = type;
        }

        Index(CpaTestingIndex type, Details details) {
            this(type);
            this.details = details;
        }

        public CpaTestingIndex getType() {
            return type;
        }

        @SuppressWarnings("unused")
        public Details getDetails() {
            return details;
        }
    }

    @SuppressWarnings("unused")
    static class Details implements SelfConverter {
        private Reason reason;
        private List<UniShopInformation> missedDatasourceInfo;

        Details(Reason reason) {
            this.reason = reason;
        }

        Details(Reason reason, List<UniShopInformation> missedDatasourceInfo) {
            this(reason);
            this.missedDatasourceInfo = missedDatasourceInfo;
        }

        public Reason getReason() {
            return reason;
        }

        public List<UniShopInformation> getMissedDatasourceInfo() {
            return missedDatasourceInfo;
        }

        @Override
        public void convert(HierarchicalWriter writer, MarshallingContext context) {
            writer.setAttribute("reason", MarshallingUtils.convertToStandard(reason.name()));
            context.convertAnother(missedDatasourceInfo);
        }
    }
}
