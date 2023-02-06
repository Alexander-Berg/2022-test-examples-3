package ru.yandex.direct.core.testing.steps;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.bids.container.ShowConditionSelectionCriteria;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.operation.AddedModelId;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class RelevanceMatchSteps {
    @Autowired
    DslContextProvider dslContextProvider;
    @Autowired
    RelevanceMatchRepository relevanceMatchRepository;
    @Autowired
    RelevanceMatchService relevanceMatchService;

    public Long addDefaultRelevanceMatchToAdGroup(AdGroupInfo adGroupInfo) {
        return addRelevanceMatchToAdGroup(adGroupInfo, BigDecimal.TEN, BigDecimal.TEN.add(BigDecimal.ZERO));
    }

    public Long addRelevanceMatchToAdGroup(AdGroupInfo adGroupInfo, BigDecimal price, BigDecimal priceContext) {
        List<AddedModelId> ids = relevanceMatchRepository.addRelevanceMatches(
                dslContextProvider.ppc(adGroupInfo.getShard()).configuration(),
                adGroupInfo.getClientId(),
                singletonList(createRelevanceMatch(adGroupInfo, price, priceContext)),
                singleton(adGroupInfo.getAdGroupId()));
        return ids.get(0).getId();
    }

    public RelevanceMatch addDefaultRelevanceMatch(AdGroupInfo adGroupInfo) {
        Long relevanceMatchId = addDefaultRelevanceMatchToAdGroup(adGroupInfo);
        return relevanceMatchRepository.getRelevanceMatchesByIds(
                adGroupInfo.getShard(), adGroupInfo.getClientId(), singleton(relevanceMatchId))
                .get(relevanceMatchId);
    }

    /**
     * Для нескольких групп добавляет автотаргетинги одним запросом в БД.
     * В качестве клиента и шарда используются клиент и шард, определённые в первой по списку группе.
     */
    public List<RelevanceMatch> addDefaultRelevanceMatches(List<AdGroupInfo> adGroupInfos) {
        if (adGroupInfos.isEmpty()) {
            return emptyList();
        }

        List<RelevanceMatch> relevanceMatches = StreamEx.of(adGroupInfos)
                .map(this::getDefaultRelevanceMatch)
                .toList();

        ClientInfo clientInfo = adGroupInfos.get(0).getClientInfo();
        Set<Long> adGroupIds = StreamEx.of(adGroupInfos).map(AdGroupInfo::getAdGroupId).toSet();
        List<AddedModelId> addedModelIds = relevanceMatchRepository.addRelevanceMatches(
                dslContextProvider.ppc(clientInfo.getShard()).configuration(),
                clientInfo.getClientId(),
                relevanceMatches,
                adGroupIds);

        List<Long> relevanceMatchesIds = StreamEx.of(addedModelIds).map(AddedModelId::getId).toList();
        ShowConditionSelectionCriteria showConditionSelectionCriteria =
                new ShowConditionSelectionCriteria().withShowConditionIds(relevanceMatchesIds);
        return relevanceMatchRepository.getRelevanceMatches(clientInfo.getShard(), clientInfo.getClientId(),
                showConditionSelectionCriteria, LimitOffset.maxLimited(), false);
    }

    public List<RelevanceMatch> getRelevanceMatchesByAdGroupId(
            int shard, ClientId clientId, Long adGroupId, boolean withDeleted) {
        ShowConditionSelectionCriteria showConditionSelectionCriteria =
                new ShowConditionSelectionCriteria().withAdGroupIds(List.of(adGroupId));
        return relevanceMatchRepository.getRelevanceMatches(
                        shard, clientId, showConditionSelectionCriteria, LimitOffset.maxLimited(), withDeleted);
    }

    public List<Long> addRelevanceMatchToAdGroup(List<RelevanceMatch> relevanceMatches, AdGroupInfo adGroupInfo) {
        List<AddedModelId> ids = relevanceMatchRepository.addRelevanceMatches(
                dslContextProvider.ppc(adGroupInfo.getShard()).configuration(),
                adGroupInfo.getClientId(),
                relevanceMatches,
                singleton(adGroupInfo.getAdGroupId()));
        return mapList(ids, AddedModelId::getId);
    }

    public RelevanceMatch getDefaultRelevanceMatch(AdGroupInfo adGroupInfo) {
        return createRelevanceMatch(adGroupInfo, BigDecimal.TEN, BigDecimal.TEN.add(BigDecimal.ONE));
    }

    private RelevanceMatch createRelevanceMatch(AdGroupInfo adGroupInfo, BigDecimal price, BigDecimal priceContext) {
        return createRelevanceMatch(adGroupInfo, price, priceContext, DEFAULT_AUTOBUDGET_PRIORITY);
    }

    public RelevanceMatch createRelevanceMatch(
            AdGroupInfo adGroupInfo, BigDecimal price, BigDecimal priceContext, Integer autobudgetPriority) {
        return new RelevanceMatch()
                .withPrice(price)
                .withPriceContext(priceContext)
                .withAutobudgetPriority(autobudgetPriority)
                .withHrefParam1("href_val_01")
                .withHrefParam2("href_val_02")
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLastChangeTime(now())
                .withRelevanceMatchCategories(emptySet());
    }

    public RelevanceMatch getDefaultRelevanceMatch(ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo<?> adGroupInfo) {
        return createRelevanceMatch(adGroupInfo, BigDecimal.TEN, BigDecimal.TEN.add(BigDecimal.ONE));
    }

    private RelevanceMatch createRelevanceMatch(ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo<?> adGroupInfo,
                                                BigDecimal price, BigDecimal priceContext) {
        return new RelevanceMatch()
                .withPrice(price)
                .withPriceContext(priceContext)
                .withAutobudgetPriority(3)
                .withHrefParam1("href_val_01")
                .withHrefParam2("href_val_02")
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLastChangeTime(now());
    }

    public void setRelevanceMatchRepository(RelevanceMatchRepository relevanceMatchRepository) {
        this.relevanceMatchRepository = relevanceMatchRepository;
    }
}
