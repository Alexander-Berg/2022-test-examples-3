package ru.yandex.market.loyalty.core.service.coin;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.coin.PersonTopSortType;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.MskuFilterRule;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_MSKU;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class TopCoinsServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TopCoinsService topCoinsService;
    @Autowired
    private CoinService coinService;

    @Test
    public void testTopCoins() {
        final RuleContainer<MskuFilterRule> ruleRuleContainer = RuleContainer.builder(MSKU_FILTER_RULE)
                .withParams(MSKU_ID, Collections.singleton(DEFAULT_MSKU))
                .build();

        RulesContainer rulesContainer = new RulesContainer();
        rulesContainer.add(ruleRuleContainer);

        final Promo promoHigherPriority = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setPromoKey("promoHigherPriority")
        );
        final Promo promoLowerPriority = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setPromoKey("promoLowerPriority")
                        .setRulesContainer(rulesContainer)
        );

        coinService.create
                .createCoin(promoHigherPriority, defaultAuth().build());
        coinService.create
                .createCoin(promoLowerPriority, defaultAuth().build());

        final List<Coin> topCoins = topCoinsService.getTopCoins(DEFAULT_UID, 2, PersonTopSortType.TOP);

        assertThat(topCoins, contains(
                hasProperty("promoKey", equalTo("promoHigherPriority")),
                hasProperty("promoKey", equalTo("promoLowerPriority"))
        ));
    }
}
