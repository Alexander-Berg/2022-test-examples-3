package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannerDeleteLoaderTest {

    private BsOrderIdCalculator bsOrderIdCalculator;
    private BannerDeleteLoader bannerDeleteLoader;
    private BannerTypedRepository bannerTypedRepository;

    private static final long DELETED_TIME = 1601635018L;
    private static final long BID = 123L;
    private static final long BS_BANNER_ID = 12345L;
    private static final long CID = 34L;
    private static final long PID = 67L;
    private static final long ORDER_ID = 11L;

    @BeforeEach
    void before() {
        var clock = Clock.fixed(Instant.ofEpochSecond(DELETED_TIME), UTC);
        bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        bannerTypedRepository = mock(BannerTypedRepository.class);
        this.bannerDeleteLoader = new BannerDeleteLoader(bsOrderIdCalculator, bannerTypedRepository, clock);
    }

    @Test
    void test() {
        var object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setBannerId(BS_BANNER_ID)
                .setCid(CID)
                .setPid(PID)
                .setResourceType(BannerResourceType.BANNER_DELETE)
                .build();

        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(Map.of(34L, 11L));
        var loaderResult = bannerDeleteLoader.loadResources(1, List.of(object));
        var expectedResource = new BannerResource.Builder<Long>()
                .setBid(BID)
                .setBsBannerId(BS_BANNER_ID)
                .setCid(CID)
                .setOrderId(ORDER_ID)
                .setPid(PID)
                .setResource(DELETED_TIME)
                .build();
        var expectedStat = new BannerResourcesStat().setCandidates(1).setSent(1);
        assertThat(loaderResult.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(loaderResult.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void bannerExistsTest() {
        var object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setBannerId(BS_BANNER_ID)
                .setCid(CID)
                .setPid(PID)
                .setResourceType(BannerResourceType.BANNER_DELETE)
                .build();

        mockBannerTypedRepository(BID);
        var loaderResult = bannerDeleteLoader.loadResources(1, List.of(object));
        verify(bsOrderIdCalculator, never()).calculateOrderIdIfNotExist(anyInt(), argThat(cids -> cids.contains(CID)));

        var expectedStat = new BannerResourcesStat().setCandidates(1).setSent(0);
        assertThat(loaderResult.getResources()).isEmpty();
        assertThat(loaderResult.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void testBannerWithoutBannerId() {
        var object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setBannerId(0L)
                .setCid(CID)
                .setPid(PID)
                .setResourceType(BannerResourceType.BANNER_DELETE)
                .build();

        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(Map.of(34L, 11L));
        var loaderResult = bannerDeleteLoader.loadResources(1, List.of(object));
        var expectedResource = new BannerResource.Builder<Long>()
                .setBid(BID)
                .setBsBannerId(72057594037928059L)
                .setCid(CID)
                .setOrderId(ORDER_ID)
                .setPid(PID)
                .setResource(DELETED_TIME)
                .build();
        var expectedStat = new BannerResourcesStat().setCandidates(1).setSent(1);
        assertThat(loaderResult.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(loaderResult.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void testBannerCampaignNotExist() {
        var object = new BsExportBannerResourcesObject.Builder()
                .setBid(BID)
                .setBannerId(BS_BANNER_ID)
                .setCid(CID)
                .setPid(PID)
                .setResourceType(BannerResourceType.BANNER_DELETE)
                .build();

        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(Map.of());
        var loaderResult = bannerDeleteLoader.loadResources(1, List.of(object));
        var expectedStat = new BannerResourcesStat().setCandidates(1).setSent(0);
        assertThat(loaderResult.getResources()).isEmpty();
        assertThat(loaderResult.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    private void mockBannerTypedRepository(List<Long> bids) {
        var banners = StreamEx.of(bids)
                .distinct()
                .map(bid -> new TextBanner().withId(bid))
                .toList();

        doReturn(banners)
                .when(bannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(Banner.class));
    }

    private void mockBannerTypedRepository(Long bid) {
        mockBannerTypedRepository(List.of(bid));
    }
}
