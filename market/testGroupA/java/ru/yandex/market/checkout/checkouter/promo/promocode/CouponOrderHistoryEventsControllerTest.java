package ru.yandex.market.checkout.checkouter.promo.promocode;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class CouponOrderHistoryEventsControllerTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";
    private static final String PROMO_KEY = "some promo key";
    private Parameters parameters;
    private OrderItem orderItem;

    @BeforeEach
    void configure() {
        parameters = defaultBlueOrderParameters();
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(PROMO_CODE));
        parameters.configuration().cart().multiCartMocks().setMockLoyalty(true);
        orderItem = parameters.getItems().iterator().next();
        orderItem.setPrice(BigDecimal.valueOf(1000));
        orderItem.setBuyerPrice(BigDecimal.valueOf(1000));
        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(orderItem).build()));
        parameters.getLoyaltyParameters().setExpectedPromoCode(PROMO_CODE);
        parameters.getLoyaltyParameters().addLoyaltyDiscount(
                orderItem,
                LoyaltyDiscount.builder()
                        .promoType(PromoType.MARKET_COUPON)
                        .promoKey(PROMO_KEY)
                        .discount(BigDecimal.valueOf(100))
                        .build()
        );
    }

    @Test
    public void shouldReturnPromoIdInEvents() {
        Order order = orderCreateHelper.createOrder(parameters);

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null,
                0, 10
        );

        assertThat(events.getItems(), everyItem(hasProperty("orderAfter", allOf(
                hasProperty("id", equalTo(order.getId())),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("offerId", is(orderItem.getOfferId())),
                                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(900))),
                                hasProperty("promos", hasItem(allOf(
                                        hasProperty("promoDefinition", allOf(
                                                hasProperty("type", is(PromoType.MARKET_COUPON)),
                                                hasProperty("marketPromoId", is(PROMO_KEY))
                                        )),
                                        hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(100)))
                                )))
                        )
                ))
        ))));
    }

}
