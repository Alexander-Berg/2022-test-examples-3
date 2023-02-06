package ru.yandex.direct.grid.processing.service.showcondition.keywords;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.advq.AdvqClient;
import ru.yandex.direct.advq.SearchKeywordResult;
import ru.yandex.direct.advq.SearchRequest;
import ru.yandex.direct.advq.search.AdvqRequestKeyword;
import ru.yandex.direct.advq.search.SearchItem;
import ru.yandex.direct.advq.search.Statistics;
import ru.yandex.direct.advq.search.StatisticsPhraseItem;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.service.BaseCampaignService;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdBulkRefineKeywords;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdRefineKeyword;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdRefineKeywordPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.RefinedWord;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class RefineKeywordServiceTest {

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Autowired
    private KeywordWithLemmasFactory keywordWithLemmasFactory;

    @Autowired
    private StopWordService stopWordService;

    @Autowired
    private BaseCampaignService baseCampaignService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private GridValidationService gridValidationService;

    @Mock
    private AdvqClient advqClient;

    private RefineKeywordService refineKeywordService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        refineKeywordService = new RefineKeywordService(
                keywordNormalizer,
                keywordWithLemmasFactory,
                baseCampaignService, stopWordService,
                advqClient,
                shardHelper, minusKeywordsPackRepository, adGroupRepository, gridValidationService);
    }

    @Test
    public void testRefine_manyMinusWords() {
        String keyword = "keyword";
        List<String> minusWords = Collections.nCopies(RefineKeywordService.MAX_HUMAN_SCROLL_WORD_COUNT + 1, "-minus");
        GdRefineKeyword request = buildRequest(keyword, minusWords);
        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        assertThat(payload).is(matchedBy(beanDiffer(RefineKeywordService.EMPTY_PAYLOAD)));
    }

    @Test
    public void testRefine_keywordWithQuotes() {
        String keyword = " \"keyword in quotes\"";
        GdRefineKeyword request = buildRequest(keyword);
        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        assertThat(payload).is(matchedBy(beanDiffer(RefineKeywordService.EMPTY_PAYLOAD)));
    }

    @Test
    public void testComputeWordStats_advqReturnNull() {
        GdRefineKeyword request = buildRequest("keyword");
        when(advqClient.search(anyCollection(), any(), any())).thenAnswer(
                invocation -> {
                    Collection<SearchRequest> requests = invocation.getArgument(0);
                    Map<SearchRequest, Map<AdvqRequestKeyword, SearchKeywordResult>> map = new IdentityHashMap<>();
                    requests.forEach(r -> {
                        Map<AdvqRequestKeyword, SearchKeywordResult> results = new HashMap<>();
                        results.put(r.getKeywords().get(0), null);
                        map.put(r, results);
                    });
                    return map;
                }
        );
        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        assertThat(payload).is(matchedBy(beanDiffer(RefineKeywordService.EMPTY_PAYLOAD)));
    }

    @Test
    public void testComputeWordsStat() {
        GdRefineKeyword request = buildRequest("скидка +на одежду +в москве", List.of("скидка"));

        Statistics stats = generateStatistics();

        when(advqClient.search(anyCollection(), any(), any())).thenAnswer(
                invocation -> {
                    Collection<SearchRequest> requests = invocation.getArgument(0);
                    return StreamEx.of(requests)
                            .mapToEntry(r -> StreamEx.of(r.getKeywords())
                                    .mapToEntry(keyword -> SearchKeywordResult.success(
                                            new SearchItem()
                                                    .withReq(keyword.getPhrase())
                                                    .withStat(stats)))
                                    .toMap()
                            )
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        GdRefineKeywordPayload expectedPayload = generateExpectedPayload(false);
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void testComputeWordsStat_ContentPromotionVideoGroup() {
        GdRefineKeyword request = buildRequest("скидка +на одежду +в москве", List.of("скидка"))
                .withAdGroupType(GdAdGroupType.CONTENT_PROMOTION_VIDEO);

        Statistics stats = generateStatistics();

        when(advqClient.searchVideo(anyCollection(), any(), any())).thenAnswer(
                invocation -> {
                    Collection<SearchRequest> requests = invocation.getArgument(0);
                    return StreamEx.of(requests)
                            .mapToEntry(r -> StreamEx.of(r.getKeywords())
                                    .mapToEntry(keyword -> SearchKeywordResult.success(
                                            new SearchItem()
                                                    .withReq(keyword.getPhrase())
                                                    .withStat(stats)))
                                    .toMap()
                            )
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        GdRefineKeywordPayload expectedPayload = generateExpectedPayload(false);
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void testComputeWordsStat_ContentPromotionCollectionGroup() {
        GdRefineKeyword request = buildRequest("скидка +на одежду +в москве", List.of("скидка"))
                .withAdGroupType(GdAdGroupType.CONTENT_PROMOTION_COLLECTION);

        Statistics stats = generateStatistics();

        when(advqClient.search(anyCollection(), any(), any())).thenAnswer(
                invocation -> {
                    Collection<SearchRequest> requests = invocation.getArgument(0);
                    return StreamEx.of(requests)
                            .mapToEntry(r -> StreamEx.of(r.getKeywords())
                                    .mapToEntry(keyword -> SearchKeywordResult.success(
                                            new SearchItem()
                                                    .withReq(keyword.getPhrase())
                                                    .withStat(stats)))
                                    .toMap()
                            )
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        GdRefineKeywordPayload expectedPayload = generateExpectedPayload(true);
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void testBulkComputeWordsStat() {
        GdBulkRefineKeywords request = buildBulkRequest(
                List.of("скидка +на одежду +в москве", "скидка на обувь +в москве"));

        HashMap<String, Statistics> stats = generateBulkStatistics(request);

        when(advqClient.search(anyCollection(), any(), any())).thenAnswer(
                invocation -> {
                    Collection<SearchRequest> requests = invocation.getArgument(0);
                    return StreamEx.of(requests)
                            .mapToEntry(r -> StreamEx.of(r.getKeywords())
                                    .mapToEntry(keyword -> SearchKeywordResult.success(
                                            new SearchItem()
                                                    .withReq(keyword.getPhrase())
                                                    .withStat(stats.get(keyword.getPhrase()))))
                                    .toMap()
                            )
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        checkBulkPayload(payload, false);
    }

    @Test
    public void testBulkComputeWordsStat_ContentPromotionVideoGroup() {
        GdBulkRefineKeywords request = buildBulkRequest(
                List.of("скидка +на одежду +в москве", "скидка на обувь +в москве"))
                .withAdGroupType(GdAdGroupType.CONTENT_PROMOTION_VIDEO);

        HashMap<String, Statistics> stats = generateBulkStatistics(request);

        when(advqClient.searchVideo(anyCollection(), any(), any())).thenAnswer(
                invocation -> {
                    Collection<SearchRequest> requests = invocation.getArgument(0);
                    return StreamEx.of(requests)
                            .mapToEntry(r -> StreamEx.of(r.getKeywords())
                                    .mapToEntry(keyword -> SearchKeywordResult.success(
                                            new SearchItem()
                                                    .withReq(keyword.getPhrase())
                                                    .withStat(stats.get(keyword.getPhrase()))))
                                    .toMap()
                            )
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        checkBulkPayload(payload, false);
    }

    @Test
    public void testBulkComputeWordsStat_ContentPromotionCollectionGroup() {
        GdBulkRefineKeywords request = buildBulkRequest(
                List.of("скидка +на одежду +в москве", "скидка на обувь +в москве"))
                .withAdGroupType(GdAdGroupType.CONTENT_PROMOTION_COLLECTION);

        HashMap<String, Statistics> stats = generateBulkStatistics(request);

        when(advqClient.search(anyCollection(), any(), any())).thenAnswer(
                invocation -> {
                    Collection<SearchRequest> requests = invocation.getArgument(0);
                    return StreamEx.of(requests)
                            .mapToEntry(r -> StreamEx.of(r.getKeywords())
                                    .mapToEntry(keyword -> SearchKeywordResult.success(
                                            new SearchItem()
                                                    .withReq(keyword.getPhrase())
                                                    .withStat(stats.get(keyword.getPhrase()))))
                                    .toMap()
                            )
                            .toCustomMap(IdentityHashMap::new);
                }
        );

        GdRefineKeywordPayload payload = refineKeywordService.refine(request);
        checkBulkPayload(payload, true);
    }

    private Statistics generateStatistics() {
        StatisticsPhraseItem spi1 = new StatisticsPhraseItem();
        spi1.setPhrase("скидки +на детскую одежду +в магазинах москвы");
        spi1.setCnt(32L);  //  add to Acc: [word, exactCnt, cnt] => [детская, 0, 32], [магазин, 0, 32]
        StatisticsPhraseItem spi2 = new StatisticsPhraseItem();
        spi2.setPhrase("скидки +в магазинах москвы +на одежду");
        spi2.setCnt(140L);  // add to Acc: [word, exactCnt, cnt] => [магазин, 140, 140]
        StatisticsPhraseItem spi3 = new StatisticsPhraseItem();
        spi3.setPhrase("скидки +в магазинах москвы +на одежду 2019");
        spi3.setCnt(21L); // add to Acc: [word, exactCnt, cnt] => [магазин, 0, 21], [2019, 0, 21]
        StatisticsPhraseItem spi4 = new StatisticsPhraseItem();
        spi4.setPhrase("скидки +на одежду +в москве магазины 2019");
        spi4.setCnt(20L); // add to Acc: [word, exactCnt, cnt] => [магазин, 0, 20], [2019, 0, 20]
        StatisticsPhraseItem spi5 = new StatisticsPhraseItem();
        spi5.setPhrase("скидки +в магазинах москвы +на одежду сейчас");
        spi5.setCnt(12L); // add to Acc: [word, exactCnt, cnt] => [магазин, 0, 12], [сейчас, 0, 12]
        StatisticsPhraseItem spi6 = new StatisticsPhraseItem();
        spi6.setPhrase("скидки +на летнюю одежду +в москве");
        spi6.setCnt(7L); // add to Acc: [word, exactCnt, cnt] => [летняя, 7, 0]
        Statistics stats = new Statistics();
        stats.setIncludingPhrases(Arrays.asList(spi1, spi2, spi3, spi4, spi5, spi6));
        return stats;
    }

    private GdRefineKeywordPayload generateExpectedPayload(boolean collectionsAdGroup) {
        return new GdRefineKeywordPayload()
                .withHasMore(Boolean.FALSE)
                .withWords(Arrays.asList(
                        new RefinedWord(
                                "магазин",
                                List.of(
                                        "скидки +на детскую одежду +в магазинах москвы",
                                        "скидки +в магазинах москвы +на одежду",
                                        "скидки +в магазинах москвы +на одежду 2019",
                                        "скидки +на одежду +в москве магазины 2019",
                                        "скидки +в магазинах москвы +на одежду сейчас"
                                ),
                                collectionsAdGroup ? 14L : 140L
                        ),
                        new RefinedWord(
                                "2019",
                                List.of(
                                        "скидки +в магазинах москвы +на одежду 2019",
                                        "скидки +на одежду +в москве магазины 2019"
                                ),
                                collectionsAdGroup ? 5L : 41L
                        ),
                        new RefinedWord(
                                "детская",
                                List.of("скидки +на детскую одежду +в магазинах москвы"),
                                collectionsAdGroup ? 4L : 32L
                        ),
                        new RefinedWord(
                                "сейчас",
                                List.of("скидки +в магазинах москвы +на одежду сейчас"),
                                collectionsAdGroup ? 2L : 12L
                        ),
                        new RefinedWord(
                                "летний",
                                List.of("скидки +на летнюю одежду +в москве"),
                                collectionsAdGroup ? 1L : 7L
                        )
                ));
    }

    private HashMap<String, Statistics> generateBulkStatistics(GdBulkRefineKeywords request) {
        StatisticsPhraseItem spi01 = new StatisticsPhraseItem();
        spi01.setPhrase("скидки +на летнюю одежду +в магазинах москвы");
        spi01.setCnt(32L);  //  add to Acc: [word, exactCnt, cnt] => [летний, 0, 32], [магазин, 0, 32]
        StatisticsPhraseItem spi02 = new StatisticsPhraseItem();
        spi02.setPhrase("скидки +в магазинах москвы +на одежду");
        spi02.setCnt(140L);  // add to Acc: [word, exactCnt, cnt] => [магазин, 140, 140]
        Statistics stats0 = new Statistics();
        stats0.setIncludingPhrases(Arrays.asList(spi01, spi02));

        StatisticsPhraseItem spi11 = new StatisticsPhraseItem();
        spi11.setPhrase("скидки +на обувь +в москве магазины");
        spi11.setCnt(32L);  //  add to Acc: [word, exactCnt, cnt] => [магазин, 32, 32]
        StatisticsPhraseItem spi12 = new StatisticsPhraseItem();
        spi12.setPhrase("скидки +на летнюю обувь +в москве");
        spi12.setCnt(140L);  // add to Acc: [word, exactCnt, cnt] => [летний, 140, 140]
        Statistics stats1 = new Statistics();
        stats1.setIncludingPhrases(Arrays.asList(spi11, spi12));

        HashMap<String, Statistics> stats = new HashMap<>();
        stats.put(request.getKeywords().get(0), stats0);
        stats.put(request.getKeywords().get(1), stats1);
        return stats;
    }

    private void checkBulkPayload(GdRefineKeywordPayload payload, boolean collectionsAdGroup) {
        assertThat(payload.getHasMore()).isFalse();
        List<RefinedWord> words = payload.getWords();
        assertThat(words.size()).isEqualTo(2);
        words.sort(Comparator.comparing(RefinedWord::getWord));

        RefinedWord firstRefinedWord = words.get(0);
        assertThat(firstRefinedWord.getWord()).isEqualTo("летний");
        assertThat(firstRefinedWord.getCount()).isEqualTo(collectionsAdGroup ? 18L : 172L);
        firstRefinedWord.getPhrases().sort(String::compareTo);
        assertThat(firstRefinedWord.getPhrases()).is(matchedBy(beanDiffer(List.of(
                "скидки +на летнюю обувь +в москве",
                "скидки +на летнюю одежду +в магазинах москвы"
        ))));

        RefinedWord secondRefinedWord = words.get(1);
        assertThat(secondRefinedWord.getWord()).isEqualTo("магазин");
        assertThat(secondRefinedWord.getCount()).isEqualTo(collectionsAdGroup ? 18L : 172L);
        secondRefinedWord.getPhrases().sort(String::compareTo);
        assertThat(secondRefinedWord.getPhrases()).is(matchedBy(beanDiffer(List.of(
                "скидки +в магазинах москвы +на одежду",
                "скидки +на летнюю одежду +в магазинах москвы",
                "скидки +на обувь +в москве магазины"
        ))));
    }

    private GdRefineKeyword buildRequest(String keyword) {
        return buildRequest(keyword, Collections.emptyList());
    }

    private GdRefineKeyword buildRequest(String keyword, List<String> minusWords) {
        return new GdRefineKeyword()
                .withGeo(Collections.singletonList(Region.RUSSIA_REGION_ID))
                .withKeyword(keyword)
                .withMinusWords(minusWords);
    }

    private GdBulkRefineKeywords buildBulkRequest(List<String> keywords) {
        return new GdBulkRefineKeywords()
                .withGeo(Collections.singletonList(Region.RUSSIA_REGION_ID))
                .withKeywords(keywords);
    }
}
