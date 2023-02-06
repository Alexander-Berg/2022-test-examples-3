package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.BannerNameStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithNameForBsExport;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerNamesLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private BannerNamesLoader bannerNamesLoader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.bannerNamesLoader = new BannerNamesLoader(context);
    }

    @Test
    void deletedNameTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_NAME)
                        .setDeleted(true).build();

        BannerWithNameForBsExport resourceFromDb = getBannerWithCommonFields();

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithNameForBsExport.class));

        var res = bannerNamesLoader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(null)
                .build();
        assertThat(res.getResources()).usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void deletedNameAndBannerTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_NAME)
                .setDeleted(true)
                .build();


        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithNameForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = bannerNamesLoader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void readyNameTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_NAME)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withNameStatusModerate(BannerNameStatusModerate.READY)
                .withName("SomeName");
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithNameForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = bannerNamesLoader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void moderateNameTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_NAME)
                .build();

        String name = "TestName";
        BannerWithNameForBsExport resourceFromDb = getBannerWithCommonFields()
                .withNameStatusModerate(BannerNameStatusModerate.YES)
                .withName(name);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithNameForBsExport.class));

        var res = bannerNamesLoader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var expectedResource = getResourceWithCommonFields()
                .setResource(name)
                .build();
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void notModerateNameTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_NAME)
                .build();

        String name = "TestName";
        BannerWithNameForBsExport resourceFromDb = getBannerWithCommonFields()
                .withNameStatusModerate(BannerNameStatusModerate.NO)
                .withName(name);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithNameForBsExport.class));

        var res = bannerNamesLoader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var expectedResource = getResourceWithCommonFields()
                .setResource(null)
                .build();
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void notDeletedObjectButAbsentInDbTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_NAME)
                .build();

        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithNameForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = bannerNamesLoader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
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

    private BannerWithNameForBsExport getBannerWithCommonFields() {
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
