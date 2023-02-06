package ru.yandex.market.aliasmaker.offers.matching;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.aliasmaker.cache.CategoryCache;
import ru.yandex.market.aliasmaker.cache.models.EmptyCategoryModelsCache;
import ru.yandex.market.aliasmaker.cache.models.ModelsShadowReloadingCache;
import ru.yandex.market.aliasmaker.cache.vendors.GlobalVendorsCache;
import ru.yandex.market.aliasmaker.models.CategoryKnowledge;
import ru.yandex.market.ir.FormalizerContractor;
import ru.yandex.market.ir.dao.CategoryCreator;
import ru.yandex.market.ir.processor.FormalizationStringIndexer;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.skubd2.knowledge.SkuRuntimeKnowledge;
import ru.yandex.matcher.knowledge.MatcherRuntimeKnowledge;
import ru.yandex.utils.string.indexed.IndexedStringFactory;

/**
 * @author apluhin
 * @created 4/15/22
 */
public class MatchingServiceTest {

    private MatchingService matchingService;
    private SkuRuntimeKnowledge skuRuntimeKnowledge;
    private MatcherRuntimeKnowledge matcherRuntimeKnowledge;
    private ModelsShadowReloadingCache modelCache;
    private CategoryCache categoryCache;


    @Before
    public void setUp() throws Exception {
        categoryCache = Mockito.mock(CategoryCache.class);
        modelCache = Mockito.mock(ModelsShadowReloadingCache.class);
        matchingService = new MatchingService(
                modelCache,
                Mockito.mock(GlobalVendorsCache.class),
                categoryCache,
                Executors.newCachedThreadPool(),
                Mockito.mock(FormalizerContractor.class),
                Mockito.mock(IndexedStringFactory.class),
                Mockito.mock(FormalizationStringIndexer.class),
                Mockito.mock(CategoryCreator.class),
                "path"
        );
        skuRuntimeKnowledge = Mockito.mock(SkuRuntimeKnowledge.class);
        matcherRuntimeKnowledge = Mockito.mock(MatcherRuntimeKnowledge.class);
        ReflectionTestUtils.setField(matchingService, "skutcherMaxQueue", 2);
        ReflectionTestUtils.setField(matchingService, "skuRuntimeKnowledge", skuRuntimeKnowledge);
        ReflectionTestUtils.setField(matchingService, "matcherRuntimeKnowledge", matcherRuntimeKnowledge);
    }

    @Test(timeout = 10_000)
    public void updateSkuthcerConcurrentCheck() throws InterruptedException {
        var latch = new CountDownLatch(3);
        Mockito.doAnswer(answer -> {
            Thread.sleep(1_000);
            latch.countDown();
            return null;
        }).when(skuRuntimeKnowledge).fullyReloadCategory(Mockito.anyLong(), Mockito.any(), Mockito.any());
        Mockito.when(categoryCache.getCategory(1)).thenReturn(Mockito.mock(CategoryKnowledge.class));
        Mockito.when(modelCache.getSkuModelsCache(1))
                .thenReturn(new EmptyCategoryModelsCache(1, ModelStorage.ModelType.SKU));
        Mockito.when(modelCache.getGuruModelsCache(1))
                .thenReturn(new EmptyCategoryModelsCache(1, ModelStorage.ModelType.SKU));

        List<ModelStorage.Model> models = models(1L);
        for (int i = 0; i < 10; i++) {
            //48 updates will be skipped
            matchingService.asynUpdate(models);
        }

        Assertions.assertThat(latch.await(5, TimeUnit.SECONDS)).isFalse();
        Assertions.assertThat(latch.getCount()).isEqualTo(1);
        matchingService.asynUpdate(models);
        Assertions.assertThat(latch.await(1500, TimeUnit.MILLISECONDS)).isTrue();
    }

    private List<ModelStorage.Model> models(Long categoryId) {
        return List.of(ModelStorage.Model.newBuilder().setCategoryId(categoryId).build());
    }
}
