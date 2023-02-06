package ru.yandex.market.aliasmaker.cache.offers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.protobuf.ByteString;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.cache.KnowledgeService;
import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.aliasmaker.offers.OfferConversionTest;
import ru.yandex.market.aliasmaker.offers.deep_matcher.DeepMatcherSuggestService;
import ru.yandex.market.aliasmaker.offers.matching.Filter;
import ru.yandex.market.aliasmaker.offers.matching.OffersMatchingService;
import ru.yandex.market.ir.http.Markup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 26.02.2020
 */
public class OffersGenerationQueueTest {
    private static final int CATEGORY_ID = 1;
    private DeepMatcherOffersGenerationQueue deepMatcherOffersGenerationQueue;    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(0)
            .randomize(ByteString.class, (Supplier<ByteString>) OffersGenerationQueueTest::randomByteString)
            .build();
    private WhiteOffersGenerationQueue whiteOffersGenerationQueue;
    private ExecutorService executorService;
    private List<FutureTask<?>> futureTasks;
    private OfferService offerService;
    private OffersMatchingService offersMatchingService;
    private DeepMatcherSuggestService deepMatcherSuggestService;
    private OffersCache cache;
    private List<Offer> offers;
    private List<Offer> deepmatcherOffers;
    private KnowledgeService knowledgeService;
    private Set<String> forbiddenFilterResults;

    private static ByteString randomByteString() {
        var object = RANDOM.nextObject(String.class);
        return ByteString.copyFromUtf8(object);
    }

    @Before
    public void setUp() {
        futureTasks = new ArrayList<>();
        executorService = mock(ExecutorService.class);
        when(executorService.submit(any(Callable.class))).then(invocation -> {
            Callable callable = invocation.getArgument(0, Callable.class);
            FutureTask result = new FutureTask<>(callable);
            futureTasks.add(result);
            return result;
        });
        cache = mock(OffersCache.class);
        when(cache.doWithOffers(anyInt(), any(Consumer.class),
                anyInt(), any(TimeUnit.class), anyBoolean())).thenAnswer(invocation -> {
            Consumer<Iterator<Offer>> consumer = invocation.getArgument(1, Consumer.class);
            consumer.accept(offers.iterator());
            return true;
        });
        offerService = new OfferService();
        offerService.setOffersCache(cache);
        offers = new ArrayList<>();
        deepmatcherOffers = new ArrayList<>();
        forbiddenFilterResults = new HashSet<>();
        offersMatchingService = mock(OffersMatchingService.class);
        when(offersMatchingService.getNotPlacedOffers(anyInt(), anyInt(), anySet(), anySet(),
                any(Filter.class), anyBoolean(), any(Iterator.class))).thenAnswer(invocation -> {
            int count = invocation.getArgument(1, Integer.class);
            Iterator<Offer> iterator = invocation.getArgument(6, Iterator.class);
            return StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                            false)
                    .filter(o -> !forbiddenFilterResults.contains(o.getClassifierMagicId()))
                    .limit(count).collect(Collectors.toList());
        });
        whiteOffersGenerationQueue = new WhiteOffersGenerationQueue(offersMatchingService, offerService,
                executorService);
        deepMatcherSuggestService = mock(DeepMatcherSuggestService.class);
        when(deepMatcherSuggestService.getDeepMatcherOffers(anyInt(), anyList(), anyList(), anyList(), anyInt()))
                .thenAnswer(invocation -> {
                    int limit = invocation.getArgument(4, Integer.class);
                    return deepmatcherOffers.subList(0, Math.min(deepmatcherOffers.size(), limit));
                });
        deepMatcherOffersGenerationQueue = new DeepMatcherOffersGenerationQueue(executorService,
                deepMatcherSuggestService);
        knowledgeService = mock(KnowledgeService.class);
        Mockito.when(knowledgeService.getVendorName(anyInt())).thenReturn("Q");
        Mockito.when(knowledgeService.getModelName(anyInt(), anyInt(), anyInt())).thenReturn("Q");
    }

    @Test
    public void testSyncWhite() {
        AliasMaker.GetOffersRequest request = AliasMaker.GetOffersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setMaxOffers(10)
                .addOfferFilters(newOfferFilter())
                .addOfferFilters(newOfferFilter())
                .addAllIgnoredClusterId(Arrays.asList(10L, 11L))
                .addAllIgnoredOfferId(Arrays.asList("abc", "bcd", "def"))
                .build();
        offers.add(newOffer());
        offers.add(newOffer());
        offers.add(newOffer());

        Filter filter = new Filter()
                .setSkutched(false)
                .setInnerFilters(request.getOfferFiltersList())
                .setMatchedOnOperatorModel(true);

        AliasMaker.GetOffersResponse resp = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        verify(offersMatchingService).getNotPlacedOffers(eq(CATEGORY_ID),
                eq(request.getMaxOffers()),
                eq(new HashSet<>(request.getIgnoredOfferIdList())),
                eq(new HashSet<>(request.getIgnoredClusterIdList())),
                eq(filter),
                eq(false),
                any(Iterator.class)
        );
        assertThat(resp.getOfferCount()).isEqualTo(offers.size());
        verify(cache).doWithOffers(
                eq(CATEGORY_ID), any(Consumer.class),
                anyInt(), any(TimeUnit.class), eq(true));
    }

    @Test
    public void testSyncDeepmatcher() {
        AliasMaker.GetDeepMatcherOffersRequest request = AliasMaker.GetDeepMatcherOffersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setCount(10)
                .addAllIgnoredModelId(Arrays.asList(100L, 101L))
                .addAllIgnoredOfferId(Arrays.asList("abc", "bcd", "def"))
                .addAllModelMetaInfoType(Collections.singletonList(AliasMaker.ModelMetaInfo.Type.VENDOR))
                .build();
        deepmatcherOffers.add(newOffer());
        deepmatcherOffers.add(newOffer());
        deepmatcherOffers.add(newOffer());

        AliasMaker.GetOffersResponse resp = deepMatcherOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        verify(deepMatcherSuggestService).getDeepMatcherOffers(eq(CATEGORY_ID),
                eq(request.getModelMetaInfoTypeList()),
                eq(request.getIgnoredModelIdList()),
                eq(request.getIgnoredOfferIdList()),
                eq(request.getCount()));
        assertThat(resp.getOfferCount()).isEqualTo(deepmatcherOffers.size());
        verify(deepMatcherSuggestService).getDeepMatcherOffers(eq(CATEGORY_ID),
                eq(request.getModelMetaInfoTypeList()),
                eq(request.getIgnoredModelIdList()),
                eq(request.getIgnoredOfferIdList()),
                eq(request.getCount()));
    }

    @Test
    public void testAsyncEnqueueWhite() {
        AliasMaker.GetOffersRequest request = AliasMaker.GetOffersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setMaxOffers(10)
                .setMaxOffersAllTasks(100)
                .setAsync(true)
                .build();
        offers.add(newOffer());

        AliasMaker.GetOffersResponse resp = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        assertThat(futureTasks).hasSize(1);
        assertThat(resp.getStatus()).isEqualTo(AliasMaker.GetOffersResponse.RequestStatus.ENQUEUED);
        assertThat(resp.getOfferCount()).isEqualTo(0);
    }

    @Test
    public void testAsyncEnqueueDiffConfigs() {
        AliasMaker.GetOffersRequest request = AliasMaker.GetOffersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setMaxOffers(10)
                .setMaxOffersAllTasks(100)
                .setAsync(true)
                .build();
        offers.add(newOffer());

        whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        request = request.toBuilder()
                .addOfferFilters(newOfferFilter())
                .build();

        AliasMaker.GetOffersResponse response = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        assertThat(futureTasks).hasSize(2);
        assertThat(response.getStatus()).isEqualTo(AliasMaker.GetOffersResponse.RequestStatus.ENQUEUED);
    }

    @Test
    public void testAsyncAlreadyEnqueuedWhite() {
        AliasMaker.GetOffersRequest request = AliasMaker.GetOffersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setMaxOffers(10)
                .setMaxOffersAllTasks(100)
                .addAllIgnoredOfferId(Arrays.asList("q", "v", "i"))
                .addAllIgnoredClusterId(Arrays.asList(10L, 12L))
                .setAsync(true)
                .build();
        offers.add(newOffer());

        whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        request = request.toBuilder()
                .setMaxOffers(15)
                .setMaxOffersAllTasks(45)
                .clearIgnoredClusterId()
                .clearIgnoredOfferId()
                .addAllIgnoredOfferId(Arrays.asList("EWRWER"))
                .addAllIgnoredClusterId(Arrays.asList(15L, 16L))
                .build();
        AliasMaker.GetOffersResponse resp = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        assertThat(resp.getStatus()).isEqualTo(AliasMaker.GetOffersResponse.RequestStatus.IN_PROCESS);
    }

    @Test
    public void testAsyncSuccessWhite() throws Exception {
        List<String> ignoredOfferIds = Arrays.asList("q", "v", "i");
        List<Long> ignoredClusterIds = Arrays.asList(10L, 11L);

        AliasMaker.GetOffersRequest request = AliasMaker.GetOffersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setMaxOffers(2)
                .setMaxOffersAllTasks(4)
                .addAllIgnoredOfferId(ignoredOfferIds)
                .addAllIgnoredClusterId(ignoredClusterIds)
                .setAsync(true)
                .build();
        offers.add(newOffer());
        offers.add(newOffer());
        offers.add(newOffer());
        offers.add(newOffer());
        offers.add(newOffer());

        whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        futureTasks.get(0).run();
        verify(offersMatchingService).getNotPlacedOffers(eq(CATEGORY_ID),
                eq(request.getMaxOffers()),
                eq(new HashSet<>(ignoredOfferIds)),
                eq(new HashSet<>(ignoredClusterIds)),
                any(),
                eq(false),
                any(Iterator.class)
        );
        ignoredOfferIds = new ArrayList<>(ignoredOfferIds);
        ignoredOfferIds.add(offers.get(0).getClassifierMagicId());
        ignoredOfferIds.add(offers.get(1).getClassifierMagicId());

        ignoredClusterIds = new ArrayList<>(ignoredClusterIds);
        ignoredClusterIds.add(offers.get(0).getClusterId());
        ignoredClusterIds.add(offers.get(1).getClusterId());

        verify(offersMatchingService).getNotPlacedOffers(eq(CATEGORY_ID),
                eq(request.getMaxOffers()),
                eq(new HashSet<>(ignoredOfferIds)),
                eq(new HashSet<>(ignoredClusterIds)),
                any(),
                eq(false),
                any(Iterator.class)
        );

        BaseCachedOffersGenerationQueue.FoundOffers foundOffers = whiteOffersGenerationQueue.getFoundOffers(
                request
        );
        assertThat(foundOffers).isNotNull();
        assertThat(foundOffers.getOffersLeft()).isEqualTo(request.getMaxOffersAllTasks());
        assertThat(foundOffers.originalOffersCount).isEqualTo(request.getMaxOffersAllTasks());

        AliasMaker.GetOffersResponse resp = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());
        assertThat(resp.getStatus()).isEqualTo(AliasMaker.GetOffersResponse.RequestStatus.SUCCESS);
        assertThat(resp.getOfferCount()).isEqualTo(request.getMaxOffers());

        foundOffers = whiteOffersGenerationQueue.getFoundOffers(
                request
        );
        assertThat(foundOffers).isNotNull();
        assertThat(foundOffers.getOffersLeft()).isEqualTo(request.getMaxOffersAllTasks() - request.getMaxOffers());
    }

    @Test
    public void testAsyncReschedule() {
        AliasMaker.GetOffersRequest request = AliasMaker.GetOffersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setMaxOffers(1)
                .setMaxOffersAllTasks(2)
                .setAsync(true)
                .build();
        offers.add(newOffer());
        offers.add(newOffer());

        whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        futureTasks.get(0).run();

        AliasMaker.GetOffersResponse resp = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());
        assertThat(resp.getStatus()).isEqualTo(AliasMaker.GetOffersResponse.RequestStatus.SUCCESS);
        assertThat(resp.getOfferCount()).isEqualTo(request.getMaxOffers());

        resp.getOfferList().forEach(o -> forbiddenFilterResults.add(o.getOfferId()));
        resp = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());
        assertThat(resp.getStatus()).isEqualTo(AliasMaker.GetOffersResponse.RequestStatus.SUCCESS);
        assertThat(resp.getOfferCount()).isEqualTo(request.getMaxOffers());

        resp = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());
        assertThat(resp.getStatus()).isEqualTo(AliasMaker.GetOffersResponse.RequestStatus.ENQUEUED);
    }

    @Test
    public void testAsyncZeroResult() throws Exception {
        AliasMaker.GetOffersRequest request = AliasMaker.GetOffersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setMaxOffers(1)
                .setMaxOffersAllTasks(2)
                .setAsync(true)
                .build();
        offers.add(newOffer());
        offers.add(newOffer());

        whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());

        futureTasks.get(0).run();
        BaseCachedOffersGenerationQueue.FoundOffers found = whiteOffersGenerationQueue.getFoundOffers(request);

        assertThat(found).isNotNull();
        assertThat(found.originalOffersCount).isEqualTo(2);

        offers.forEach(o -> forbiddenFilterResults.add(o.getClassifierMagicId()));
        AliasMaker.GetOffersResponse resp = whiteOffersGenerationQueue.requestOffers(
                knowledgeService, request, request.getCategoryId());
        //zero offers found -> rescheduling
        assertThat(resp.getStatus()).isEqualTo(AliasMaker.GetOffersResponse.RequestStatus.ENQUEUED);
    }

    private Markup.OfferFilter newOfferFilter() {
        return Markup.OfferFilter.newBuilder()
                .addStringValues(RANDOM.nextObject(String.class))
                .setSourceType(RANDOM.nextObject(Markup.OfferFilter.SourceType.class))
                .setOperator(RANDOM.nextObject(Markup.OfferFilter.Operator.class))
                .setSourceId(RANDOM.nextLong())
                .setNumericValue(RANDOM.nextDouble())
                .addValueIds(RANDOM.nextLong())
                .build();
    }

    private Offer newOffer() {
        return OfferConversionTest.randomOffer(RANDOM);
    }



}
