package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.bsexport.repository.resources.BsExportBannerVcardsRepository;
import ru.yandex.direct.core.bsexport.resources.model.Vcard;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithVcardForBsExport;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.core.entity.vcard.model.Phone;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.VcardInfo;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.DomainFilterService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannerVcardLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private DomainFilterService domainFilterService;
    private BsExportBannerVcardsRepository vcardsRepository;
    private BannerVcardLoader loader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        this.domainFilterService = mock(DomainFilterService.class);
        this.vcardsRepository = mock(BsExportBannerVcardsRepository.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.loader = new BannerVcardLoader(context, vcardsRepository, domainFilterService);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?????????????? ?????????????????????????????? ?? ?? ?????? ???????? ??????????????, ???? ???????????????????? ???????????????????? ??????????
     */
    @Test
    void test() {
        mockOrderIdCalculator();
        var vcardId = 45L;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setPid(2L)
                .setCid(3L)
                .setResourceType(BannerResourceType.BANNER_VCARD)
                .build();
        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withVcardStatusModerate(BannerVcardStatusModerate.YES)
                .withVcardId(vcardId);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithVcardForBsExport.class));

        var phone = new Phone().withCountryCode("+7").withCityCode("499").withPhoneNumber("111-11-11");

        doReturn(Map.of(vcardId, new Vcard().withVcardId(vcardId).withPhone(phone)))
                .when(vcardsRepository).getVcards(anyInt(), anyCollection());

        var domain = "74991111111.phone";
        var domainFilter = "site.com";

        doReturn(Map.of(domain, domainFilter))
                .when(domainFilterService).getDomainsFilters(argThat(domains -> domains.contains(domain)));

        var expectedVcardInfo = VcardInfo.builder().withDomainFilter(domainFilter).build();
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(expectedVcardInfo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }


    /**
     * ???????? ??????????????????, ?????? ???????? ?? ?????????????????????????????????? ?????????????? ?????? ????????????????, ???? ???????????????????? ???????????? ????????????
     */
    @Test
    void noPhoneInVcard() {
        mockOrderIdCalculator();
        var vcardId = 45L;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setPid(2L)
                .setCid(3L)
                .setResourceType(BannerResourceType.BANNER_VCARD)
                .build();
        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withVcardStatusModerate(BannerVcardStatusModerate.YES)
                .withVcardId(vcardId);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithVcardForBsExport.class));


        doReturn(Map.of(vcardId, new Vcard().withVcardId(vcardId).withPhone(null)))
                .when(vcardsRepository).getVcards(anyInt(), anyCollection());


        verify(domainFilterService, never()).getDomainsFilters(anyCollection());

        var expectedVcardInfo = VcardInfo.builder().withDomainFilter("").build();
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(expectedVcardInfo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ?????????????? ?????????????? ???????????? ?? ???????????? ?????????????????? new, ???? ???????????????????? ???????????? ????????????
     */
    @Test
    void nullVcard_PhoneFlagNewTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setPid(2L)
                .setCid(3L)
                .setResourceType(BannerResourceType.BANNER_VCARD)
                .build();
        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withVcardStatusModerate(BannerVcardStatusModerate.NEW)
                .withVcardId(null);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithVcardForBsExport.class));


        verify(vcardsRepository, never()).getVcards(anyInt(), anyCollection());


        verify(domainFilterService, never()).getDomainsFilters(anyCollection());

        var expectedVcardInfo = VcardInfo.builder().withDomainFilter("").build();
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(expectedVcardInfo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ?????????????? ?????????????? ???????????? ?? ???????????? ?????????????????? new, ???? ???????????????????? ???????????? ????????????
     */
    @Test
    void nullVcard_PhoneFlagYesTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setPid(2L)
                .setCid(3L)
                .setResourceType(BannerResourceType.BANNER_VCARD)
                .build();
        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withVcardStatusModerate(BannerVcardStatusModerate.YES)
                .withVcardId(null);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithVcardForBsExport.class));


        verify(vcardsRepository, never()).getVcards(anyInt(), anyCollection());


        verify(domainFilterService, never()).getDomainsFilters(anyCollection());

        var expectedVcardInfo = VcardInfo.builder().withDomainFilter("").build();
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(expectedVcardInfo)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?? ?? ?????????????? vcards ?????? ?????????????? ??????????????, ???? ???????????????????? ???????????? ????????????
     */
    @Test
    void noVcardForBannerTest() {
        mockOrderIdCalculator();
        var vcardId = 45L;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setPid(2L)
                .setCid(3L)
                .setResourceType(BannerResourceType.BANNER_VCARD)
                .build();
        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withVcardStatusModerate(BannerVcardStatusModerate.YES)
                .withVcardId(vcardId);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithVcardForBsExport.class));

        doReturn(Map.of())
                .when(vcardsRepository).getVcards(anyInt(), anyCollection());

        verify(domainFilterService, never()).getDomainsFilters(anyCollection());

        var res = loader.loadResources(SHARD, List.of(object));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?????????????? ?????????????????? ???? ??????????????????, ???? ?? ???????????????? domainFiler ???????????????????? ???????????? ????????????
     */
    @Test
    void notModerateVcardForBannerTest() {
        mockOrderIdCalculator();
        var vcardId = 45L;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setPid(2L)
                .setCid(3L)
                .setResourceType(BannerResourceType.BANNER_VCARD)
                .build();
        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withVcardStatusModerate(BannerVcardStatusModerate.NO)
                .withVcardId(vcardId);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithVcardForBsExport.class));


        verify(vcardsRepository, never()).getVcards(anyInt(), anyCollection());

        verify(domainFilterService, never()).getDomainsFilters(anyCollection());

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedVcardInfo = VcardInfo.builder().withDomainFilter("").build();
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(expectedVcardInfo)
                .build();

        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * ???????? ??????????????????, ?????? ???????? ?????????? ?????????????????? ?????????????? ?????????? ready, ???? ?????? ???? ?????????? ????????????????????
     */
    @Test
    void readyVcardForBannerTest() {
        mockOrderIdCalculator();
        var vcardId = 45L;
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setPid(2L)
                .setCid(3L)
                .setResourceType(BannerResourceType.BANNER_VCARD)
                .build();
        var resourceFromDb = getBannerWithCommonFields();
        resourceFromDb.withVcardStatusModerate(BannerVcardStatusModerate.READY)
                .withVcardId(vcardId);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithVcardForBsExport.class));


        verify(vcardsRepository, never()).getVcards(anyInt(), anyCollection());

        verify(domainFilterService, never()).getDomainsFilters(anyCollection());

        var res = loader.loadResources(SHARD, List.of(object));


        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }


    private BannerResource.Builder<VcardInfo> getResourceWithCommonFields() {
        return new BannerResource.Builder<VcardInfo>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setOrderId(30L)
                .setBsBannerId(40L);
    }

    private BannerWithVcardForBsExport getBannerWithCommonFields() {
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
