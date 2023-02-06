package ru.yandex.market.api.internal.loyalty.parser;

import org.junit.Test;
import ru.yandex.market.api.domain.v2.loyalty.*;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by fettsery on 04.09.18.
 */
public class CoinsForCartParserTest {
    @Test
    public void shouldParseCoinsForCart() {
        CoinsForCart result = new CoinsForCartParser().parse(ResourceHelpers.getResource("get-coins-for-cart.json"));

        assertEquals(3, result.getApplicableCoins().size());

        UserCoinResponse coinResponse = result.getApplicableCoins().get(0);
        assertEquals(10060L, coinResponse.getId().longValue());
        assertEquals("Скидка на 100 рублей", coinResponse.getTitle());
        assertEquals(UserCoinResponse.CoinType.FIXED, coinResponse.getCoinType());
        assertEquals(2, coinResponse.getCoinRestrictions().size());
        assertEquals("_Xsny49ILVQa3WY9QhoLXQ", coinResponse.getPromoKey());

        CoinRestriction coinRestriction = coinResponse.getCoinRestrictions().get(0);
        assertEquals(1L, coinRestriction.getCategoryId().longValue());

        coinRestriction = coinResponse.getCoinRestrictions().get(1);
        assertEquals(2L, coinRestriction.getCategoryId().longValue());

        coinResponse = result.getApplicableCoins().get(1);
        assertEquals(10061L, coinResponse.getId().longValue());
        assertEquals(UserCoinResponse.CoinType.PERCENT, coinResponse.getCoinType());

        coinResponse = result.getApplicableCoins().get(2);
        assertEquals(10066L, coinResponse.getId().longValue());
        assertEquals(UserCoinResponse.CoinType.FREE_DELIVERY, coinResponse.getCoinType());


        assertEquals(8, result.getDisabledCoins().size());

        List<UserCoinResponse> lDIDisabledCoins = result.getDisabledCoins().stream()
            .filter(c -> c.getReason().equals(CoinApplicationRestrictionType.LDI_RESTRICTION))
            .map(DisabledCoin::getCoin)
            .collect(Collectors.toList());
        assertEquals(2, lDIDisabledCoins.size());
        assertEquals(100L, lDIDisabledCoins.get(0).getId().longValue());
        assertEquals(101L, lDIDisabledCoins.get(1).getId().longValue());

        List<UserCoinResponse> unknownDisabledCoins = result.getDisabledCoins().stream()
            .filter(c -> c.getReason().equals(CoinApplicationRestrictionType.UNKNOWN))
            .map(DisabledCoin::getCoin)
            .collect(Collectors.toList());
        assertEquals(2, unknownDisabledCoins.size());
        assertEquals(103L, unknownDisabledCoins.get(0).getId().longValue());
        assertEquals(102L, unknownDisabledCoins.get(1).getId().longValue());

        UserCoinResponse dropshipDisabledCoin = result.getDisabledCoins().stream()
                .filter(c -> c.getReason().equals(CoinApplicationRestrictionType.DROPSHIP_RESTRICTION))
                .map(DisabledCoin::getCoin)
                .findFirst()
                .orElse(null);
        assertNotNull(dropshipDisabledCoin);
        assertEquals(105L, dropshipDisabledCoin.getId().longValue());

        UserCoinResponse deviceDisabledCoin = result.getDisabledCoins().stream()
                .filter(c -> c.getReason().equals(CoinApplicationRestrictionType.DEVICE_RESTRICTION))
                .map(DisabledCoin::getCoin)
                .findFirst()
                .orElse(null);
        assertNotNull(deviceDisabledCoin);
        assertEquals(106L, deviceDisabledCoin.getId().longValue());

        UserCoinResponse filterRuleDisabledCoin = result.getDisabledCoins().stream()
                .filter(c -> c.getReason().equals(CoinApplicationRestrictionType.FILTER_RULE_RESTRICTION))
                .map(DisabledCoin::getCoin)
                .findFirst()
                .orElse(null);
        assertNotNull(filterRuleDisabledCoin);
        assertEquals(107L, filterRuleDisabledCoin.getId().longValue());


        UserCoinResponse singleDeliveryDisabledCoin = result.getDisabledCoins().stream()
                .filter(c -> c.getReason().equals(CoinApplicationRestrictionType.SINGLE_FREE_DELIVERY_RESTRICTION))
                .map(DisabledCoin::getCoin)
                .findFirst()
                .orElse(null);
        assertNotNull(singleDeliveryDisabledCoin);
        assertEquals(108L, singleDeliveryDisabledCoin.getId().longValue());
    }
}
