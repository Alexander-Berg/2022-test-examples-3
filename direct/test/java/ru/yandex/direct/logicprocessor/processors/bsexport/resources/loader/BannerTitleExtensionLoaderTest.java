package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithTitleExtensionForBsExport;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.BannerTextFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerTitleExtensionLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private BannerTitleExtensionLoader loader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        var bannerTextFormatter = new BannerTextFormatter();
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.loader = new BannerTitleExtensionLoader(context, bannerTextFormatter);
    }

    @Test
    void moderateBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TITLE_EXTENSION)
                        .build();

        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withStatusModerate(BannerStatusModerate.YES)
                .withTitleExtension("По МО по выгодной цене");
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTitleExtensionForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource("По МО по выгодной цене")
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
                        .setResourceType(BannerResourceType.BANNER_TITLE_EXTENSION)
                        .build();

        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withStatusModerate(BannerStatusModerate.NO)
                .withTitleExtension("По МО по выгодной цене");
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTitleExtensionForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);

        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void bannerWithNullTitleExtensionTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TITLE_EXTENSION)
                        .build();

        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withStatusModerate(BannerStatusModerate.YES)
                .withTitleExtension(null);
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTitleExtensionForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedBannerResource = getResourceWithCommonFields()
                .setResource("")
                .build();

        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    private BannerResource.Builder<String> getResourceWithCommonFields() {
        return new BannerResource.Builder<String>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setBsBannerId(40L)
                .setOrderId(30L);
    }

    private BannerWithTitleExtensionForBsExport getBannerWithCommonFields() {
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
