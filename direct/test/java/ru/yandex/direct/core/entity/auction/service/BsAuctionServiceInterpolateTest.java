package ru.yandex.direct.core.entity.auction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.auction.BsRequestPhraseWrapperAdditionalData;
import ru.yandex.direct.core.entity.auction.container.AdGroupForAuction;
import ru.yandex.direct.core.entity.auction.container.BsRequestPhraseWrapper;
import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.auction.type.BsAuctionRequestTypeSupportFacade;
import ru.yandex.direct.core.entity.auction.type.support.ContentPromotionBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.McBannerBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.MobileContentBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.auction.type.support.TextBsAuctionRequestTypeSupport;
import ru.yandex.direct.core.entity.autobroker.service.AutoBrokerCalculatorProviderService;
import ru.yandex.direct.core.entity.banner.type.href.BannerDomainRepository;
import ru.yandex.direct.core.entity.bids.interpolator.CapFactory;
import ru.yandex.direct.core.entity.bids.interpolator.InterpolatorService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.currency.model.CurrencyRate;
import ru.yandex.direct.core.entity.currency.repository.CurrencyRateRepository;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordForecastService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class BsAuctionServiceInterpolateTest {
    private InterpolatorService interpolatorService;
    private BsAuctionService bsAuctionService;
    private BannerDomainRepository bannerDomainRepository;

    @Before
    public void setUp() {
        CurrencyRateRepository currencyRateRepository = mock(CurrencyRateRepository.class);
        when(currencyRateRepository.getCurrencyRate(eq(CurrencyCode.USD), any()))
                .thenAnswer(invocation -> {
                    LocalDate date = (LocalDate) invocation.getArguments()[1];
                    return new CurrencyRate()
                            .withCurrencyCode(CurrencyCode.USD)
                            .withDate(date)
                            .withRate(BigDecimal.valueOf(60));
                });
        CapFactory capFactory = new CapFactory();
        CurrencyRateService currencyRateService = new CurrencyRateService(currencyRateRepository);
        interpolatorService = spy(new InterpolatorService(currencyRateService, capFactory));

        doAnswer(invocation -> invocation.getArgument(1)).when(interpolatorService)
                .getInterpolatedTrafaretBidItems(any(), anyList(), any(), any());
        bsAuctionService = new BsAuctionService(mock(KeywordForecastService.class),
                mock(AutoBrokerCalculatorProviderService.class),
                mock(BsTrafaretClient.class),
                mock(BsTrafaretClient.class),
                false,
                mock(CampaignService.class),
                interpolatorService,
                new BsAuctionRequestTypeSupportFacade(asList(
                        new TextBsAuctionRequestTypeSupport(),
                        new MobileContentBsAuctionRequestTypeSupport(),
                        new ContentPromotionBsAuctionRequestTypeSupport(),
                        new McBannerBsAuctionRequestTypeSupport())),
                bannerDomainRepository);
    }

    @Test
    public void interpolate_minBidForTheSameTrafficVolume() {
        List<TrafaretBidItem> bidItems = new ArrayList<>();
        TrafaretBidItem item1 =
                new TrafaretBidItem().withPositionCtrCorrection(1).withBid(Money.valueOf(100, CurrencyCode.RUB))
                        .withPrice(Money.valueOf(100, CurrencyCode.RUB));
        TrafaretBidItem item2 =
                new TrafaretBidItem().withPositionCtrCorrection(1).withBid(Money.valueOf(101, CurrencyCode.RUB))
                        .withPrice(Money.valueOf(101, CurrencyCode.RUB));
        bidItems.add(item1);
        bidItems.add(item2);
        Keyword keyword = new Keyword().withPhrase("empty");

        AdGroupForAuction adGroupForAuction = AdGroupForAuction.builder()
                .campaign(mock(Campaign.class))
                .adGroup(mock(AdGroup.class))
                .keywords(asList(keyword))
                .bannerQuantity(1)
                .currency(CurrencyCode.RUB.getCurrency())
                .build();

        BsRequestPhraseWrapper wrapper = new BsRequestPhraseWrapper(mock(BsRequestPhrase.class));
        wrapper.withAdditionalData(new BsRequestPhraseWrapperAdditionalData(adGroupForAuction, keyword));

        List<TrafaretBidItem> actual = bsAuctionService.interpolate(bidItems, wrapper);
        Assertions.assertThat(actual).containsExactly(item1);
    }
}
