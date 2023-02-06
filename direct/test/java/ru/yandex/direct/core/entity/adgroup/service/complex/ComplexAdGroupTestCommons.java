package ru.yandex.direct.core.entity.adgroup.service.complex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableSet;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting;
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;


public class ComplexAdGroupTestCommons {
    private static final CompareStrategy RETARGETINGS_MATCHER =
            DefaultCompareStrategies.onlyFields(newPath(Retargeting.PRICE_CONTEXT.name()),
                    newPath(Retargeting.RETARGETING_CONDITION_ID.name()))
                    .forFields(newPath(Retargeting.PRICE_CONTEXT.name())).useDiffer(new BigDecimalDiffer());

    private KeywordRepository keywordRepository;
    private RelevanceMatchRepository relevanceMatchRepository;
    private OfferRetargetingRepository offerRetargetingRepository;
    private RetargetingService retargetingService;
    private BidModifierRepository bidModifierRepository;

    public ComplexAdGroupTestCommons(KeywordRepository keywordRepository,
                                     RelevanceMatchRepository relevanceMatchRepository,
                                     OfferRetargetingRepository offerRetargetingRepository,
                                     RetargetingService retargetingService,
                                     BidModifierRepository bidModifierRepository) {
        this.keywordRepository = keywordRepository;
        this.relevanceMatchRepository = relevanceMatchRepository;
        this.offerRetargetingRepository = offerRetargetingRepository;
        this.retargetingService = retargetingService;
        this.bidModifierRepository = bidModifierRepository;
    }

    public void checkKeywords(List<Keyword> keywords, Long adGroupId, int shard) {
        List<Keyword> actualKeywords =
                keywordRepository.getKeywordsByAdGroupId(shard, adGroupId);
        List<String> actualPhrases = mapList(actualKeywords, Keyword::getPhrase);

        List<Keyword> expectedKeywords = nvl(keywords, emptyList());
        List<String> expectedPhrases = mapList(expectedKeywords, Keyword::getPhrase);
        assertThat("в группе правильные фразы", actualPhrases,
                containsInAnyOrder(expectedPhrases.toArray()));
    }

    public void checkRelevanceMatches(List<RelevanceMatch> relevanceMatches, Long adGroupId, ClientId clientId,
                                      int shard) {
        List<RelevanceMatch> actualRelevanceMatches = getRelevanceMatchesOfAdGroup(adGroupId, clientId, shard);
        List<RelevanceMatch> expectedRelevanceMatches = nvl(relevanceMatches, emptyList());

        assertThat("в группе верное количество бесфразных таргетингов",
                actualRelevanceMatches,
                hasSize(expectedRelevanceMatches.size()));
    }

    public void checkOfferRetargetings(List<OfferRetargeting> offerRetargetings, Long adGroupId, ClientId clientId,
                                       int shard) {
        List<OfferRetargeting> actualOfferRetargetings = getOfferRetargetingsOfAdGroup(adGroupId, clientId, shard);
        List<OfferRetargeting> expectedOfferRetargetings = nvl(offerRetargetings, emptyList());
        assertThat("в группе верное количество офферных ретаргетингов",
                actualOfferRetargetings,
                hasSize(expectedOfferRetargetings.size()));
    }

    public ArrayList<RelevanceMatch> getRelevanceMatchesOfAdGroup(Long adGroupId, ClientId clientId, int shard) {
        return new ArrayList<>(relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(shard, clientId, singleton(adGroupId)).values());
    }

    public ArrayList<OfferRetargeting> getOfferRetargetingsOfAdGroup(Long adGroupId, ClientId clientId, int shard) {
        return new ArrayList<>(offerRetargetingRepository.
                getOfferRetargetingsByAdGroupIds(shard, clientId, singleton(adGroupId)).values());
    }

    public void checkRetargetings(List<TargetInterest> targetInterests, Long adGroupId, ClientId clientId, int shard) {
        List<TargetInterest> retargetings = retargetingService
                .getTargetInterestsWithInterestByAdGroupIds(singletonList(adGroupId), clientId, shard);

        List<TargetInterest> expectedRetargetings = nvl(targetInterests, emptyList());
        assertThat("в группу добавились правильные ретаргетинги", retargetings, containsInAnyOrder(
                mapList(expectedRetargetings, r -> beanDiffer(r).useCompareStrategy(RETARGETINGS_MATCHER))));
    }

    public void checkBidModifiers(ComplexBidModifier complexBidModifier, Long adGroupId, Long campaignId, int shard) {
        List<BidModifier> actualModifiers = bidModifierRepository
                .getByAdGroupIds(shard, singletonMap(adGroupId, campaignId), ALL_TYPES,
                        ImmutableSet.of(BidModifierLevel.ADGROUP));
        List<BidModifier> expectedModifiers = getFlatBidModifiers(complexBidModifier);

        actualModifiers.sort(Comparator.comparing(b -> b.getClass().getSimpleName()));
        expectedModifiers.sort(Comparator.comparing(b -> b.getClass().getSimpleName()));

        DefaultCompareStrategy compareStrategy =
                DefaultCompareStrategies
                        .allFieldsExcept(newPath("mobileAdjustment", "id"), newPath("mobileAdjustment", "lastChange"))
                        .forFields(newPath("lastChange"), newPath("demographicsAdjustments", "0", "lastChange"))
                        .useMatcher(approximatelyNow());
        assertThat("корректировки не соответствуют ожидаемым", actualModifiers,
                containsInAnyOrder(mapList(expectedModifiers,
                        modifier -> beanDiffer(modifier).useCompareStrategy(compareStrategy))));
    }

    private List<BidModifier> getFlatBidModifiers(ComplexBidModifier complexBidModifier) {
        if (complexBidModifier == null) {
            return emptyList();
        }
        List<BidModifier> bidModifiers = new ArrayList<>();
        if (complexBidModifier.getRetargetingModifier() != null) {
            bidModifiers.add(complexBidModifier.getRetargetingModifier());
        }
        if (complexBidModifier.getDemographyModifier() != null) {
            bidModifiers.add(complexBidModifier.getDemographyModifier());
        }
        if (complexBidModifier.getMobileModifier() != null) {
            bidModifiers.add(complexBidModifier.getMobileModifier());
        }
        if (complexBidModifier.getVideoModifier() != null) {
            bidModifiers.add(complexBidModifier.getVideoModifier());
        }
        if (complexBidModifier.getPerformanceTgoModifier() != null) {
            bidModifiers.add(complexBidModifier.getPerformanceTgoModifier());
        }
        if (complexBidModifier.getGeoModifier() != null) {
            bidModifiers.add(complexBidModifier.getGeoModifier());
        }
        return bidModifiers;
    }
}
