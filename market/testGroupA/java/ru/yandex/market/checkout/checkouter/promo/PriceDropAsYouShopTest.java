package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.checkout.checkouter.order.SupplierType.THIRD_PARTY;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.MARKET_BLUE;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.PRICE_DROP_AS_YOU_SHOP;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.UNKNOWN;
import static ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer.applyFulfilmentParams;

public class PriceDropAsYouShopTest extends AbstractWebTestBase {

    private static final String PD_PROMO = "price drop";

    @Autowired
    private PromoConfigurer promoConfigurer;
    private Parameters parameters;

    @BeforeEach
    public void setUp() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_REPORT_DISCOUNT_VALUE, true);
    }

    @Test
    public void shouldApplyMarketBlueAndPriceDropPromoTogether() {
        //given
        final Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        final OrderItem firstItem = order.getItems().iterator().next();
        promoConfigurer.applyBlueDiscount(firstItem, BigDecimal.ONE, o -> o.supplierType(THIRD_PARTY));
        promoConfigurer.applyPriceDropDiscount(firstItem, PD_PROMO, PD_PROMO, BigDecimal.valueOf(9));
        promoConfigurer.applyTo(parameters);

        parameters.getLoyaltyParameters().expectResponseItem(OrderItemResponseBuilder.createFrom(firstItem)
                .promo(new ItemPromoResponse(
                        BigDecimal.valueOf(9),
                        PromoType.EXTERNAL,
                        null,
                        PD_PROMO,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );


        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(
                createdOrder.getPromos().stream().map(OrderPromo::getType).collect(Collectors.toList()),
                allOf(
                        hasItems(MARKET_BLUE, PRICE_DROP_AS_YOU_SHOP),
                        not(hasItem(UNKNOWN))
                )
        );
        assertThat(
                createdOrder.getPromos().stream().map(OrderPromo::getBuyerItemsDiscount).collect(Collectors.toList()),
                hasItems(BigDecimal.valueOf(1), BigDecimal.valueOf(9))
        );
        assertThat(createdOrder.getItems(), hasItem(allOf(
                hasProperty("promos", hasItems(
                        allOf(
                                hasProperty(
                                        "promoDefinition",
                                        hasProperty("type", is(MARKET_BLUE))
                                ),
                                hasProperty(
                                        "buyerDiscount",
                                        comparesEqualTo(BigDecimal.ONE)
                                )
                        ),
                        allOf(
                                hasProperty(
                                        "promoDefinition",
                                        hasProperty("type", is(PRICE_DROP_AS_YOU_SHOP))
                                ),
                                hasProperty(
                                        "buyerDiscount",
                                        comparesEqualTo(BigDecimal.valueOf(9))
                                )
                        ),
                        not(hasProperty(
                                "promoDefinition",
                                hasProperty("type", is(UNKNOWN))
                        ))
                ))
        )));
    }

    @Test
    public void shouldApplyPriceDropPromoOnly() {
        //given
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        promoConfigurer.applyPriceDropDiscount(firstItem, PD_PROMO, PD_PROMO, BigDecimal.TEN,
                o -> o.supplierType(THIRD_PARTY));
        promoConfigurer.applyTo(parameters);

        parameters.getLoyaltyParameters().expectResponseItem(OrderItemResponseBuilder.createFrom(firstItem)
                .promo(new ItemPromoResponse(
                        BigDecimal.TEN,
                        PromoType.EXTERNAL,
                        null,
                        PRICE_DROP_AS_YOU_SHOP.getCode(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(
                createdOrder.getPromos().stream().map(OrderPromo::getType).collect(Collectors.toList()),
                allOf(
                        hasItem(PRICE_DROP_AS_YOU_SHOP),
                        not(hasItem(UNKNOWN))
                )
        );
        assertThat(
                createdOrder.getPromos().stream().map(OrderPromo::getBuyerItemsDiscount).collect(Collectors.toList()),
                hasItems(BigDecimal.TEN)
        );
        assertThat(createdOrder.getItems(), hasItem(allOf(
                hasProperty("promos", hasItems(
                        allOf(
                                hasProperty(
                                        "promoDefinition",
                                        hasProperty("type", is(PRICE_DROP_AS_YOU_SHOP))
                                ),
                                hasProperty(
                                        "buyerDiscount",
                                        comparesEqualTo(BigDecimal.TEN)
                                )
                        ),
                        not(hasProperty(
                                "promoDefinition",
                                hasProperty("type", is(UNKNOWN))
                        ))
                ))
        )));
    }

    @Test
    public void shouldApplyPriceDropPromoWithSubsidy() {
        //given
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        promoConfigurer.applyPriceDropDiscount(firstItem, PD_PROMO, PD_PROMO, BigDecimal.TEN,
                o -> o.supplierType(THIRD_PARTY));
        promoConfigurer.applyTo(parameters);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        createdOrder = client.getOrder(createdOrder.getId(), ClientRole.SYSTEM, 0L);

        assertThat(createdOrder.getItems(), everyItem(
                hasProperty("promos", hasItem(allOf(
                        hasProperty(
                                "promoDefinition",
                                hasProperty("type", is(PRICE_DROP_AS_YOU_SHOP))
                        ),
                        hasProperty("subsidy", comparesEqualTo(BigDecimal.TEN))
                )))
        ));
    }

    @Test
    public void shouldApplyMarketBluePromoOnly() {
        //given
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        promoConfigurer.applyBlueDiscount(firstItem, BigDecimal.TEN,
                o -> o.supplierType(THIRD_PARTY));
        promoConfigurer.applyTo(parameters);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(
                createdOrder.getPromos().stream().map(OrderPromo::getType).collect(Collectors.toList()),
                hasItems(MARKET_BLUE)
        );
        assertThat(
                createdOrder.getPromos().stream().map(OrderPromo::getBuyerItemsDiscount).collect(Collectors.toList()),
                hasItems(BigDecimal.TEN)
        );
    }

    @Test
    public void shouldNotApplyPromoWithoutReportInfo() {
        //given
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));

        OrderItem firstItem = order.getItems().iterator().next();
        ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(firstItem.getFeedOfferId());
        itemInfo.setSupplierType(THIRD_PARTY);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(
                createdOrder.getPromos().stream().map(OrderPromo::getType).collect(Collectors.toList()),
                empty()
        );
        assertThat(
                createdOrder.getPromos().stream().map(OrderPromo::getBuyerItemsDiscount).collect(Collectors.toList()),
                empty()
        );
    }
}
