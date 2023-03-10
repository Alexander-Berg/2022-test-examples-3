package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.bsexport.repository.resources.BsExportBannerPermalinksRepository;
import ru.yandex.direct.core.bsexport.resources.model.BannerPermalink;
import ru.yandex.direct.core.bsexport.resources.model.PermalinkAssignType;
import ru.yandex.direct.core.bsexport.resources.model.StatusPublish;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BaseBannerWithResourcesForBsExport;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.CpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.PermalinksInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerPermalinksLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private BsExportBannerPermalinksRepository bannerPermalinksRepository;
    private BannerPermalinksLoader loader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        this.bannerPermalinksRepository = mock(BsExportBannerPermalinksRepository.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.loader = new BannerPermalinksLoader(context, bannerPermalinksRepository);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?????? ???????????????? ???????????? ?? ?????????????? BANNER_PERMALINKSs, ?????????? ???????????? ???????????? ????????????(~??????
     * ????????????????)
     */
    @Test
    void deletedPermalinkTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .setDeleted(true)
                .build();
        var resourceFromDb = getBannerWithCommonFields();

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(null)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?????? ???????????????? ???????????? ?? ?????????????? BANNER_PERMALINKSs ?????? ?????????? ???????????? ?????? ??????????????,
     * ???? ???????????? ???? ?????????? ??????????????????
     */
    @Test
    void deletedPermalinkAndBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .setDeleted(true)
                .build();

        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = loader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ???????? ???????????? ?????????????????? ?? ?????????????? ?????????? Ready,
     * ???? ???????????? ???? ?????????? ??????????????????
     */
    @Test
    void readyBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.READY);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ???????? ?????? ????????????????????, ???????????????????? ?????? ??????????????, ???? ???????????? ???? ?????????? ??????????????????
     */
    @Test
    void noPermalinkInDbTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection())).thenReturn(List.of());
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ???????? ?????? ???????????????? ?????? ??????????????, ???? ???????????? ???? ?????????? ??????????????????
     */
    @Test
    void noCampaignForBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalink = new BannerPermalink()
                .withBannerId(1234L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED);
        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection())).thenReturn(List.of(permalink));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(0L)
                .withPermalinkHref("")
                .withPermalinkSite("")
                .withPermalinkDomainFilter("")
                .withPermalinkAssignType(null)
                .withPermalinkChainIds(List.of())
                .build();
        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ???????????? ????????????????????????????, ???? ?????????????????? ?????????? ??????????????????
     */
    @Test
    void moderatedBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalink = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withPermalinkAssignType(PermalinkAssignType.MANUAL)
                .withStatusPublish(StatusPublish.PUBLISHED);
        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection())).thenReturn(List.of(permalink));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(5454L)
                .withPermalinkHref("")
                .withPermalinkSite("")
                .withPermalinkDomainFilter("5454")
                .withPermalinkAssignType(PermalinkAssignType.MANUAL)
                .withPermalinkChainIds(List.of())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?????? cpm_geo_pin ???????????????? ?????????? ???????????????? href, site ?? domainFilter
     */
    @Test
    void cpmGeoPinBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = new CpmGeoPinBanner()
                .withId(1L)
                .withAdGroupId(3L)
                .withCampaignId(5L)
                .withBsBannerId(40L)
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalink = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withPermalinkAssignType(PermalinkAssignType.MANUAL)
                .withStatusPublish(StatusPublish.PUBLISHED);
        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection())).thenReturn(List.of(permalink));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(5454L)
                .withPermalinkHref("https://yandex.ru/profile/5454")
                .withPermalinkSite("yandex.ru")
                .withPermalinkDomainFilter("5454.ya-profile")
                .withPermalinkAssignType(PermalinkAssignType.MANUAL)
                .withPermalinkChainIds(List.of())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ?????????????? ???????? ???????????? auto ??????????????????, ???? href,site ?? domainFilter ?????????????????????? ??????????????
     */
    @Test
    void onlyAutoPermalinksTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalink = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.AUTO);
        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection())).thenReturn(List.of(permalink));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(5454L)
                .withPermalinkDomainFilter("")
                .withPermalinkSite("")
                .withPermalinkHref("")
                .withPermalinkAssignType(PermalinkAssignType.AUTO)
                .withPermalinkChainIds(List.of())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ?????????????? ???????? ?? auto ?? manual ??????????????????, ???? domainFilter ???????????????????? ?????? manual
     */
    @Test
    void autoAndManualPermalinksTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalinkManual = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.MANUAL);

        var permalinkAuto = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5455L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.AUTO);

        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection()))
                .thenReturn(List.of(permalinkManual, permalinkAuto));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(5454L)
                .withPermalinkDomainFilter("5454")
                .withPermalinkSite("")
                .withPermalinkHref("")
                .withPermalinkAssignType(PermalinkAssignType.MANUAL)
                .withPermalinkChainIds(List.of())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ?????????????? ???????????????????? ?????????? ?????????????? ???????????????????????? ??????????????, ???? href, site ?? domainFilter
     * ???????????????????? ?????????? ??????????????
     */
    @Test
    void manualPermalinkWithPreferVcardTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalink = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(true)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.MANUAL);
        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection())).thenReturn(List.of(permalink));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(5454L)
                .withPermalinkDomainFilter("")
                .withPermalinkSite("")
                .withPermalinkHref("")
                .withPermalinkAssignType(PermalinkAssignType.AUTO)
                .withPermalinkChainIds(List.of())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        var resources = res.getResources();
        assertThat(resources).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ???????? ???????????? ?????????????????? ?????????????? No,
     * ???? ?????????????????? ???? ?????????? ??????????????????
     */
    @Test
    void notModerateBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.NO);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ?????????????? BANNER_PERMALINKSs ???????????? ?????? ?????????????? bid ??????, ???? ?????????????? ???????? ???? ????????????????,
     * ???? ???????????? ???????????????????? ???? ??????????
     */
    @Test
    void notDeletedObjectButAbsentInDbTest() {
        mockOrderIdCalculator();
        var logicObject =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                        .build();

        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(logicObject));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????????? chainIds ?????????????????????????? ???? ?????????????????????? permalink_id, chain_id
     */
    @Test
    void chainIdsInRightOrderTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalinkManual = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.MANUAL)
                .withChainId(3L);

        var permalinkAuto1 = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5455L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.AUTO)
                .withChainId(1L);

        var permalinkAuto2 = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5456L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.UNPUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.AUTO)
                .withChainId(2L);

        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection()))
                .thenReturn(List.of(permalinkManual, permalinkAuto1, permalinkAuto2));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(5454L)
                .withPermalinkDomainFilter("5454")
                .withPermalinkSite("")
                .withPermalinkHref("")
                .withPermalinkAssignType(PermalinkAssignType.MANUAL)
                .withPermalinkChainIds(List.of(3L, 1L, 2L))
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???? ???????????????????? auto ?????????????????????? ???????????????????? ?????????????????? ?? ?????????????? ID
     */
    @Test
    void useAutoPermalinkWithMaxIdTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalinkAuto1 = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.AUTO);

        var permalinkAuto2 = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5456L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.AUTO);

        var permalinkAuto3 = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5455L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.PUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.AUTO);

        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection()))
                .thenReturn(List.of(permalinkAuto1, permalinkAuto2, permalinkAuto3));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(5456L)
                .withPermalinkDomainFilter("")
                .withPermalinkSite("")
                .withPermalinkHref("")
                .withPermalinkAssignType(PermalinkAssignType.AUTO)
                .withPermalinkChainIds(List.of())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???? ???????? ???????? ???????????? ???????? manual ?????????????????? ???? ???????????????? UNPUBLISHED, ???? ?????? ???????? ?????????? ????????????
     */
    @Test
    void onlyUnpublishedManualPermalinkTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalink = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.UNPUBLISHED)
                .withPermalinkAssignType(PermalinkAssignType.MANUAL);

        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection()))
                .thenReturn(List.of(permalink));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(0L)
                .withPermalinkDomainFilter("")
                .withPermalinkSite("")
                .withPermalinkHref("")
                .withPermalinkAssignType(null)
                .withPermalinkChainIds(List.of())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???? ???????? ???????? ???????????? ???????? manual ?????????????????? ???? ???????????????? UNKNOWN, ???? ?????? ???????? ?????????? ????????????
     */
    @Test
    void onlyUnknownPublishStatusManualPermalinkTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var permalink = new BannerPermalink()
                .withBannerId(1L)
                .withPermalinkId(5454L)
                .withPreferVcardOverPermalink(false)
                .withStatusPublish(StatusPublish.UNKNOWN)
                .withPermalinkAssignType(PermalinkAssignType.MANUAL);

        when(bannerPermalinksRepository.getPermalinks(eq(SHARD), anyCollection()))
                .thenReturn(List.of(permalink));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedPermalinkInfo = PermalinksInfo.builder()
                .withPermalinkId(0L)
                .withPermalinkDomainFilter("")
                .withPermalinkSite("")
                .withPermalinkHref("")
                .withPermalinkAssignType(null)
                .withPermalinkChainIds(List.of())
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedPermalinkInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    private BannerResource.Builder<PermalinksInfo> getResourceWithCommonFields() {
        return new BannerResource.Builder<PermalinksInfo>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setOrderId(30L)
                .setBsBannerId(40L);
    }

    private BaseBannerWithResourcesForBsExport getBannerWithCommonFields() {
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
