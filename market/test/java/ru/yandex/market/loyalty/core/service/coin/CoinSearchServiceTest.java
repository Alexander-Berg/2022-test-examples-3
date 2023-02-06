package ru.yandex.market.loyalty.core.service.coin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;

public class CoinSearchServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinCreationService coinCreationService;
    @Autowired
    private CoinSearchService coinSearchService;
    @Autowired
    private PromoService promoService;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private UnsafeTaskService unsafeTaskService;

    @Test
    public void shouldFixMarketdiscount8668() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed()
                .setExpiration(ExpirationPolicy.toEndOfPromo()));
        promoManager.updateCoinPromo(defaultFixed().setId(promo.getId())
                .setExpiration(ExpirationPolicy.expireByDays(30)));
        unsafeTaskService.resetCoinProperties(promo.getId(), "", "");

        coinSearchService.invalidateCaches();

        Set<Long> allCoinProps = coinDao.getAllCoinProps(promo.getId()).stream()
                .map(cp -> cp.getId())
                .collect(Collectors.toSet());

        Map<Long, CoinProps> cachedCoinProps = coinSearchService.getCachedCoinProps(allCoinProps);
        List<CoinProps> coinProps = new ArrayList<>(cachedCoinProps.values());

        assertThat(coinProps, hasSize(2));
        assertEquals(coinProps.get(0).getExpirationPolicy().getType(), ExpirationPolicy.Type.EXPIRE_BY_DAY);
        assertEquals(coinProps.get(1).getExpirationPolicy().getType(), ExpirationPolicy.Type.EXPIRE_BY_DAY);
    }
}
