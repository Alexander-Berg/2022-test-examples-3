package ru.yandex.market.aliasmaker.offers.deep_matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.cache.KnowledgeService;
import ru.yandex.market.aliasmaker.cache.deep_matcher.DeepMatcherProcessedItemService;
import ru.yandex.market.aliasmaker.cache.deep_matcher.DeepMatcherSuggestCache;
import ru.yandex.market.aliasmaker.cache.offers.OffersCache;
import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.aliasmaker.offers.matching.OffersMatchingService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.matcher.be.OfferCopy;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 22.01.2019
 */
public class DeepMatcherSuggestServiceTest {
    private static final int CATEGORY_ID = 1;
    private static final int ANOTHER_CATEGORY_ID = 2;
    private static final int LIMIT = 10;
    private static final long MODEL_ID = 100L;
    private static final long ANOTHER_MODEL_ID = 101L;
    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .stringLengthRange(32, 32)
            .build();
    private DeepMatcherSuggestService suggestService;
    private OffersMatchingService offersMatchingService;
    private OffersCache offersCache;
    private DeepMatcherSuggestCache suggestCache;
    private DeepMatcherProcessedItemService processedItemService;
    private KnowledgeService knowledgeService;

    private static <T> Map<Long, T> groupByModelId(List<DeepMatcherSuggest> suggests,
                                                   Collector<DeepMatcherSuggest, ?, T> collector) {
        return suggests.stream().collect(Collectors.groupingBy(DeepMatcherSuggest::getModelId, collector));
    }

    private static Map<Long, List<DeepMatcherSuggest>> groupByModelId(List<DeepMatcherSuggest> suggests) {
        return groupByModelId(suggests, Collectors.mapping(Function.identity(), Collectors.toList()));
    }

    @Before
    public void setUp() {
        offersMatchingService = mock(OffersMatchingService.class);
        offersCache = mock(OffersCache.class);
        suggestCache = mock(DeepMatcherSuggestCache.class);
        processedItemService = mock(DeepMatcherProcessedItemService.class);
        knowledgeService = mock(KnowledgeService.class);

        suggestService = new DeepMatcherSuggestService(offersMatchingService, offersCache,
                suggestCache, processedItemService, knowledgeService);
    }

    @Test
    public void noSuggests() {
        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(Collections.emptyMap());

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        assertThat(offers).isEmpty();
        verify(offersMatchingService, never()).matchOffers(eq(CATEGORY_ID), anyList(), any(), any(), any());
    }

    @Test
    public void allProcessed() {
        List<DeepMatcherSuggest> suggests = generateSuggests(LIMIT);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));
        when(processedItemService.getCategoryProcessedGoodIds(eq(CATEGORY_ID))).thenReturn(
                groupByModelId(suggests, Collectors.mapping(DeepMatcherSuggest::getClassifierGoodId,
                        Collectors.toSet())));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        assertThat(offers).isEmpty();
        verify(offersMatchingService, never()).matchOffers(eq(CATEGORY_ID), anyList(), any(), any(), any());
    }

    @Test
    public void allIgnoredByType() {
        List<DeepMatcherSuggest> suggests = generateSuggests(LIMIT).stream()
                .peek(s -> s.setType(null))
                .collect(Collectors.toList());

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID,
                Collections.singletonList(AliasMaker.ModelMetaInfo.Type.BLUE_MARKET),
                emptyList(), emptyList(), LIMIT);

        assertThat(offers).isEmpty();
        verify(offersMatchingService, never()).matchOffers(eq(CATEGORY_ID), anyList(), any(), any(), any());
    }

    @Test
    public void allIgnoredByModelId() {
        List<DeepMatcherSuggest> suggests = generateSuggests(LIMIT);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(),
                suggests.stream().map(DeepMatcherSuggest::getModelId).distinct().collect(Collectors.toList()),
                emptyList(), LIMIT);

        assertThat(offers).isEmpty();
        verify(offersMatchingService, never()).matchOffers(eq(CATEGORY_ID), anyList(), any(), any(), any());
    }

    @Test
    public void allIgnoredByOfferId() {
        List<DeepMatcherSuggest> suggests = generateSuggests(LIMIT);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                suggests.stream().map(DeepMatcherSuggest::getClassifierMagicId).distinct().collect(Collectors.toList()),
                LIMIT);

        assertThat(offers).isEmpty();
        verify(offersMatchingService, never()).matchOffers(eq(CATEGORY_ID), anyList(), any(), any(), any());
    }

    @Test
    public void allAlreadyMatched() {
        List<DeepMatcherSuggest> suggests = generateSuggests(LIMIT, MODEL_ID);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));
        Map<String, Offer> offersMap = suggests.stream()
                .collect(Collectors.toMap(DeepMatcherSuggest::getClassifierMagicId, this::generateOffer));
        when(offersCache.getOffers(eq(CATEGORY_ID), anyCollection()))
                .then(args -> args.getArgument(1, Collection.class).stream()
                        .map(offersMap::get)
                        .collect(Collectors.toList()));

        when(offersMatchingService.matchOffers(eq(CATEGORY_ID), anyList(), any(), any(), any())).thenReturn(
                suggests.stream()
                        .map(s -> {
                            AliasMaker.OfferMatchState.Builder builder = AliasMaker.OfferMatchState.newBuilder()
                                    .setOfferId(s.getClassifierMagicId());
                            ModelStorage.Model model = ModelStorage.Model.newBuilder().build();
                            switch (Math.toIntExact(Math.abs(s.getModelId() % 3))) {
                                case 0:
                                    builder.setModel(model);
                                    break;
                                case 1:
                                    builder.setModification(model);
                                    break;
                                default:
                                    builder.setSku(model);
                            }
                            return builder.build();
                        }).collect(Collectors.toList()));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        assertThat(offers).isEmpty();
        verify(offersMatchingService, times(1)).matchOffers(eq(CATEGORY_ID),
                anyList(), eq(AliasMaker.OfferType.OFFER), any(), eq(null));
        verify(offersCache, never()).getOffers(eq(CATEGORY_ID), anyCollection());
    }

    @Test
    public void getOnlyLimit() {
        List<DeepMatcherSuggest> suggests = generateSuggests(2 * LIMIT, MODEL_ID);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));
        Map<String, Offer> offersMap = suggests.stream()
                .collect(Collectors.toMap(DeepMatcherSuggest::getClassifierMagicId, this::generateOffer));
        when(offersCache.getOffers(eq(CATEGORY_ID), anyCollection()))
                .then(args -> args.getArgument(1, Collection.class).stream()
                        .map(offersMap::get)
                        .collect(Collectors.toList()));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        assertThat(offers).hasSize(LIMIT);
        verify(offersMatchingService, times(1)).matchOffers(eq(CATEGORY_ID),
                anyList(), eq(AliasMaker.OfferType.OFFER), any(), eq(null));
    }

    @Test
    public void processedInAnotherCategory() {
        List<DeepMatcherSuggest> suggests = generateSuggests(LIMIT, MODEL_ID);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));
        Map<String, Offer> offersMap = suggests.stream()
                .collect(Collectors.toMap(DeepMatcherSuggest::getClassifierMagicId, this::generateOffer));
        when(offersCache.getOffers(eq(CATEGORY_ID), anyCollection()))
                .then(args -> args.getArgument(1, Collection.class).stream()
                        .map(offersMap::get)
                        .collect(Collectors.toList()));
        when(processedItemService.getCategoryProcessedGoodIds(eq(ANOTHER_CATEGORY_ID))).thenReturn(
                groupByModelId(suggests, Collectors.mapping(DeepMatcherSuggest::getClassifierGoodId,
                        Collectors.toSet())));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        assertThat(offers).hasSize(LIMIT);
        verify(offersMatchingService, times(1)).matchOffers(eq(CATEGORY_ID), anyList(),
                eq(AliasMaker.OfferType.OFFER), any(), eq(null));
    }

    @Test
    public void processedInAnotherModel() {
        List<DeepMatcherSuggest> suggests = generateSuggests(LIMIT, MODEL_ID);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));

        Map<String, Offer> offersMap = suggests.stream()
                .collect(Collectors.toMap(DeepMatcherSuggest::getClassifierMagicId, this::generateOffer));
        when(offersCache.getOffers(eq(CATEGORY_ID), anyCollection()))
                .then(args -> args.getArgument(1, Collection.class).stream()
                        .map(offersMap::get)
                        .collect(Collectors.toList()));

        Map<Long, Set<String>> processedGoodIds = groupByModelId(suggests,
                Collectors.mapping(DeepMatcherSuggest::getClassifierGoodId, Collectors.toSet()));
        when(processedItemService.getCategoryProcessedGoodIds(eq(CATEGORY_ID))).thenReturn(
                ImmutableMap.of(ANOTHER_MODEL_ID, processedGoodIds.get(MODEL_ID)));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        assertThat(offers).hasSize(LIMIT);
        verify(offersMatchingService, times(1)).matchOffers(eq(CATEGORY_ID),
                anyList(), eq(AliasMaker.OfferType.OFFER), any(), eq(null));
    }

    @Test
    public void updateGoodIdAndDeepMatcherModelId() {
        List<DeepMatcherSuggest> suggests = generateSuggests(LIMIT, MODEL_ID);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));

        Map<String, Offer> offersMap = suggests.stream()
                .collect(Collectors.toMap(DeepMatcherSuggest::getClassifierMagicId, this::generateOffer));
        when(offersCache.getOffers(eq(CATEGORY_ID), anyCollection()))
                .then(args -> args.getArgument(1, Collection.class).stream()
                        .map(offersMap::get)
                        .collect(Collectors.toList()));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        Map<String, DeepMatcherSuggest> suggestByOfferId = suggests.stream()
                .collect(Collectors.toMap(DeepMatcherSuggest::getClassifierMagicId, Function.identity()));

        assertThat(offers).hasSize(LIMIT);
        assertThat(offers).allMatch(o -> {
            DeepMatcherSuggest suggest = suggestByOfferId.get(o.getClassifierMagicId());
            return suggest.getClassifierGoodId().equals(o.getClassifierGoodId())
                    && suggest.getModelId() == o.getDeepMatcherModelId();
        });
        verify(offersMatchingService, times(1)).matchOffers(eq(CATEGORY_ID),
                anyList(), eq(AliasMaker.OfferType.OFFER), any(KnowledgeService.class), eq(null));
    }

    @Test
    public void getWithMaxNumberOfOffers() {
        List<DeepMatcherSuggest> suggests = new ArrayList<>(generateSuggests(LIMIT, MODEL_ID));
        suggests.addAll(generateSuggests(LIMIT * 2, ANOTHER_MODEL_ID));

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));

        Map<String, Offer> offersMap = suggests.stream()
                .collect(Collectors.toMap(DeepMatcherSuggest::getClassifierMagicId, this::generateOffer));
        when(offersCache.getOffers(eq(CATEGORY_ID), anyCollection()))
                .then(args -> args.getArgument(1, Collection.class).stream()
                        .map(offersMap::get)
                        .collect(Collectors.toList()));

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        assertThat(offers).hasSize(LIMIT);
        assertThat(offers).allMatch(o -> ANOTHER_MODEL_ID == o.getDeepMatcherModelId());
        verify(offersMatchingService, times(1)).matchOffers(eq(CATEGORY_ID),
                anyList(), eq(AliasMaker.OfferType.OFFER), any(KnowledgeService.class), eq(null));
    }

    @Test
    public void filterWithMaxNumberOfOffers() {
        List<DeepMatcherSuggest> suggests = new ArrayList<>(generateSuggests(LIMIT, MODEL_ID));
        List<DeepMatcherSuggest> anotherModelSuggests = generateSuggests(LIMIT * 2, ANOTHER_MODEL_ID);
        suggests.addAll(anotherModelSuggests);

        when(suggestCache.getValue(eq(CATEGORY_ID))).thenReturn(groupByModelId(suggests));

        Map<String, Offer> offersMap = suggests.stream()
                .collect(Collectors.toMap(DeepMatcherSuggest::getClassifierMagicId, this::generateOffer));
        when(offersCache.getOffers(eq(CATEGORY_ID), anyCollection()))
                .then(args -> args.getArgument(1, Collection.class).stream()
                        .map(offersMap::get)
                        .collect(Collectors.toList()));

        Set<String> anotherModelOfferIds = anotherModelSuggests.stream()
                .map(DeepMatcherSuggest::getClassifierMagicId)
                .collect(Collectors.toSet());
        when(offersMatchingService.matchOffers(eq(CATEGORY_ID), anyList(), any(), any(), any())).then(args -> {
            List<String> offerIds = args.getArgument(1, List.class);
            return offerIds.stream()
                    .map(offerId -> {
                        AliasMaker.OfferMatchState.Builder offerMatchStateBuilder =
                                AliasMaker.OfferMatchState.newBuilder()
                                .setOfferId(offerId);
                        if (anotherModelOfferIds.contains(offerId)) {
                            offerMatchStateBuilder.setModel(ModelStorage.Model.newBuilder().build());
                        }
                        return offerMatchStateBuilder.build();
                    }).collect(Collectors.toList());
        });

        List<Offer> offers = suggestService.getDeepMatcherOffers(CATEGORY_ID, emptyList(), emptyList(),
                emptyList(), LIMIT);

        assertThat(offers).hasSize(LIMIT);
        assertThat(offers).allMatch(o -> MODEL_ID == o.getDeepMatcherModelId());
        verify(offersMatchingService, times(2)).matchOffers(eq(CATEGORY_ID),
                anyList(), eq(AliasMaker.OfferType.OFFER), any(), eq(null));
    }

    private List<DeepMatcherSuggest> generateSuggests(int limit, Long modelId) {
        return IntStream.range(0, limit).mapToObj(i -> {
            DeepMatcherSuggest suggest = new DeepMatcherSuggest();
            suggest.setModelId(modelId == null ? random.nextInt() : modelId);
            suggest.setClassifierGoodId(random.nextObject(String.class));
            suggest.setClassifierMagicId(random.nextObject(String.class));
            suggest.setType(random.nextObject(AliasMaker.ModelMetaInfo.Type.class));
            return suggest;
        }).collect(Collectors.toList());
    }

    private List<DeepMatcherSuggest> generateSuggests(int limit) {
        return generateSuggests(limit, null);
    }

    private Offer generateOffer(DeepMatcherSuggest suggest) {
        Offer offer = new Offer();
        offer.setClassifierMagicId(suggest.getClassifierMagicId());
        offer.setOfferCopy(OfferCopy.newBuilder().build());
        return offer;
    }
}
