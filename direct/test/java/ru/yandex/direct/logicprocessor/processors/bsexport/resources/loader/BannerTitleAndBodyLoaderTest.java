package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithTitleAndBodyForBsExport;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.TitleAndBody;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.BannerTextFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerTitleAndBodyLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private BannerTitleAndBodyLoader loader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        var bannerTextFormatter = new BannerTextFormatter();
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.loader = new BannerTitleAndBodyLoader(context, bannerTextFormatter);
    }

    @Test
    void moderateBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TITLE_AND_BODY)
                        .build();

        BannerWithTitleAndBodyForBsExport resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withStatusModerate(BannerStatusModerate.YES)
                .withTitle("Купить часы DOM на 72% выгоднее")
                .withBody("Оригинальные часы по закупочным ценам.");
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTitleAndBodyForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(TitleAndBody.builder()
                        .setTitle("Купить часы DOM на 72% выгоднее")
                        .setBody("Оригинальные часы по закупочным ценам.").build())
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void moderatedDynamicBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TITLE_AND_BODY)
                        .build();

        DynamicBanner resourceFromDb = new DynamicBanner()
                .withId(1L)
                .withAdGroupId(3L)
                .withCampaignId(5L)
                .withBsBannerId(40L)
                .withStatusModerate(BannerStatusModerate.YES)
                .withTitle("{Динамический заголовок}")
                .withBody("Оригинальные часы по закупочным ценам.");

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTitleAndBodyForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(TitleAndBody.builder()
                        .setTitle("")
                        .setBody("Оригинальные часы по закупочным ценам.").build())
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void notModerateBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TITLE_AND_BODY)
                        .build();

        BannerWithTitleAndBodyForBsExport resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withStatusModerate(BannerStatusModerate.NO)
                .withTitle("Купить часы DOM на 72% выгоднее")
                .withBody("Оригинальные часы по закупочным ценам.");
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTitleAndBodyForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);

        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void bannerWithNullTitleAndBodyTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TITLE_AND_BODY)
                        .build();

        BannerWithTitleAndBodyForBsExport resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withStatusModerate(BannerStatusModerate.YES)
                .withTitle(null)
                .withBody(null);
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTitleAndBodyForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(TitleAndBody.builder()
                        .setTitle("")
                        .setBody("").build())
                .build();

        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    private BannerResource.Builder<TitleAndBody> getResourceWithCommonFields() {
        return new BannerResource.Builder<TitleAndBody>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setBsBannerId(40L)
                .setOrderId(30L);
    }

    private BannerWithTitleAndBodyForBsExport getBannerWithCommonFields() {
        return new TextBanner()
                .withId(1L)
                .withAdGroupId(3L)
                .withCampaignId(5L)
                .withBsBannerId(40L);
    }

    private void mockOrderIdCalculator() {
        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(Map.of(5L, 30L));
    }
}
