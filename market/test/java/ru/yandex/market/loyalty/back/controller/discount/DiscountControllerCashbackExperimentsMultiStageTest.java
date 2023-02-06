package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerCashbackExperimentsMultiStageTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;

    @Test
    public void shouldNotStoreMultistageStateForPromoWithExperimentsCuttingRule() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.EXPERIMENTS_CUTTING_RULE,
                                RuleParameterName.EXPERIMENTS,
                                "test=1")
                        .setPriority(1)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "test=1");
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MASTERCARD)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                        .build(),
                experiments
        );

        assertThat(discountResponse.getCashback().getEmit().getAmount(), not(comparesEqualTo(BigDecimal.ZERO)));
        assertThat(orderCashbackCalculationDao.findAll(), empty());
    }
}
