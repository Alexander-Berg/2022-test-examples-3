package ru.yandex.market.checkout.checkouter.promo.smartshopping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.loyalty.model.CoinDiscountEntry;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class CoinCheckoutControllerTest extends AbstractWebTestBase {

    private static final String PROMO_KEY = "some promo key";
    private static final String ANAPLAN_ID = "some anaplan id";

    private Parameters parameters;
    private OrderItem orderItem;

    @BeforeEach
    void configure() {
        parameters = defaultBlueOrderParameters();
        parameters.configuration().cart().multiCartMocks().setMockLoyalty(false);
        orderItem = parameters.getItems().iterator().next();
        orderItem.setPrice(BigDecimal.valueOf(1000));
        orderItem.setBuyerPrice(BigDecimal.valueOf(1000));
        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(orderItem).build()));
    }

    @Test
    void shouldReturnOrderWithAnaplanId() {
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(1, PROMO_KEY)
                .discount(Map.of(orderItem.getOfferItemKey(), BigDecimal.valueOf(100)))
                .anaplanId(ANAPLAN_ID)
        );

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getItems(), hasItem(allOf(
                hasProperty("offerId", is(orderItem.getOfferId())),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(900))),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoDefinition", allOf(
                                hasProperty("type", is(PromoType.MARKET_COIN)),
                                hasProperty("marketPromoId", is(PROMO_KEY)),
                                hasProperty("anaplanId", is(ANAPLAN_ID))
                        ))
                )))
        )));

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, 0L);

        assertThat(order.getItems(), hasItem(allOf(
                hasProperty("offerId", is(orderItem.getOfferId())),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(900))),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoDefinition", allOf(
                                hasProperty("type", is(PromoType.MARKET_COIN)),
                                hasProperty("marketPromoId", is(PROMO_KEY)),
                                hasProperty("anaplanId", is(ANAPLAN_ID))
                        ))
                )))
        )));
    }

}
