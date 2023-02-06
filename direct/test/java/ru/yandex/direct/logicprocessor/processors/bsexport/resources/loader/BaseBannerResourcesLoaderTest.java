package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.BaseBannerWithResourcesForBsExport;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.AdditionalInfo;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class BaseBannerResourcesLoaderTest {
    private static final int SHARD = 1;

    private BannerTypedRepository newBannerTypedRepository;
    private BsOrderIdCalculator bsOrderIdCalculator;
    private BannerResourcesLoaderContext context;
    private TestLoader testLoader;

    private static final List<Long> ADDITIONAL_BIDS = List.of(100L, 102L, 103L);

    @BeforeEach
    void before() {
        newBannerTypedRepository = mock(BannerTypedRepository.class);
        bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        testLoader = new TestLoader(context);
    }

    /**
     * Тест проверяет, что если есть 2 объекта с одинаковым bid, но первый будет с поменкой удаления, то выберется
     * только один из них
     */
    @Test
    void getBidsToDbBannerWithObjectDuplicateBidTest() {
        var object1 = new BsExportBannerResourcesObject.Builder()
                .setBid(12L).setMethod("method1").setReqid(1L).build();

        var object2 = new BsExportBannerResourcesObject.Builder()
                .setBid(12L).setMethod("method2").setReqid(2L).build();

        var object3 = new BsExportBannerResourcesObject.Builder()
                .setBid(12L).setMethod("method3").setReqid(3L).build();

        var bannerFromDb = new TextBanner()
                .withId(12L)
                .withBsBannerId(567L)
                .withCampaignId(3L);

        when(newBannerTypedRepository.getSafely(anyInt(), anyCollection(),
                eq(BaseBannerWithResourcesForBsExport.class)))
                .thenReturn(List.of(bannerFromDb));

        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection()))
                .thenReturn(Map.of(3L, 123456L));
        var gotBidsToDbBannerWithObject =
                testLoader.getBidsToDbBannerWithObject(SHARD, List.of(object1, object2, object3));
        var expectedBidsToDbBannerWithObject = Map.of(
                12L, new BannerFromDbWithLogicObject(bannerFromDb, 123456L, false));
        assertThat(gotBidsToDbBannerWithObject).isEqualToComparingFieldByFieldRecursively(expectedBidsToDbBannerWithObject);

    }

    /**
     * Тест проверяет, что если есть 2 объекта с одинаковым bid, но первый будет с поменкой удаления, то выберется
     * первый
     */
    @Test
    void getBidsToDbBannerWithObject_DuplicateBid_DeletedFirstTest() {
        var object1 = new BsExportBannerResourcesObject.Builder()
                .setBid(12L).setMethod("method1").setReqid(1L).setDeleted(true)
                .build();

        var object2 = new BsExportBannerResourcesObject.Builder()
                .setBid(12L).setMethod("method2").setReqid(2L).build();

        var bannerFromDb = new TextBanner()
                .withId(12L)
                .withBsBannerId(567L)
                .withCampaignId(3L);
        when(newBannerTypedRepository.getSafely(anyInt(), anyCollection(),
                eq(BaseBannerWithResourcesForBsExport.class)))
                .thenReturn(List.of(bannerFromDb));

        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection()))
                .thenReturn(Map.of(3L, 123456L));
        var gotBidsToDbBannerWithObject =
                testLoader.getBidsToDbBannerWithObject(SHARD, List.of(object1, object2));
        var expectedBidsToDbBannerWithObject = Map.of(
                12L, new BannerFromDbWithLogicObject(bannerFromDb, 123456L, true));
        assertThat(gotBidsToDbBannerWithObject).isEqualToComparingFieldByFieldRecursively(expectedBidsToDbBannerWithObject);
    }

    /**
     * Тест проверяет, что если есть 2 объекта с одинаковым bid, но второй будет с поменкой удаления, то выберется
     * второй
     */
    @Test
    void getBidsToDbBannerWithObject_DuplicateBid_DeletedSecondTest() {
        var object1 = new BsExportBannerResourcesObject.Builder()
                .setBid(12L).setMethod("method1").setReqid(1L)
                .build();

        var object2 = new BsExportBannerResourcesObject.Builder()
                .setBid(12L).setMethod("method2").setReqid(2L).setDeleted(true)
                .build();

        var bannerFromDb = new TextBanner()
                .withId(12L)
                .withBsBannerId(567L)
                .withCampaignId(3L);
        when(newBannerTypedRepository.getSafely(anyInt(), anyCollection(),
                eq(BaseBannerWithResourcesForBsExport.class)))
                .thenReturn(List.of(bannerFromDb));

        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection()))
                .thenReturn(Map.of(3L, 123456L));
        var gotBidsToDbBannerWithObject =
                testLoader.getBidsToDbBannerWithObject(SHARD, List.of(object1, object2));
        var expectedBidsToDbBannerWithObject = Map.of(
                12L, new BannerFromDbWithLogicObject(bannerFromDb, 123456L, true));
        assertThat(gotBidsToDbBannerWithObject).isEqualToComparingFieldByFieldRecursively(expectedBidsToDbBannerWithObject);

    }

    /**
     * Тест проверяет, что если есть у объекта есть изменения в таблицах, у которых нет bid, то bid'ы для таких
     * объектов будут подгружены и для них будут выбираться баннеры из репозитория
     */
    @Test
    void getBidsToDbBannerWithObject_AdditionalIdsTest() {
        var testLoaderWithAdditionalBids = new TestLoaderWithAdditionalBids(context);
        var objectWithAdditionalIds = new BsExportBannerResourcesObject.Builder()
                .setAdditionalId(1L)
                .build();

        var bid = 12L;
        var objectWithBid = new BsExportBannerResourcesObject.Builder()
                .setBid(bid)
                .build();

        testLoaderWithAdditionalBids
                .getBidsToDbBannerWithObject(SHARD, List.of(objectWithAdditionalIds, objectWithBid));
        var expectedBids = new ArrayList<>(ADDITIONAL_BIDS);
        expectedBids.add(bid);
        verify(newBannerTypedRepository)
                .getSafely(
                        eq(SHARD),
                        (Collection<Long>) argThat(bids -> containsInAnyOrder(expectedBids.toArray()).matches(bids)),
                        eq(BaseBannerWithResourcesForBsExport.class));
    }

    private static class TestLoader extends BaseBannerResourcesLoader<BaseBannerWithResourcesForBsExport, Integer> {

        public TestLoader(BannerResourcesLoaderContext context) {
            super(context);
        }

        @Override
        protected Class<BaseBannerWithResourcesForBsExport> getClassToLoadFromDb() {
            return BaseBannerWithResourcesForBsExport.class;
        }

        @Override
        protected Map<Long, Integer> getResources(int shard,
                                                  List<BaseBannerWithResourcesForBsExport> bannerWithResourceFromDb) {
            return Map.of();
        }
    }

    private static class TestLoaderWithAdditionalBids extends BaseBannerResourcesLoader<BaseBannerWithResourcesForBsExport, Integer> {

        public TestLoaderWithAdditionalBids(BannerResourcesLoaderContext context) {
            super(context);
        }

        @Override
        protected Class<BaseBannerWithResourcesForBsExport> getClassToLoadFromDb() {
            return BaseBannerWithResourcesForBsExport.class;
        }

        @Override
        List<Long> getAdditionalBids(int shard, Collection<AdditionalInfo> objects) {
            return ADDITIONAL_BIDS;
        }

        @Override
        protected Map<Long, Integer> getResources(int shard,
                                                  List<BaseBannerWithResourcesForBsExport> bannerWithResourceFromDb) {
            return Map.of();
        }
    }
}
