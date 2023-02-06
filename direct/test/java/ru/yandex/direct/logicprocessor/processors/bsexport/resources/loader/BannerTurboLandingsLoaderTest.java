package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLandingForBsExport;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.TurboLandingInfo;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.parameterizer.BsHrefParametrizingService;
import ru.yandex.direct.logicprocessor.processors.bsexport.utils.CampaignNameTransliterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerTurboLandingsLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private TurboLandingRepository turboLandingRepository;
    private CampaignTypedRepository campaignTypedRepository;
    private BsHrefParametrizingService bsHrefParametrizingService;
    private CampaignNameTransliterator campaignNameTransliterator;
    private BannerTurboLandingsLoader loader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.turboLandingRepository = mock(TurboLandingRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        this.campaignTypedRepository = mock(CampaignTypedRepository.class);
        this.bsHrefParametrizingService = mock(BsHrefParametrizingService.class);
        this.campaignNameTransliterator = mock(CampaignNameTransliterator.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.loader = new BannerTurboLandingsLoader(context, turboLandingRepository, campaignTypedRepository,
                bsHrefParametrizingService, campaignNameTransliterator);
    }

    /**
     * Тест проверяет, что если при удалении записи в таблице banner_turbolandings, будет собран пустой ресурс
     * (~его удалению)
     */
    @Test
    void deletedTurbolandingTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                .setDeleted(true)
                .build();
        BannerWithTurboLandingForBsExport resourceFromDb = getBannerWithCommonFields();

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(null)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Тест проверяет, что если при удалении записи в таблице banner_turbolandings для такой записи нет баннера,
     * то ресурс не будет отправлен
     */
    @Test
    void deletedTurboLandingAndBannerTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                .setDeleted(true)
                .build();

        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        var res = loader.loadResources(SHARD, List.of(object));
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Тест проверяет, что если в базе статус модерации у связки баннер-турболендинг в статусе Ready,
     * то ресурс не будет отправлен
     */
    @Test
    void readyTurboLandingTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.READY);
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Тест проверяет, что если в базе нет турболендинга, указанного для баннера, то ресурс не будет отправлен
     */
    @Test
    void noTurboLandingInDbTest() {
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES);
        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        when(turboLandingRepository.getTurboLandings(eq(SHARD), anyCollection())).thenReturn(Map.of());
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Тест проверяет, что если в базе нет кампании для баннера, то ресурс не будет отправлен
     */
    @Test
    void noCampaignForTurboLandingTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES)
                .withTurboLandingId(1234L);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        var turbolanding = new TurboLanding()
                .withId(1234L)
                .withUrl("https://yandex" +
                        ".ru/turbo?text=lpc%2F0b8ddc8865dc76eef6bcd7ae58383fe44cedc3175ea908f79dc745e64a9f3031&promo" +
                        "=nomooa&no_friendly_url=1&clck_host=yandex.ru%2Fclck");
        when(turboLandingRepository.getTurboLandings(eq(SHARD), anyCollection())).thenReturn(Map.of(1234L,
                turbolanding));
        when(campaignTypedRepository.getSafely(eq(SHARD), anyCollection(), eq(CommonCampaign.class)))
                .thenReturn(List.of());

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);

        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Тест проверяет, что если у связь баннера с турболендингом промедерирована, то турболендинг будет отправлен
     */
    @Test
    void moderatedTurboLandingTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES)
                .withTurboLandingId(1234L);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        var turbolanding = new TurboLanding()
                .withId(1234L)
                .withUrl("https://yandex" +
                        ".ru/turbo?text=lpc%2F0b8ddc8865dc76eef6bcd7ae58383fe44cedc3175ea908f79dc745e64a9f3031&promo" +
                        "=nomooa&no_friendly_url=1&clck_host=yandex.ru%2Fclck");
        when(turboLandingRepository.getTurboLandings(eq(SHARD), anyCollection())).thenReturn(Map.of(1234L,
                turbolanding));
        when(campaignTypedRepository.getSafely(eq(SHARD), anyCollection(), eq(CommonCampaign.class)))
                .thenReturn(List.of(new TextCampaign().withId(5L).withType(CampaignType.TEXT).withName("Кампания 1")));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedTurboLandingInfo = TurboLandingInfo.builder()
                .withTurbolandingId(1234L)
                .withHref("https://yandex" +
                        ".ru/turbo?text=lpc%2F0b8ddc8865dc76eef6bcd7ae58383fe44cedc3175ea908f79dc745e64a9f3031&promo" +
                        "=nomooa&no_friendly_url=1&clck_host=yandex.ru%2Fclck")
                .withSite("yandex.ru")
                .withDomainFilter("1234.y-turbo")
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedTurboLandingInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Тест проверяет, что если у турболендинга есть параметры, то они подставятся к ссылке
     */
    @Test
    void turboLandingWithParametersTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                .build();

        var resourceFromDb = getBannerWithCommonFields()
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES)
                .withTurboLandingHrefParams("utm_source={source}&utm_medium=cpc&utm_campaign={campaign_id" +
                        "}-{campaign_name_lat}&utm_content={position_type}.{position}&utm_term={keyword}")
                .withTurboLandingId(1234L);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        var turbolanding = new TurboLanding()
                .withId(1234L)
                .withUrl("https://yandex" +
                        ".ru/turbo?text=lpc%2F0b8ddc8865dc76eef6bcd7ae58383fe44cedc3175ea908f79dc745e64a9f3031&promo" +
                        "=nomooa&no_friendly_url=1&clck_host=yandex.ru%2Fclck");
        when(turboLandingRepository.getTurboLandings(eq(SHARD), anyCollection())).thenReturn(Map.of(1234L,
                turbolanding));
        when(campaignTypedRepository.getSafely(eq(SHARD), anyCollection(), eq(CommonCampaign.class)))
                .thenReturn(List.of(new TextCampaign().withId(5L).withType(CampaignType.TEXT).withName("Кампания 1")));

        when(bsHrefParametrizingService.parameterize(any(), any())).thenReturn("utm_source={SOURCE}&utm_medium=cpc" +
                "&utm_campaign=5-Kampaniya_1&utm_content={PTYPE}.{POS}&utm_term={PHRASE}");
        when(campaignNameTransliterator.translit(anyString())).thenReturn("Kampaniya_1");
        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedTurboLandingInfo = TurboLandingInfo.builder()
                .withTurbolandingId(1234L)
                .withHref("https://yandex" +
                        ".ru/turbo?text=lpc%2F0b8ddc8865dc76eef6bcd7ae58383fe44cedc3175ea908f79dc745e64a9f3031&promo" +
                        "=nomooa&no_friendly_url=1&clck_host=yandex" +
                        ".ru%2Fclck&utm_source={SOURCE}&utm_medium=cpc&utm_campaign=5-Kampaniya_1&utm_content={PTYPE}" +
                        ".{POS}&utm_term={PHRASE}")
                .withSite("yandex.ru")
                .withDomainFilter("1234.y-turbo")
                .build();

        var expectedResource = getResourceWithCommonFields()
                .setResource(expectedTurboLandingInfo)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Тест проверяет, что если в базе статус модерации у связки баннер-турболендинг в статусе No,
     * то будет собран пустой ресурс(~его удалению)
     */
    @Test
    void notModerateTurboLandingTest() {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                .build();

        BannerWithTurboLandingForBsExport resourceFromDb = getBannerWithCommonFields()
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.NO);

        doReturn(List.of(resourceFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedResource = getResourceWithCommonFields()
                .setResource(null)
                .build();
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);
        assertThat(res.getResources()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    /**
     * Тест проверяет, что если в таблице banner_turbolandings записи для нужного bid нет, но событие было не удаление,
     * то ничего отправлено не будет
     */
    @Test
    void notDeletedObjectButAbsentInDbTest() {
        mockOrderIdCalculator();
        var logicObject =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .build();

        doReturn(List.of())
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BannerWithTurboLandingForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(logicObject));
        var expectedStat = new BannerResourcesStat().setSent(0).setCandidates(1);
        assertThat(res.getResources()).isEmpty();
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    private BannerResource.Builder<TurboLandingInfo> getResourceWithCommonFields() {
        return new BannerResource.Builder<TurboLandingInfo>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setOrderId(30L)
                .setBsBannerId(40L);
    }

    private BannerWithTurboLandingForBsExport getBannerWithCommonFields() {
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
