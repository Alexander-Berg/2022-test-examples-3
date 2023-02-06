package ru.yandex.market.checkout.checkouter.promo.direct;

import java.math.BigDecimal;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.promo.PromoConfigurer;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.DIRECT_DISCOUNT;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.MARKET_BLUE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;

public class DirectDiscountPromoTest extends AbstractWebTestBase {

    private static final String DD_PROMO = "direct discount";

    @Autowired
    private PromoConfigurer promoConfigurer;
    private Parameters parameters;
    private OrderItem firstItem;

    @BeforeEach
    public void setUp() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);
        firstItem = parameters.getOrder().getItems().iterator().next();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_REPORT_DISCOUNT_VALUE, true);
    }

    @Test
    public void shouldApplyDirectDiscountPromo() {
        shouldApplyDirectDiscountPromoBase(true, false);
    }

    @Test
    public void shouldApplyDirectDiscountPromoWithMultiPromoBlock() {
        shouldApplyDirectDiscountPromoBase(false, true);
    }

    @Test
    public void shouldApplyDirectDiscountPromoWithBothBlocks() {
        shouldApplyDirectDiscountPromoBase(true, true);
    }

    private void shouldApplyDirectDiscountPromoBase(boolean fillDeprecatedPromoFields, boolean fillMultiPromoFields) {
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));

        promoConfigurer.applyDirectDiscount(firstItem,
                DD_PROMO,
                ANAPLAN_ID,
                SHOP_PROMO_KEY,
                BigDecimal.valueOf(150), null, fillDeprecatedPromoFields, fillMultiPromoFields);

        MultiCart multiCart = orderCreateHelper.cart(promoConfigurer.applyTo(parameters));
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(allOf(
                        hasProperty("buyerPrice", comparesEqualTo(firstItem.getPrice())),
                        hasProperty("promoFaults", empty()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(
                                                DIRECT_DISCOUNT
                                        )),
                                        hasProperty("anaplanId", is(
                                                ANAPLAN_ID
                                        )),
                                        hasProperty("shopPromoId", is(
                                                SHOP_PROMO_KEY
                                        ))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(150)))
                        )))
                ))
        );
    }

    @Test
    public void shouldReturnPriceChangeOnClientMistake() {
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));

        applyDirectDiscount(firstItem, BigDecimal.valueOf(150), null);

        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));

        MultiCart multiCart = orderCreateHelper.cart(promoConfigurer.applyTo(parameters));
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(allOf(
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(350))),
                        hasProperty("promoFaults", empty()),
                        hasProperty("changes", hasItem(ItemChange.PRICE)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(
                                                DIRECT_DISCOUNT
                                        )),
                                        hasProperty("anaplanId", is(
                                                ANAPLAN_ID
                                        ))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(150)))
                        )))
                ))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 450, 550})
    public void shouldApplyDirectDiscountWithOldDiscountOldMinPassed(int oldDiscountOldMin) {
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));

        applyDirectDiscount(firstItem, BigDecimal.valueOf(150), null,
                b -> b.oldDiscountOldMin(BigDecimal.valueOf(oldDiscountOldMin)));

        MultiCart multiCart = orderCreateHelper.cart(promoConfigurer.applyTo(parameters));
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(allOf(
                        hasProperty("buyerPrice", comparesEqualTo(firstItem.getPrice())),
                        hasProperty("promoFaults", empty()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(
                                                DIRECT_DISCOUNT
                                        )),
                                        hasProperty("anaplanId", is(
                                                ANAPLAN_ID
                                        ))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(150)))
                        ))),
                        hasProperty("promos", not(hasItem(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(
                                                MARKET_BLUE
                                        ))
                                ))
                        )))
                ))
        );
    }

    @Test
    public void shouldApplyDirectDiscountPromoWithSubsidy() {
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));
        firstItem.setSupplierType(SupplierType.THIRD_PARTY);

        applyDirectDiscount(firstItem, BigDecimal.valueOf(150), BigDecimal.valueOf(50));

        Order createdOrder = orderCreateHelper.createOrder(promoConfigurer.applyTo(parameters));

        assertThat(
                createdOrder.getItems(), hasItem(allOf(
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(350))),
                        hasProperty("promoFaults", empty()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(
                                                DIRECT_DISCOUNT
                                        )),
                                        hasProperty("anaplanId", is(
                                                ANAPLAN_ID
                                        ))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(150)))
                        )))
                ))
        );

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(createdOrder.getId(),
                ClientRole.SYSTEM, null,
                0, 10
        );

        OrderHistoryEvent event = events.getItems().stream()
                .filter(e -> HistoryEventType.NEW_ORDER == e.getType())
                .findFirst().orElseThrow();

        assertThat(event.getOrderAfter().getPromos(), hasItem(allOf(
                hasProperty("promoDefinition", hasProperty("type", is(DIRECT_DISCOUNT))),
                hasProperty("subsidy", comparesEqualTo(BigDecimal.valueOf(50)))
        )));
    }

    @Test
    public void shouldApplyDirectDiscountPromoWithSubsidyV2() {
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));
        firstItem.setSupplierType(SupplierType.THIRD_PARTY);

        promoConfigurer.applyDirectDiscount(firstItem, DD_PROMO, ANAPLAN_ID, SHOP_PROMO_KEY, BigDecimal.valueOf(150),
                BigDecimal.valueOf(50),
                true, true);

        Order createdOrder = orderCreateHelper.createOrder(promoConfigurer.applyTo(parameters));

        assertThat(
                createdOrder.getItems(), hasItem(allOf(
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(350))),
                        hasProperty("promoFaults", empty()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(
                                                DIRECT_DISCOUNT
                                        )),
                                        hasProperty("anaplanId", is(
                                                ANAPLAN_ID
                                        ))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(150)))
                        )))
                ))
        );

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(createdOrder.getId(),
                ClientRole.SYSTEM, null,
                0, 10
        );

        OrderHistoryEvent event = events.getItems().stream()
                .filter(e -> HistoryEventType.NEW_ORDER == e.getType())
                .findFirst().orElseThrow();

        assertThat(event.getOrderAfter().getPromos(), hasItem(allOf(
                hasProperty("promoDefinition", hasProperty("type", is(DIRECT_DISCOUNT))),
                hasProperty("subsidy", comparesEqualTo(BigDecimal.valueOf(50)))
        )));
    }

    @SafeVarargs
    @Nonnull
    private FoundOffer applyDirectDiscount(
            @Nonnull OrderItem item,
            @Nonnull BigDecimal discount,
            @Nullable BigDecimal subsidy,
            Consumer<FoundOfferBuilder>... customizers
    ) {
        return promoConfigurer.applyDirectDiscount(item, DD_PROMO, ANAPLAN_ID, SHOP_PROMO_KEY, discount, subsidy,
                true, false, customizers);
    }


    @Nonnull
    public static Order firstOrder(@Nonnull MultiCart cart) {
        return cart.getCarts().get(0);
    }
}
