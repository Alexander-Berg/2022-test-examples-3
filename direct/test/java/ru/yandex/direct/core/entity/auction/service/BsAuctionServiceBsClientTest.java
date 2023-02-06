package ru.yandex.direct.core.entity.auction.service;

import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.auction.container.AdGroupForAuction;
import ru.yandex.direct.core.entity.auction.type.BsAuctionRequestTypeSupportFacade;
import ru.yandex.direct.core.entity.auction.type.support.ContentPromotionBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.McBannerBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.MobileContentBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.TextBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.autobroker.service.AutoBrokerCalculatorProviderService;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.href.BannerDomainRepository;
import ru.yandex.direct.core.entity.bids.interpolator.InterpolatorService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignDeviceTargeting;
import ru.yandex.direct.core.entity.campaign.model.CampaignOpts;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordForecastService;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BsAuctionServiceBsClientTest {

    private BsTrafaretClient bsTrafaretClient;
    private BsTrafaretClient bsTrafaretClientWeb;

    @Before
    public void before() {
        bsTrafaretClient = mock(BsTrafaretClient.class);
        when(bsTrafaretClient.getAuctionResults(anyList()))
                .thenReturn(new IdentityHashMap<>());
        when(bsTrafaretClient.getAuctionResultsWithPositionCtrCorrection(anyList()))
                .thenReturn(new IdentityHashMap<>());

        bsTrafaretClientWeb = mock(BsTrafaretClient.class);
        when(bsTrafaretClientWeb.getAuctionResults(anyList()))
                .thenReturn(new IdentityHashMap<>());
        when(bsTrafaretClientWeb.getAuctionResultsWithPositionCtrCorrection(anyList()))
                .thenReturn(new IdentityHashMap<>());
    }

    @Test
    public void getBsResult_WebClient_Test() {
        BsAuctionService bsAuctionService = createBsAuctionService(true);

        bsAuctionService.getBsResults(ClientId.fromLong(1L), createAdGroupsForAuction());
        verify(bsTrafaretClient, never()).getAuctionResults(anyList());
        verify(bsTrafaretClientWeb, times(1)).getAuctionResults(anyList());

        bsAuctionService.getBsTrafaretResults(ClientId.fromLong(1L), createAdGroupsForAuction());
        verify(bsTrafaretClient, never()).getAuctionResultsWithPositionCtrCorrection(anyList());
        verify(bsTrafaretClientWeb, times(1)).getAuctionResultsWithPositionCtrCorrection(anyList());
    }

    @Test
    public void getBsResult_CommonClient_Test() {
        BsAuctionService bsAuctionService = createBsAuctionService(false);

        bsAuctionService.getBsResults(ClientId.fromLong(1L), createAdGroupsForAuction());
        verify(bsTrafaretClient, times(1)).getAuctionResults(anyList());
        verify(bsTrafaretClientWeb, never()).getAuctionResults(anyList());

        bsAuctionService.getBsTrafaretResults(ClientId.fromLong(1L), createAdGroupsForAuction());
        verify(bsTrafaretClient, times(1)).getAuctionResultsWithPositionCtrCorrection(anyList());
        verify(bsTrafaretClientWeb, never()).getAuctionResultsWithPositionCtrCorrection(anyList());
    }

    @Test
    public void getBsResult_CommonClient_WebClientNotUsed_Test() {
        BsAuctionService bsAuctionService = createBsAuctionService(false);

        bsAuctionService.getBsResults(ClientId.fromLong(1L), createAdGroupsForAuction());
        verify(bsTrafaretClient, times(1)).getAuctionResults(anyList());
        verify(bsTrafaretClientWeb, never()).getAuctionResults(anyList());

        bsAuctionService.getBsTrafaretResults(ClientId.fromLong(1L), createAdGroupsForAuction());
        verify(bsTrafaretClient, times(1)).getAuctionResultsWithPositionCtrCorrection(anyList());
        verify(bsTrafaretClientWeb, never()).getAuctionResultsWithPositionCtrCorrection(anyList());
    }

    @Test
    public void getBsResult_CommonClient_WebClientUsed_Test() {
        BsAuctionService bsAuctionService = createBsAuctionService(true);

        bsAuctionService.getBsResults(ClientId.fromLong(1L), createAdGroupsForAuction());
        verify(bsTrafaretClient, never()).getAuctionResults(anyList());
        verify(bsTrafaretClientWeb, times(1)).getAuctionResults(anyList());

        bsAuctionService.getBsTrafaretResults(ClientId.fromLong(1L), createAdGroupsForAuction());
        verify(bsTrafaretClient, never()).getAuctionResultsWithPositionCtrCorrection(anyList());
        verify(bsTrafaretClientWeb, times(1)).getAuctionResultsWithPositionCtrCorrection(anyList());
    }

    private BsAuctionService createBsAuctionService(boolean useWebClientConfig) {
        var keywordForecastService = mock(KeywordForecastService.class);
        when(keywordForecastService.getForecast(anyCollection())).thenReturn(new IdentityHashMap<>());
        var interpolatorService = mock(InterpolatorService.class);
        Answer<?> answer = invocation -> invocation.getArguments()[1];
        doAnswer(answer).when(interpolatorService).getInterpolatedTrafaretBidItems(any(), anyList(), any(), any());

        return new BsAuctionService(keywordForecastService, mock(AutoBrokerCalculatorProviderService.class),
                bsTrafaretClient, bsTrafaretClientWeb, useWebClientConfig,
                mock(CampaignService.class), interpolatorService,
                new BsAuctionRequestTypeSupportFacade(asList(
                        new TextBsAuctionRequestTypeSupport(),
                        new MobileContentBsAuctionRequestTypeSupport(),
                        new ContentPromotionBsAuctionRequestTypeSupport(),
                        new McBannerBsAuctionRequestTypeSupport())), mock(BannerDomainRepository.class));
    }

    private List<AdGroupForAuction> createAdGroupsForAuction() {
        var campaign = new Campaign()
                .withOrderId(0L);
        campaign.withOpts(EnumSet.noneOf(CampaignOpts.class));
        campaign.withDeviceTargeting(EnumSet.noneOf(CampaignDeviceTargeting.class));
        var adGroup = new TextAdGroup().withType(AdGroupType.BASE);
        var mainBanner = new TextBanner().withId(1L);
        var keywords = singletonList(new Keyword());
        var currency = CurrencyRub.getInstance();
        int bannerQuantity = 10;

        var adGroupForAuction = mock(AdGroupForAuction.class);
        when(adGroupForAuction.getCampaign()).thenReturn(campaign);
        when(adGroupForAuction.getBanner()).thenReturn(mainBanner);
        when(adGroupForAuction.getAdGroup()).thenReturn(adGroup);
        when(adGroupForAuction.getBannerQuantity()).thenReturn(bannerQuantity);
        when(adGroupForAuction.getCurrency()).thenReturn(currency);
        when(adGroupForAuction.getKeywords()).thenReturn(keywords);
        return List.of(adGroupForAuction);
    }
}
