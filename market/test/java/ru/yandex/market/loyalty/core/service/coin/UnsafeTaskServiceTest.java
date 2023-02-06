package ru.yandex.market.loyalty.core.service.coin;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.unsafe.UnsafeTaskLogDao;
import ru.yandex.market.loyalty.core.dao.unsafe.UnsafeTaskRecord;
import ru.yandex.market.loyalty.core.exception.CoinNotBoundException;
import ru.yandex.market.loyalty.core.exception.CoinsNotFoundException;
import ru.yandex.market.loyalty.core.exception.IncorrectCoinStatusException;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.core.dao.unsafe.UnsafeTaskRecordType.Constants.REBIND_COIN;
import static ru.yandex.market.loyalty.core.dao.unsafe.UnsafeTaskRecordType.Constants.RESET_COIN_BINDING;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class UnsafeTaskServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private UnsafeTaskService unsafeTaskService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private UnsafeTaskLogDao unsafeTaskLogDao;
    @Autowired
    private CoinService coinService;

    @Test(expected = CoinNotBoundException.class)
    public void shouldWorkCorrectlyIfCoinNotBound() throws IncorrectCoinStatusException, CoinsNotFoundException,
            CoinNotBoundException {
        SmartShoppingPromoBuilder coinPromoBuilder = PromoUtils.SmartShopping.defaultFixed();
        Promo promo = promoManager.createSmartShoppingPromo(coinPromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth("123").build());
        unsafeTaskService.resetBind(coinKey, "MARKETDISCOUNT-10000", "admin");
    }

    @Test
    public void shouldCreateLogRecordWhenBindingReset() throws IncorrectCoinStatusException, CoinsNotFoundException,
            CoinNotBoundException {
        SmartShoppingPromoBuilder coinPromoBuilder = PromoUtils.SmartShopping.defaultFixed();
        Promo promo = promoManager.createSmartShoppingPromo(coinPromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth("123").build());
        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, "123");
        unsafeTaskService.resetBind(coinKey, "MARKETDISCOUNT-10000", "admin");
        List<UnsafeTaskRecord<?>> records = unsafeTaskLogDao.getUnsafeTaskLogRecords();
        assertThat(records, hasSize(1));
        assertThat(records, contains(
                hasProperty("type", equalTo(RESET_COIN_BINDING))
        ));
    }

    @Test
    public void shouldCreateLogRecordWhenCoinRebound() throws IncorrectCoinStatusException, CoinsNotFoundException,
            CoinNotBoundException {
        SmartShoppingPromoBuilder coinPromoBuilder = PromoUtils.SmartShopping.defaultFixed();
        Promo promo = promoManager.createSmartShoppingPromo(coinPromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth("123").build());
        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, "123");
        unsafeTaskService.rebind(coinKey, ANOTHER_UID, "MARKETDISCOUNT-10000", "admin");
        List<UnsafeTaskRecord<?>> records = unsafeTaskLogDao.getUnsafeTaskLogRecords();
        assertThat(records, hasSize(1));
        assertThat(records, contains(
                hasProperty("type", equalTo(REBIND_COIN))
        ));
    }
}
