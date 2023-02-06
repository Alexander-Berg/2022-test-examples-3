package ru.yandex.market.checkout.checkouter.promo.flash;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.PromoDetails;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.order.promo.ItemPromoFaultReason.INVALID_PROMO_DEFINITION;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.BLUE_FLASH;
import static ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer.applyFulfilmentParams;

public class BlueFlashPromoReportDiscountTest extends AbstractWebTestBase {

    public static final String PROMO_KEY = "some promo";
    public static final String ANAPLAN_ID = "some anaplan id";
    public static final String SHOP_PROMO_KEY = "some promo";

    private Parameters parameters;
    private Map<OrderItem, FoundOfferBuilder> foundOfferByItem;


    @BeforeEach
    public void setUp() throws Exception {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);
        foundOfferByItem = new HashMap<>();
    }

    @Test
    public void shouldApplyBlueFlashPromoWithLoyaltyAccept() {
        Order order = parameters.getOrder();

        OrderItem firstItem = order.getItems().iterator().next();
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        PromoDetails promoDetails = applyBlueFlashPromo(firstItem, BigDecimal.valueOf(300));
        acceptFlashPromo(promoDetails, firstItem, BigDecimal.valueOf(200));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(allOf(
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("prices",
                                hasProperty("buyerPriceNominal", comparesEqualTo(BigDecimal.valueOf(300)))),
                        hasProperty("promoFaults", empty()),
                        hasProperty("changes", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(
                                                BLUE_FLASH
                                        )),
                                        hasProperty("marketPromoId", is(
                                                PROMO_KEY
                                        )),
                                        hasProperty("shopPromoId", is(
                                                SHOP_PROMO_KEY
                                        )),
                                        hasProperty("anaplanId", is(
                                                ANAPLAN_ID
                                        ))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(200)))
                        )))
                ))
        );
    }

    @Test
    public void shouldApplyBlueFlashPromoWithNominalPriceCorrection() {
        Order order = parameters.getOrder();

        parameters.setExperiments(Experiments.ofName(Experiments.BUYER_PRICE_NOMINAL_NORMALIZATION));

        OrderItem firstItem = order.getItems().iterator().next();
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        PromoDetails promoDetails = applyBlueFlashPromo(firstItem, BigDecimal.valueOf(300));
        acceptFlashPromo(promoDetails, firstItem, BigDecimal.valueOf(200));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(allOf(
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("prices",
                                hasProperty("buyerPriceNominal", comparesEqualTo(BigDecimal.valueOf(300))))
                ))
        );
    }

    @Test
    public void shouldNotApplyBlueFlashPromoWithLoyaltyReject() {
        Order order = parameters.getOrder();

        OrderItem firstItem = order.getItems().iterator().next();
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        applyBlueFlashPromo(firstItem, BigDecimal.valueOf(300));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(allOf(
                        hasProperty("changes", hasItem(ItemChange.PRICE)),
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(500))),
                        hasProperty("prices",
                                hasProperty("buyerPriceNominal",
                                        comparesEqualTo(BigDecimal.valueOf(500)))),
                        hasProperty("promoFaults", empty()),
                        hasProperty("promos", empty())
                ))
        );
    }

    @Test
    public void shouldNotApplyBlueFlashPromoWithInvalidDiscount() {
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        applyBlueFlashPromo(firstItem, BigDecimal.valueOf(500));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order firstOrder = firstOrder(multiCart);

        assertThat(
                firstOrder.getItems(), hasItem(allOf(
                        hasProperty("changes", nullValue()),
                        hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(500))),
                        hasProperty("prices",
                                hasProperty("buyerPriceNominal",
                                        comparesEqualTo(BigDecimal.valueOf(500)))),
                        hasProperty("promoFaults", hasItem(allOf(
                                hasProperty("promoType", is(BLUE_FLASH)),
                                hasProperty("faultReason", is(INVALID_PROMO_DEFINITION))
                        ))),
                        hasProperty("promos", empty())
                ))
        );
    }

    @Test
    public void shouldAddPriceChangeOnWrongClientPrice() {
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        PromoDetails promoDetails = applyBlueFlashPromo(firstItem, BigDecimal.valueOf(300));
        firstItem.setBuyerPrice(BigDecimal.valueOf(400));
        acceptFlashPromo(promoDetails, firstItem, BigDecimal.valueOf(200));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(hasProperty("changes", hasItem(ItemChange.PRICE)))
        );
    }

    @Test
    public void shouldAddPriceChangeOnLoyaltyReject() {
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        applyBlueFlashPromo(firstItem, BigDecimal.valueOf(300));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(hasProperty("changes", hasItem(ItemChange.PRICE)))
        );
    }

    @Test
    public void shouldAddPriceChangeOnLoyaltyMistake() {
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        PromoDetails promoDetails = applyBlueFlashPromo(firstItem, BigDecimal.valueOf(300));
        acceptFlashPromo(promoDetails, firstItem, BigDecimal.valueOf(201));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(hasProperty("changes", hasItem(ItemChange.PRICE)))
        );
    }

    @Test
    public void shouldNotAddPriceChangeOnLoyaltyRejectAndClientPriceWithoutDiscount() {
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        applyBlueFlashPromo(firstItem, BigDecimal.valueOf(300));
        firstItem.setBuyerPrice(BigDecimal.valueOf(500));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order createdOrder = firstOrder(multiCart);

        assertThat(
                createdOrder.getItems(), hasItem(hasProperty("changes", nullValue()))
        );
    }

    public static Order firstOrder(MultiCart cart) {
        return cart.getCarts().get(0);
    }

    private PromoDetails applyBlueFlashPromo(OrderItem item, BigDecimal fixedPrice) {
        final PromoDetails promoDetails = PromoDetails.builder()
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.BLUE_FLASH.getCode())
                .promoFixedPrice(fixedPrice)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(2))
                .build();

        final FoundOfferBuilder offerBuilder = foundOfferByItem.computeIfAbsent(item, i ->
                FoundOfferBuilder.createFrom(item));

        offerBuilder.promoKey(promoDetails.getPromoKey())
                .promoType(promoDetails.getPromoType())
                .promoDetails(promoDetails)
                .price(fixedPrice)
                .oldMin(item.getBuyerPrice());

        parameters.getReportParameters().setOffers(foundOfferByItem.values().stream()
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toList()));

        item.setBuyerPrice(fixedPrice);

        return promoDetails;
    }

    private void acceptFlashPromo(PromoDetails promoDetails, OrderItem item, BigDecimal discount) {
        parameters.getLoyaltyParameters().expectResponseItem(
                OrderItemResponseBuilder.createFrom(item)
                        .promo(new ItemPromoResponse(
                                discount,
                                PromoType.BLUE_FLASH,
                                UUID.randomUUID().toString(),
                                promoDetails.getPromoKey(),
                                SHOP_PROMO_KEY,
                                null,
                                ANAPLAN_ID,
                                null,
                                false,
                                null,
                                null,
                                null
                        ))
        );
    }

}
