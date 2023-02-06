package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.loyalty.model.PromocodeDiscountEntry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class PromocodeActualizeTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";
    private static final String PROMO_KEY = "some promo key";
    private static final long PUID = 2_190_550_858_753_437_200L;

    @Test
    void shouldApplyPromocode() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        var item = params.getItems().iterator().next();
        params.setUid(PUID);

        params.getLoyaltyParameters()
                .expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                        .discount(Map.of(item.getOfferItemKey(), BigDecimal.ONE)));

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart, notNullValue());
        assertThat(cart.getCarts().get(0).getPromos(), not(empty()));
        assertThat(cart.getCarts().get(0).getPromos(), hasItem(allOf(
                hasProperty("promoDefinition", hasProperty("type",
                        is(PromoType.MARKET_PROMOCODE)))
        )));
    }
}
