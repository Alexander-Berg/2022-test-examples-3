package ru.yandex.direct.intapi.entity.auction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import one.util.streamex.LongStreamEx;
import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.bids.interpolator.CapFactory;
import ru.yandex.direct.core.entity.bids.interpolator.InterpolatorService;
import ru.yandex.direct.core.entity.currency.model.CurrencyRate;
import ru.yandex.direct.core.entity.currency.repository.CurrencyRateRepository;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.auction.model.AuctionItem;
import ru.yandex.direct.intapi.entity.auction.model.InterpolateRequest;
import ru.yandex.direct.intapi.entity.auction.model.PhraseAuctionItem;
import ru.yandex.direct.intapi.entity.auction.service.InterpolateAuctionItemsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@IntApiTest
public class InterpolateAuctionItemsServiceTest {
    public static final int MAX_AUCTION_ITEMS = 1;
    public static final int MAX_PHRASE_AUCTION_SIZE = 1;

    private InterpolateAuctionItemsService interpolateAuctionItemsService;

    public InterpolateAuctionItemsServiceTest() {
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
        InterpolatorService interpolatorService = new InterpolatorService(currencyRateService, capFactory);
        interpolateAuctionItemsService = new InterpolateAuctionItemsService(interpolatorService);
    }

    @Test
    public void interpolate() {
        InterpolateRequest interpolateRequest = new InterpolateRequest();
        interpolateRequest.withPhraseBidsItemList(generatePhraseAuctionItem());
        interpolateAuctionItemsService.interpolate(interpolateRequest);
    }

    private List<PhraseAuctionItem> generatePhraseAuctionItem() {
        return StreamEx.generate(() -> new PhraseAuctionItem()
                .withDomain("testDomain")
                .withExactMatch(true)
                .withCurrencyCode(CurrencyCode.USD)
                .withAuctionItems(generateAuctionItem()))
                .limit(MAX_PHRASE_AUCTION_SIZE)
                .toList();
    }

    private List<AuctionItem> generateAuctionItem() {
        List<AuctionItem> result = new ArrayList<>(MAX_AUCTION_ITEMS);
        List<Long> bids = LongStreamEx.range(500, 9_000).boxed().map(x -> x * 100 + 17).toList();
        List<Long> trafficVolumes =
                LongStreamEx.range(500, 9_000).boxed().map(x -> x * 100 + 17).toList();
        List<Long> amnestyPrices =
                LongStreamEx.range(500, 9_000).boxed().map(x -> x * 100 + 17).toList();
        for (int i = 0; i < trafficVolumes.size(); i++) {
            result.add(new AuctionItem().withAmnestyPrice(amnestyPrices.get(i)).withBid(bids.get(i))
                    .withTrafficVolume(trafficVolumes.get(i)));
        }
        return result;
    }
}
