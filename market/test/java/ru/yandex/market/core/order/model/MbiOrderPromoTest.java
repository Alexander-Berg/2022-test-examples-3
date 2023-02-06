package ru.yandex.market.core.order.model;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit тесты для {@link MbiOrderPromo}.
 *
 * @author avetokhin 27/11/17.
 */
public class MbiOrderPromoTest {

    private static final Long ORDER_ID = 5577L;
    private static final PromoType PROMO_TYPE = PromoType.MARKET_PROMOCODE;
    private static final BigDecimal SUBSIDY = new BigDecimal(55);
    private static final BigDecimal SUBSIDY_CENT = new BigDecimal(5500);
    private static final String MARKET_PROMO_ID = "merket_promo_id";
    private static final String ANAPLAN_PROMO_ID = "anaplan_promo_id";
    private static final String SHOP_PROMO_ID = "shop_promo_id";
    private static final String PROMOCODE = "promocode";

    @Test
    public void fromCheckoutOrderPromo() {
        final Order order = new Order();
        order.setId(ORDER_ID);

        final PromoDefinition promoDefinition = PromoDefinition.builder()
                .type(PromoType.MARKET_PROMOCODE)
                .marketPromoId(MARKET_PROMO_ID)
                .shopPromoId(SHOP_PROMO_ID)
                .anaplanId(ANAPLAN_PROMO_ID)
                .promoCode(PROMOCODE)
                .build();

        final OrderPromo orderPromo = new OrderPromo(promoDefinition);
        orderPromo.setType(PROMO_TYPE);
        orderPromo.setSubsidy(SUBSIDY);

        final MbiOrderPromo mbiOrderPromo = MbiOrderPromo.fromCheckoutOrderPromo(order.getId(), orderPromo);

        assertThat(mbiOrderPromo, notNullValue());
        assertThat(mbiOrderPromo.getId(), nullValue());
        assertThat(mbiOrderPromo.getOrderId(), equalTo(ORDER_ID));
        assertThat(mbiOrderPromo.getPromoType(), equalTo(PROMO_TYPE));
        assertThat(mbiOrderPromo.getSubsidy(), equalTo(SUBSIDY_CENT));
        assertThat(mbiOrderPromo.getMarketPromoId(), equalTo(MARKET_PROMO_ID));
        assertThat(mbiOrderPromo.getAnaplanId(), equalTo(ANAPLAN_PROMO_ID));
        assertThat(mbiOrderPromo.getShopPromoId(), equalTo(SHOP_PROMO_ID));
    }

}
