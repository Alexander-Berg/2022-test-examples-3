package ru.yandex.direct.core.entity.auction.service;

import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.auction.container.AdGroupForAuction;
import ru.yandex.direct.core.entity.auction.container.BsRequestPhraseWrapper;
import ru.yandex.direct.core.entity.auction.type.BsAuctionRequestTypeSupportFacade;
import ru.yandex.direct.core.entity.auction.type.support.ContentPromotionBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.McBannerBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.MobileContentBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.TextBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.autobroker.service.AutoBrokerCalculatorProviderService;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.href.BannerDomainRepository;
import ru.yandex.direct.core.entity.bids.interpolator.InterpolatorService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignDeviceTargeting;
import ru.yandex.direct.core.entity.campaign.model.CampaignOpts;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.keyword.model.ForecastCtr;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordForecastService;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.currencies.CurrencyRub;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.bsauction.BidCalculationMethod.GROUP_BID;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting.PHONE;

public class BsAuctionServiceToBsRequestTest {
    private BsAuctionService bsAuctionService;
    private KeywordForecastService keywordForecastService;
    private BsTrafaretClient bsTrafaretClient;
    private AutoBrokerCalculatorProviderService autoBrokerCalculatorProviderService;
    private IdentityHashMap<Keyword, ForecastCtr> forecastCtrsByKeyword = new IdentityHashMap<>();
    private CampaignService campaignService;
    private InterpolatorService interpolatorService;
    private BannerDomainRepository bannerDomainRepository;

    @Before
    public void setUp() {
        keywordForecastService = mock(KeywordForecastService.class);
        bsTrafaretClient = mock(BsTrafaretClient.class);
        autoBrokerCalculatorProviderService = mock(AutoBrokerCalculatorProviderService.class);
        campaignService = mock(CampaignService.class);
        interpolatorService = mock(InterpolatorService.class);
        Answer<?> answer = invocation -> invocation.getArguments()[1];
        doAnswer(answer).when(interpolatorService).getInterpolatedTrafaretBidItems(any(), anyList(), any(), any());
        bsAuctionService = new BsAuctionService(keywordForecastService, autoBrokerCalculatorProviderService,
                bsTrafaretClient, bsTrafaretClient, false, campaignService, interpolatorService,
                new BsAuctionRequestTypeSupportFacade(asList(
                        new TextBsAuctionRequestTypeSupport(),
                        new MobileContentBsAuctionRequestTypeSupport(),
                        new ContentPromotionBsAuctionRequestTypeSupport(),
                        new McBannerBsAuctionRequestTypeSupport())), bannerDomainRepository);
    }

    @Test
    public void withNullCampaign() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        when(adGroupForAuction.getCampaign()).thenReturn(null);
        Throwable thrown = catchThrowable(() -> bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of()));
        assertThat(thrown).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Campaign not found");
    }

    @Test
    public void withNullAdGroup() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        when(adGroupForAuction.getAdGroup()).thenReturn(null);
        Throwable thrown = catchThrowable(() -> bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of()));
        assertThat(thrown).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("AdGroup not found");
    }

    @Test
    public void nullIfBannerIsNull() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        when(adGroupForAuction.getBanner()).thenReturn(null);
        List<BsRequest<BsRequestPhraseWrapper>> bsRequest
                = bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of());
        assertThat(bsRequest).isNull();
    }

    @Test
    public void nullIfPhrasesAreNull() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        when(adGroupForAuction.getKeywords()).thenReturn(null);
        List<BsRequest<BsRequestPhraseWrapper>> bsRequest
                = bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of());
        assertThat(bsRequest).isNull();
    }

    @Test
    public void nullForImageBanner() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        ImageBanner imageBanner = new ImageBanner();
        when(adGroupForAuction.getBanner()).thenReturn(imageBanner);

        List<BsRequest<BsRequestPhraseWrapper>> bsRequest
                = bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of());
        assertThat(bsRequest).isNull();
    }

    @Test
    public void orderId() {
        long orderId = 1L;
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        adGroupForAuction.getCampaign().withOrderId(orderId);

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of()));

        assertThat(bsRequest.getOrderId()).isEqualTo(orderId);
    }

    @Test
    public void noExtendedGeotargeting() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        EnumSet<CampaignOpts> opts = EnumSet.of(CampaignOpts.NO_EXTENDED_GEOTARGETING);
        adGroupForAuction.getCampaign().withOpts(opts);

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of()));

        assertThat(bsRequest.getNoExtendedGeotargeting()).isEqualTo(true);
    }

    @Test
    public void withoutNoExtendedGeotargeting() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of()));

        assertThat(bsRequest.getNoExtendedGeotargeting()).isEqualTo(false);
    }

    @Test
    public void timeTableOn() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        adGroupForAuction.getCampaign().withFairAuction(true);

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.isTimetable()).isEqualTo(true);
    }

    @Test
    public void timeTableOff() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.isTimetable()).isEqualTo(false);
    }

    @Test
    public void onlyMobilePagesOn() {
        AdGroupForAuction adGroupForAuction = mobileAdGroupForAuction();

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.isOnlyMobilePages()).isEqualTo(true);
    }

    @Test
    public void trafaretForMobile() {
        AdGroupForAuction adGroupForAuction = mobileAdGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        String url = bsRequest.getUrlBuilder("").build();
        assertThat(url).contains("operation=3");
        assertThat(url).contains("only-mobile-pages=1");

        assertThat(url).contains(
                BsRequest.getParamNameRankOptionId() + "=" + BsRequest
                        .getTrafaretMobileAndDeviceTargetingRankOptionIdValue()
        );
        assertThat(url).contains(BsRequest.getParamNameTargetDeviceGroup() + "=1");
        assertThat(url).contains(BsRequest.getParamNameTargetDetailedDeviceType() + "=2");
    }

    @Test
    public void toBsRequest_ContentPromotionVideoAdGroup_PageIdIsCorrect() {
        AdGroupForAuction adGroupForAuction = contentPromotionVideoAdGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getPageId()).isEqualTo(449809);
    }

    @Test
    public void toBsRequest_TextAdGroup_PageIdIsCorrect() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getPageId()).isEqualTo(24);
    }

    @Test
    public void toBsRequest_ContentPromotionVideoAdGroup_OperationIsCorrect() {
        AdGroupForAuction adGroupForAuction = contentPromotionVideoAdGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getBidCalcMethod()).isEqualTo(GROUP_BID);
    }

    @Test
    public void toBsRequest_ContentPromotionVideoAdGroup_MainCtrPredictionIdIsCorrect() {
        AdGroupForAuction adGroupForAuction = contentPromotionVideoAdGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getMainCtrPredictionId()).isEqualTo(10335);
    }

    @Test
    public void toBsRequest_TextAdGroup_MainCtrPredictionIdIsCorrect() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getMainCtrPredictionId()).isNull();
    }

    @Test
    public void toBsRequest_ContentPromotionVideoAdGroup_UrlIsCorrect() {
        AdGroupForAuction adGroupForAuction = contentPromotionVideoAdGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        String url = bsRequest.getUrlBuilder("http://bsrank.yandex.ru/rank").build();

        assertThat(url).startsWith("http://bsrank.yandex.ru/rank/449809?");
        assertThat(url).contains("operation=3");
        assertThat(url).contains("main-ctr-pred-id=10335");
    }

    @Test
    public void toBsRequest_TextAdGroup_UrlIsCorrect() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        String url = bsRequest.getUrlBuilder("http://bsrank.yandex.ru/rank").build();

        assertThat(url).startsWith("http://bsrank.yandex.ru/rank/24?");
        assertThat(url).contains("operation=3");
        assertThat(url).doesNotContain("main-ctr-pred-id=");
    }

    @Test
    public void toBsRequest_McBannerAdGroup_McBannerIsCorrect() {
        AdGroupForAuction adGroupForAuction = mcBannerAdGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.isMcBanner()).isEqualTo(true);
    }

    @Test
    public void toBsRequest_McBannerAdGroup_RankOptionIdIsCorrect() {
        AdGroupForAuction adGroupForAuction = mcBannerAdGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest =
                getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        String url = bsRequest.getUrlBuilder("").build();
        assertThat(url).contains("rank-option-id=13");
    }

    @Test
    public void toBsRequest_McBannerAdGroup_UrlIsCorrect() {
        AdGroupForAuction adGroupForAuction = mcBannerAdGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        String url = bsRequest.getUrlBuilder("").build();
        assertThat(url).contains("operation=3");
    }

    @Test
    public void trafaretForDeviceTargeting() {
        AdGroupForAuction adGroupForAuction = adGroupForAuctionWithDeviceTargeting(EnumSet.of(
                CampaignDeviceTargeting.IPHONE, CampaignDeviceTargeting.IPAD
        ));
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        String url = bsRequest.getUrlBuilder("").build();
        assertThat(url).contains("operation=3");

        assertThat(url).contains(
                BsRequest.getParamNameRankOptionId() + "=" + BsRequest
                        .getTrafaretMobileAndDeviceTargetingRankOptionIdValue()
        );
        assertThat(url).contains(BsRequest.getParamNameTargetDeviceGroup() + "=1%0A2");
        assertThat(url).contains(BsRequest.getParamNameTargetDetailedDeviceType() + "=3");
    }

    @Test
    public void trafaretForDeviceTargetingAllDevices() {
        AdGroupForAuction adGroupForAuction = adGroupForAuctionWithDeviceTargeting(EnumSet.of(
                CampaignDeviceTargeting.OTHER_DEVICES,
                CampaignDeviceTargeting.IPHONE, CampaignDeviceTargeting.IPAD
        ));
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        String url = bsRequest.getUrlBuilder("").build();
        assertThat(url).doesNotContain(BsRequest.getParamNameRankOptionId());
        assertThat(url).doesNotContain(BsRequest.getParamNameTargetDeviceGroup());
        assertThat(url).doesNotContain(BsRequest.getParamNameTargetDetailedDeviceType());
    }

    @Test
    public void onlyMobilePagesOff() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.isOnlyMobilePages()).isEqualTo(false);
    }

    @Test
    public void geo() {
        long geoId = 1L;
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        adGroupForAuction.getAdGroup().withGeo(singletonList(geoId));

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getRegionIds()).containsExactly(geoId);
    }

    @Test
    public void currency() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getCurrency()).isEqualTo(adGroupForAuction.getCurrency());
    }

    @Test
    public void domain_formBannerFilterDomain() {
        String domain = "domain";
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        adGroupForAuction.getBanner().withId(1L);
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of(adGroupForAuction.getBanner().getId(), domain)));

        assertThat(bsRequest.getDomain()).isEqualTo(domain);
    }

    @Test
    public void domain_formBannerDomain() {
        String domain = "domain";
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        adGroupForAuction.getBanner().withId(1L);
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword,
                Map.of(adGroupForAuction.getBanner().getId(), domain)));

        assertThat(bsRequest.getDomain()).isEqualTo(domain);
    }

    @Test
    public void domain_null() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getDomain()).isEqualTo(null);
    }

    @Test
    public void domain_fromPublisherDomain_whenMobileContent() {
        AdGroupForAuction adGroupForAuction = mobileAdGroupForAuction();
        when(adGroupForAuction.getPublisherDomain()).thenReturn(new Domain().withId(1L).withDomain("kinopoisk.ru"));
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getDomain()).isEqualTo("kinopoisk.ru");
    }

    @Test
    public void domain_fromBundleId_whenMobileContentAndPublisherDomainIsNull() {
        AdGroupForAuction adGroupForAuction = mobileAdGroupForAuction();
        when(adGroupForAuction.getPublisherDomain()).thenReturn(null);
        when(adGroupForAuction.getStoreAppId()).thenReturn("ru.yandex.direct");
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getDomain()).isEqualTo("ru.yandex.direct");
    }

    @Test
    public void domain_fromPublisherDomain_whenNotMobileContent() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        when(adGroupForAuction.getPublisherDomain()).thenReturn(new Domain().withId(1L).withDomain("kinopoisk.ru"));
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getDomain()).isEqualTo(null);
    }

    @Test
    public void bannerHead() {
        String title = "title";
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        ((TextBanner) adGroupForAuction.getBanner()).withTitle(title);

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getBannerHead()).isEqualTo(title);
    }

    @Test
    public void bannerBody() {
        String body = "body";
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        ((TextBanner) adGroupForAuction.getBanner()).withBody(body);
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getBannerBody()).isEqualTo(body);
    }

    @Test
    public void bidCalcMethod_forMobileGroup() {
        AdGroupForAuction adGroupForAuction = mobileAdGroupForAuction();

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getBidCalcMethod()).isEqualTo(GROUP_BID);
    }

    @Test
    public void bidCalcMethod_forIsBannerFormat() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getBidCalcMethod()).isEqualTo(GROUP_BID);
    }

    @Test
    public void bidCalcMethod_null() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();
        when(adGroupForAuction.isBannerFormat()).thenReturn(true);

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.getBidCalcMethod()).isEqualTo(null);
    }

    @Test
    public void withOnlyMobilePagesOn() {
        AdGroupForAuction adGroupForAuction = mobileAdGroupForAuction();

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.isOnlyMobilePages()).isEqualTo(true);
    }

    @Test
    public void withOnlyMobilePagesOff() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();

        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));

        assertThat(bsRequest.isOnlyMobilePages()).isEqualTo(false);
    }

    /**
     * Если фраза новая, и CTR для фразы неизвестны, применяются CTR по умолчанию:
     * * 3% для гарантии
     * * 20% для спецразмещения
     */
    @Test
    public void keywordWithNullCtr_overrideCtrWithDefaults() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();

        Keyword kw = adGroupForAuction.getKeywords().get(0);
        kw.withShowsForecast(100L);
        forecastCtrsByKeyword.put(kw, new ForecastCtr().withGuaranteeCtr(null).withPremiumCtr(null));
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));
        String qs = bsRequest.getPhrases().get(0).getQueryStringValue(1, 2L);

        assertThat(qs).contains("s:100,3,100,20");
    }

    /**
     * Если фраза новая, и CTR для фразы нулевые, применяются CTR по умолчанию:
     * * 3% для гарантии
     * * 20% для спецразмещения
     */
    @Test
    public void keywordWithZeroCtr_overrideCtrWithDefaults() {
        AdGroupForAuction adGroupForAuction = adGroupForAuction();

        Keyword kw = adGroupForAuction.getKeywords().get(0);
        kw.withShowsForecast(100L);
        forecastCtrsByKeyword.put(kw, new ForecastCtr().withGuaranteeCtr(0.0).withPremiumCtr(0.0));
        BsRequest<BsRequestPhraseWrapper> bsRequest
                = getFirstRequest(bsAuctionService.toBsRequest(adGroupForAuction, forecastCtrsByKeyword, Map.of()));
        String qs = bsRequest.getPhrases().get(0).getQueryStringValue(1, 2L);

        assertThat(qs).contains("s:100,3,100,20");
    }

    private BsRequest<BsRequestPhraseWrapper> getFirstRequest(
            List<BsRequest<BsRequestPhraseWrapper>> bsRequests) {
        return StreamEx.of(bsRequests)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Request contains no elements"));
    }

    private AdGroupForAuction adGroupForAuction() {
        Campaign campaign = new Campaign()
                .withOrderId(0L);
        campaign.withOpts(EnumSet.noneOf(CampaignOpts.class));
        campaign.withDeviceTargeting(EnumSet.noneOf(CampaignDeviceTargeting.class));
        AdGroup adGroup = new TextAdGroup().withType(AdGroupType.BASE);
        TextBanner mainBanner = new TextBanner()
                .withId(1L);
        Keyword keyword = new Keyword();
        List<Keyword> phrases = singletonList(keyword);
        Currency currency = CurrencyRub.getInstance();
        int bannerQuantity = 10;

        AdGroupForAuction adGroupForAuction = mock(AdGroupForAuction.class);
        when(adGroupForAuction.getCampaign()).thenReturn(campaign);
        when(adGroupForAuction.getBanner()).thenReturn(mainBanner);
        when(adGroupForAuction.getAdGroup()).thenReturn(adGroup);
        when(adGroupForAuction.getBannerQuantity()).thenReturn(bannerQuantity);
        when(adGroupForAuction.getCurrency()).thenReturn(currency);
        when(adGroupForAuction.getKeywords()).thenReturn(phrases);
        return adGroupForAuction;
    }

    private AdGroupForAuction mobileAdGroupForAuction() {
        Campaign campaign = new Campaign()
                .withOrderId(0L);
        campaign.withOpts(EnumSet.noneOf(CampaignOpts.class));
        MobileContentAdGroup adGroup = new MobileContentAdGroup().withType(AdGroupType.MOBILE_CONTENT);
        adGroup.setDeviceTypeTargeting(EnumSet.of(PHONE));
        MobileContent mobileContent = mock(MobileContent.class);
        when(mobileContent.getOsType()).thenReturn(OsType.ANDROID);
        adGroup.setMobileContent(mobileContent);
        TextBanner mainBanner = new TextBanner()
                .withId(1L);
        Keyword keyword = new Keyword();
        List<Keyword> phrases = singletonList(keyword);
        Currency currency = CurrencyRub.getInstance();
        int bannerQuantity = 10;

        AdGroupForAuction adGroupForAuction = mock(AdGroupForAuction.class);
        when(adGroupForAuction.getCampaign()).thenReturn(campaign);
        when(adGroupForAuction.getBanner()).thenReturn(mainBanner);
        when(adGroupForAuction.getAdGroup()).thenReturn(adGroup);
        when(adGroupForAuction.getBannerQuantity()).thenReturn(bannerQuantity);
        when(adGroupForAuction.getCurrency()).thenReturn(currency);
        when(adGroupForAuction.getKeywords()).thenReturn(phrases);
        return adGroupForAuction;
    }

    private AdGroupForAuction contentPromotionVideoAdGroupForAuction() {
        Campaign campaign = new Campaign()
                .withOrderId(0L);
        campaign.withOpts(EnumSet.noneOf(CampaignOpts.class));
        campaign.withDeviceTargeting(EnumSet.noneOf(CampaignDeviceTargeting.class));
        AdGroup adGroup =
                new ContentPromotionAdGroup().withType(AdGroupType.CONTENT_PROMOTION).withContentPromotionType(VIDEO);
        ContentPromotionBanner mainBanner = new ContentPromotionBanner()
                .withId(1L)
                .withTitle("заголовок")
                .withBody("описание");
        Keyword keyword = new Keyword().withPhrase("ключевая фраза");
        List<Keyword> phrases = singletonList(keyword);
        Currency currency = CurrencyRub.getInstance();
        int bannerQuantity = 10;

        AdGroupForAuction adGroupForAuction = mock(AdGroupForAuction.class);
        when(adGroupForAuction.getCampaign()).thenReturn(campaign);
        when(adGroupForAuction.getBanner()).thenReturn(mainBanner);
        when(adGroupForAuction.getAdGroup()).thenReturn(adGroup);
        when(adGroupForAuction.getBannerQuantity()).thenReturn(bannerQuantity);
        when(adGroupForAuction.getCurrency()).thenReturn(currency);
        when(adGroupForAuction.getKeywords()).thenReturn(phrases);
        return adGroupForAuction;
    }

    private AdGroupForAuction adGroupForAuctionWithDeviceTargeting(EnumSet<CampaignDeviceTargeting> deviceTargeting) {
        Campaign campaign = new Campaign()
                .withOrderId(0L);
        campaign.withOpts(EnumSet.noneOf(CampaignOpts.class));
        campaign.withDeviceTargeting(deviceTargeting);
        AdGroup adGroup = new TextAdGroup().withType(AdGroupType.BASE);
        TextBanner mainBanner = new TextBanner()
                .withId(1L);
        Keyword keyword = new Keyword();
        List<Keyword> phrases = singletonList(keyword);
        Currency currency = CurrencyRub.getInstance();
        int bannerQuantity = 10;

        AdGroupForAuction adGroupForAuction = mock(AdGroupForAuction.class);
        when(adGroupForAuction.getCampaign()).thenReturn(campaign);
        when(adGroupForAuction.getBanner()).thenReturn(mainBanner);
        when(adGroupForAuction.getAdGroup()).thenReturn(adGroup);
        when(adGroupForAuction.getBannerQuantity()).thenReturn(bannerQuantity);
        when(adGroupForAuction.getCurrency()).thenReturn(currency);
        when(adGroupForAuction.getKeywords()).thenReturn(phrases);
        return adGroupForAuction;
    }

    private AdGroupForAuction mcBannerAdGroupForAuction() {
        Campaign campaign = new Campaign()
                .withOrderId(0L);
        campaign.withOpts(EnumSet.noneOf(CampaignOpts.class));
        campaign.withDeviceTargeting(EnumSet.noneOf(CampaignDeviceTargeting.class));
        AdGroup adGroup = new McBannerAdGroup().withType(AdGroupType.MCBANNER);
        McBanner mainBanner = new McBanner()
                .withId(1L);
        Keyword keyword = new Keyword().withPhrase("new phrase");
        List<Keyword> phrases = singletonList(keyword);
        Currency currency = CurrencyRub.getInstance();
        int bannerQuantity = 10;

        AdGroupForAuction adGroupForAuction = mock(AdGroupForAuction.class);
        when(adGroupForAuction.getCampaign()).thenReturn(campaign);
        when(adGroupForAuction.getBanner()).thenReturn(mainBanner);
        when(adGroupForAuction.getAdGroup()).thenReturn(adGroup);
        when(adGroupForAuction.getBannerQuantity()).thenReturn(bannerQuantity);
        when(adGroupForAuction.getCurrency()).thenReturn(currency);
        when(adGroupForAuction.getKeywords()).thenReturn(phrases);
        return adGroupForAuction;
    }

}
