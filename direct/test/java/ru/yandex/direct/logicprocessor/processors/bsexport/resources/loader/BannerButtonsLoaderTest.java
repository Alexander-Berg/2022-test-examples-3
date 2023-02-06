package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.banner.resources.Button;
import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithButtonForBsExport;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.core.entity.hrefparams.service.HrefWithParamsBuildingService;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannerButtonsLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private BannerButtonsLoader bannerButtonsLoader;
    private BsOrderIdCalculator bsOrderIdCalculator;
    private HrefWithParamsBuildingService hrefWithParamsBuildingService;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        this.hrefWithParamsBuildingService = mock(HrefWithParamsBuildingService.class);
        doAnswer(invocation -> invocation.getArgument(2, String.class))
                .when(hrefWithParamsBuildingService)
                .buildHrefWithParamsByAdGroupId(anyInt(), anyLong(), anyString());
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.bannerButtonsLoader = new BannerButtonsLoader(context, hrefWithParamsBuildingService);
    }

    @Test
    void deletedButtonTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .setDeleted(true).build();

        BannerWithButtonForBsExport resourceFromDb = getBannerWithCommonFields();

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithButtonForBsExport.class));

        var res = bannerButtonsLoader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(null)
                .build();
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource);
    }

    @Test
    void deletedButtonAndBannerTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_BUTTON)
                .setDeleted(true)
                .build();


        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithButtonForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = bannerButtonsLoader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void readyButtonTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_BUTTON)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withButtonStatusModerate(BannerButtonStatusModerate.READY);
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithButtonForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = bannerButtonsLoader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void moderateButtonTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_BUTTON)
                .build();

        BannerWithButtonForBsExport resourceFromDb = getBannerWithCommonFields()
                .withButtonStatusModerate(BannerButtonStatusModerate.YES)
                .withButtonAction(ButtonAction.DOWNLOAD)
                .withButtonCaption("Download")
                .withButtonHref("https://ya.ru");

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithButtonForBsExport.class));

        var res = bannerButtonsLoader.loadResources(SHARD, List.of(object));
        var button = Button.newBuilder()
                .setButtonKey("download")
                .setButtonCaption("Download")
                .setButtonHref("https://ya.ru")
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        var expectedResource = getResourceWithCommonFields()
                .setResource(button)
                .build();
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void notModerateButtonTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_BUTTON)
                .build();

        BannerWithButtonForBsExport resourceFromDb = getBannerWithCommonFields()
                .withButtonStatusModerate(BannerButtonStatusModerate.NO)
                .withButtonAction(ButtonAction.DOWNLOAD)
                .withButtonCaption("Download")
                .withButtonHref("https://ya.ru");

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithButtonForBsExport.class));

        var res = bannerButtonsLoader.loadResources(SHARD, List.of(object));
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
                .setResourceType(BannerResourceType.BANNER_BUTTON)
                .build();

        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithButtonForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = bannerButtonsLoader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    @Test
    void callsHrefWithParamsBuildingServiceTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_BUTTON)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withButtonStatusModerate(BannerButtonStatusModerate.YES)
                .withButtonAction(ButtonAction.DOWNLOAD)
                .withButtonCaption("Download")
                .withButtonHref("https://ya.ru");

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), any(Collection.class), eq(BannerWithButtonForBsExport.class));

        bannerButtonsLoader.loadResources(SHARD, List.of(object));
        verify(hrefWithParamsBuildingService).buildHrefWithParamsByAdGroupId(anyInt(), anyLong(), anyString());
    }

    private BannerResource.Builder<Button> getResourceWithCommonFields() {
        return new BannerResource.Builder<Button>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setBsBannerId(40L)
                .setOrderId(30L);
    }

    private BannerWithButtonForBsExport getBannerWithCommonFields() {
        return new CpmBanner()
                .withId(1L)
                .withAdGroupId(3L)
                .withCampaignId(5L)
                .withBsBannerId(40L);
    }

    private void mockOrderIdCalculator() {
        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(Map.of(5L, 30L));
    }
}
