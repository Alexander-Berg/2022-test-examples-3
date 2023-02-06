package ru.yandex.market.checkout.checkouter.promo.multipromo;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.promo.PromoConfigurer;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;

public class DirectDiscountMultiPromoTest extends AbstractWebTestBase {

    private static final String DD_PROMO = "direct discount";
    private static final String PD_PROMO = "price drop";

    @Autowired
    private PromoConfigurer promoConfigurer;

    private Parameters parameters;
    private OrderItem firstItem;

    @BeforeEach
    public void configure() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);
        firstItem = parameters.getOrder().getItems().iterator().next();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_REPORT_DISCOUNT_VALUE, true);
    }

    @Test
    void shouldApplyWithPriceDrop() {
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));

        promoConfigurer.applyDirectDiscount(firstItem,
                DD_PROMO,
                ANAPLAN_ID,
                SHOP_PROMO_KEY,
                BigDecimal.valueOf(150), null, true, true);

        promoConfigurer.applyPriceDropDiscount(firstItem, PD_PROMO, ANAPLAN_ID, BigDecimal.valueOf(50));

        Order order = orderCreateHelper.createOrder(promoConfigurer.applyTo(parameters));

        assertThat(order, notNullValue());
        assertThat(order.getPromos(), hasItems(
                allOf(
                        hasProperty("promoDefinition", hasProperty("type",
                                is(PromoType.DIRECT_DISCOUNT))),
                        hasProperty("buyerItemsDiscount", comparesEqualTo(BigDecimal.valueOf(150)))
                ),
                allOf(
                        hasProperty("promoDefinition", hasProperty("type",
                                is(PromoType.PRICE_DROP_AS_YOU_SHOP))),
                        hasProperty("buyerItemsDiscount", comparesEqualTo(BigDecimal.valueOf(50)))
                )
        ));
        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(firstItem.getOfferId())),
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("promos", hasItems(allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.PRICE_DROP_AS_YOU_SHOP))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(50)))
                        ), allOf(
                                hasProperty("promoDefinition", hasProperty("type",
                                        is(PromoType.DIRECT_DISCOUNT))),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(150)))
                        )))
                )
        ));
    }
}
