package ru.yandex.market.loyalty.core.dao;

import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;

public class OrderCashbackCalculationDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;

    @Test
    public void shouldSaveEntry() {
        OrderCashbackCalculation save = OrderCashbackCalculation.builder()
                .setOrderId(DEFAULT_ORDER_ID)
                .setResult(ResolvingState.FINAL)
                .setInitialResult(ResolvingState.FINAL)
                .setUid(112L)
                .setRuleBeanName("test rule")
                .build();
        OrderCashbackCalculation saveRes = orderCashbackCalculationDao.save(save);
        Optional<OrderCashbackCalculation> byId = orderCashbackCalculationDao.findById(saveRes.getId());
        assertThat(byId.isPresent(), equalTo(true));
        assertThat(byId.get().getId(), notNullValue());
    }
}
