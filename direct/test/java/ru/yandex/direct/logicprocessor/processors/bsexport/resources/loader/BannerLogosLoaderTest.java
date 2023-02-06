package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.banner.resources.Format;
import ru.yandex.adv.direct.banner.resources.Logo;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithLogoForBsExport;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.image.model.BannerImageFormatNamespace;
import ru.yandex.direct.core.entity.image.model.ImageFormat;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.image.model.AvatarHost.AVATARS_MDS_YANDEX_NET;

class BannerLogosLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private BannerImageFormatRepository bannerImageFormatRepository;
    private BannerLogosLoader bannerLogosLoader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bannerImageFormatRepository = mock(BannerImageFormatRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.bannerLogosLoader = new BannerLogosLoader(context, bannerImageFormatRepository);
    }

    @Test
    void deletedLogoTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_LOGO)
                .setDeleted(true)
                .build();
        BannerWithLogoForBsExport resourceFromDb = getBannerWithCommonFields();

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithLogoForBsExport.class));

        var res = bannerLogosLoader.loadResources(SHARD, List.of(object));
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(null)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void deletedLogoAndBannerTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_LOGO)
                .setDeleted(true)
                .build();

        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithLogoForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = bannerLogosLoader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void readyLogoTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_LOGO)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withLogoStatusModerate(BannerLogoStatusModerate.READY);
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithLogoForBsExport.class));

        var res = bannerLogosLoader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void moderateLogoTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_LOGO)
                .build();

        BannerWithLogoForBsExport resourceFromDb = getBannerWithCommonFields()
                .withLogoImageHash("RqvjpvZ5M4gxsLOCGVNYZQ")
                .withLogoStatusModerate(BannerLogoStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithLogoForBsExport.class));

        var bannerImageFormatX80 = new BannerImageFormat()
                .withImageHash("RqvjpvZ5M4gxsLOCGVNYZQ")
                .withAvatarsHost(AVATARS_MDS_YANDEX_NET)
                .withMdsGroupId(2398261)
                .withNamespace(BannerImageFormatNamespace.DIRECT)
                .withFormats(Map.of("x80", new ImageFormat().withHeight(53).withWidth(80)));


        doReturn(Map.of("RqvjpvZ5M4gxsLOCGVNYZQ", bannerImageFormatX80))
                .when(bannerImageFormatRepository)
                .getBannerImageFormats(anyInt(), anyCollection());

        var res = bannerLogosLoader.loadResources(SHARD, List.of(object));
        var logo = Logo.newBuilder()
                .addFormats(Format.newBuilder()
                        .setFormat("x80")
                        .setHeight(53)
                        .setWidth(80)
                        .setUrl("https://avatars.mds.yandex.net/get-direct/2398261/RqvjpvZ5M4gxsLOCGVNYZQ/x80")
                        .build())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(logo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);

    }

    @Test
    void moderateLogo_FormatNotX80Test() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_LOGO)
                .build();

        BannerWithLogoForBsExport resourceFromDb = getBannerWithCommonFields()
                .withLogoImageHash("RqvjpvZ5M4gxsLOCGVNYZQ")
                .withLogoStatusModerate(BannerLogoStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithLogoForBsExport.class));

        var bannerImageFormat = new BannerImageFormat()
                .withImageHash("RqvjpvZ5M4gxsLOCGVNYZQ")
                .withAvatarsHost(AVATARS_MDS_YANDEX_NET)
                .withMdsGroupId(2398261)
                .withNamespace(BannerImageFormatNamespace.DIRECT)
                .withFormats(Map.of("x450", new ImageFormat().withHeight(300).withWidth(450)));


        doReturn(Map.of("RqvjpvZ5M4gxsLOCGVNYZQ", bannerImageFormat))
                .when(bannerImageFormatRepository)
                .getBannerImageFormats(anyInt(), anyCollection());

        var res = bannerLogosLoader.loadResources(SHARD, List.of(object));
        var logo = Logo.newBuilder().build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(logo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void moderateLogo_FormatNotX80_ExtendedLogoBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_LOGO)
                .build();

        BannerWithLogoForBsExport resourceFromDb = getExtendedLogoBannerWithCommonFields()
                .withLogoImageHash("RqvjpvZ5M4gxsLOCGVNYZQ")
                .withLogoStatusModerate(BannerLogoStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithLogoForBsExport.class));

        var bannerImageFormat = new BannerImageFormat()
                .withImageHash("RqvjpvZ5M4gxsLOCGVNYZQ")
                .withAvatarsHost(AVATARS_MDS_YANDEX_NET)
                .withMdsGroupId(2398261)
                .withNamespace(BannerImageFormatNamespace.DIRECT)
                .withFormats(Map.of("x450", new ImageFormat().withHeight(300).withWidth(450)));


        doReturn(Map.of("RqvjpvZ5M4gxsLOCGVNYZQ", bannerImageFormat))
                .when(bannerImageFormatRepository)
                .getBannerImageFormats(anyInt(), anyCollection());

        var res = bannerLogosLoader.loadResources(SHARD, List.of(object));
        var logo = Logo.newBuilder()
                .addFormats(Format.newBuilder()
                        .setFormat("x450")
                        .setHeight(300)
                        .setWidth(450)
                        .setUrl("https://avatars.mds.yandex.net/get-direct/2398261/RqvjpvZ5M4gxsLOCGVNYZQ/x450")
                        .build())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(logo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void notModerateLogoTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_LOGO)
                .build();

        BannerWithLogoForBsExport resourceFromDb = getBannerWithCommonFields()
                .withLogoStatusModerate(BannerLogoStatusModerate.NO);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithLogoForBsExport.class));

        var res = bannerLogosLoader.loadResources(SHARD, List.of(object));
        var expectedResource = getResourceWithCommonFields()
                .setResource(null)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void notDeletedObjectButAbsentInDbTest() {
        var logicObject =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_LOGO)
                        .build();

        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithLogoForBsExport.class));

        var res = bannerLogosLoader.loadResources(SHARD, List.of(logicObject));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    private BannerResource.Builder<Logo> getResourceWithCommonFields() {
        return new BannerResource.Builder<Logo>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setOrderId(30L)
                .setBsBannerId(40L);
    }

    private BannerWithLogoForBsExport getBannerWithCommonFields() {
        return new CpmBanner()
                .withId(1L)
                .withAdGroupId(3L)
                .withCampaignId(5L)
                .withBsBannerId(40L);
    }

    private BannerWithLogoForBsExport getExtendedLogoBannerWithCommonFields() {
        return new PerformanceBannerMain()
                .withId(1L)
                .withAdGroupId(3L)
                .withCampaignId(5L)
                .withBsBannerId(40L);
    }

    private void mockOrderIdCalculator() {
        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(Map.of(5L, 30L));
    }
}
