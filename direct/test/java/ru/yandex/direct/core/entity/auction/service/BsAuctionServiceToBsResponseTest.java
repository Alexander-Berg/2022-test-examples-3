package ru.yandex.direct.core.entity.auction.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.LongStreamEx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import ru.yandex.direct.bsauction.BsCpcPrice;
import ru.yandex.direct.bsauction.PositionalBsTrafaretResponsePhrase;
import ru.yandex.direct.core.entity.auction.container.BsRequestPhraseWrapper;
import ru.yandex.direct.core.entity.autobroker.model.AutoBrokerResult;
import ru.yandex.direct.core.entity.autobroker.service.AutoBrokerCalculator;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.WalletRestMoney;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.function.Function.identity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ParametersAreNonnullByDefault
public class BsAuctionServiceToBsResponseTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BsRequestPhraseWrapper phraseWrapper;

    @Mock
    private PositionalBsTrafaretResponsePhrase responsePhrase;

    @Mock
    private AutoBrokerCalculator autoBrokerCalculator;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private BsAuctionService bsAuctionService;

    private long campaignId = 5L;
    private long walletId = 500L;
    private CurrencyCode currency = CurrencyCode.EUR;

    @Before
    public void init() {
        initMocks(this);

        Campaign campaign = new Campaign()
                .withId(campaignId)
                .withWalletId(walletId)
                .withCurrency(currency);

        when(phraseWrapper.getAdGroupForAuction().getCampaign()).thenReturn(campaign);
        when(phraseWrapper.getAdGroupForAuction().getAdGroup().getBsRarelyLoaded()).thenReturn(false);

        BsCpcPrice[] cpcPrices = generateCpcPrices();
        when(responsePhrase.getPremium()).thenReturn(cpcPrices);
        when(responsePhrase.getGuarantee()).thenReturn(cpcPrices);

        AutoBrokerResult autoBrokerResult = new AutoBrokerResult().withBrokerPrice(Money.valueOf("3", currency));
        when(autoBrokerCalculator.calculatePrices(any(), any(), any(), any())).thenReturn(autoBrokerResult);
    }

    /**
     * Проверка: при конвертации ответа от БК данные об остатках на кошельках берутся не по id кошелька,
     * а по id кампании.
     * <p>
     * Тест написан после исправления дефекта DIRECT-76077.
     */
    @Test
    public void convertBsResponse_TakesWalletByCampaignId() {
        WalletRestMoney correctWallet = new WalletRestMoney()
                .withWalletId(walletId)
                .withRest(Money.valueOf(BigDecimal.ZERO, currency));
        WalletRestMoney anotherWallet = new WalletRestMoney()
                .withWalletId(666L)
                .withRest(Money.valueOf("30", currency));
        Map<Long, WalletRestMoney> wallets = new HashMap<Long, WalletRestMoney>() {{
            put(campaignId, correctWallet);
            put(walletId, anotherWallet);   // <- не должен быть использован
        }};
        wallets = spy(wallets);

        bsAuctionService.convertBsResponse(phraseWrapper, responsePhrase, autoBrokerCalculator, wallets);

        verify(wallets, only()).get(eq(campaignId));
        verify(wallets, never()).get(eq(walletId));
    }

    private BsCpcPrice[] generateCpcPrices() {
        return LongStreamEx.range(3, 10)
                .mapToObj(i -> Money.valueOf(i, currency))
                .mapToEntry(identity())
                .mapKeyValue(BsCpcPrice::new)
                .toArray(BsCpcPrice[]::new);
    }

}
