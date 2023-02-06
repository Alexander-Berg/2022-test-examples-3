package ru.yandex.market.api.internal.loyalty.parser;

import org.junit.Test;
import ru.yandex.market.api.domain.v2.loyalty.CoinRestriction;
import ru.yandex.market.api.domain.v2.loyalty.CoinsForOrder;
import ru.yandex.market.api.domain.v2.loyalty.UserCoinResponse;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;

/**
 * Created by fettsery on 19.09.18.
 */
public class CoinsForOrderParserTest {
    @Test
    public void shouldParseCoinsForOrder() {
        CoinsForOrder result = new CoinsForOrderParser().parse(ResourceHelpers.getResource("get-coins-for-order.json"));

        assertEquals(3, result.getNewCoins().size());

        UserCoinResponse coinResponse = result.getNewCoins().get(0);
        assertEquals(10060L, coinResponse.getId().longValue());
        assertEquals("Скидка на 100 рублей", coinResponse.getTitle());
        assertEquals(UserCoinResponse.CoinType.FIXED, coinResponse.getCoinType());
        assertEquals(2, coinResponse.getCoinRestrictions().size());
        assertEquals("_Xsny49ILVQa3WY9QhoLXQ", coinResponse.getPromoKey());

        CoinRestriction coinRestriction = coinResponse.getCoinRestrictions().get(0);
        assertEquals(1L, coinRestriction.getCategoryId().longValue());

        coinRestriction = coinResponse.getCoinRestrictions().get(1);
        assertEquals(2L, coinRestriction.getCategoryId().longValue());

        coinResponse = result.getNewCoins().get(1);
        assertEquals(10061L, coinResponse.getId().longValue());
        assertEquals(UserCoinResponse.CoinType.PERCENT, coinResponse.getCoinType());

        coinResponse = result.getNewCoins().get(2);
        assertEquals(10066L, coinResponse.getId().longValue());
        assertEquals(UserCoinResponse.CoinType.FREE_DELIVERY, coinResponse.getCoinType());
    }
}
