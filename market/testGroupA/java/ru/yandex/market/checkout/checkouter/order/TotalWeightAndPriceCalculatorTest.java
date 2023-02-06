package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.utils.TotalWeightAndPriceCalculator;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.model.OrderImmutableView;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableItemStockAvailable;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder.getExperiments;
import static ru.yandex.market.common.report.model.Color.BLUE;
import static ru.yandex.market.common.report.model.Color.WHITE;

/**
 * @author zagidullinri
 * @date 03.06.2021
 */
@ExtendWith(MockitoExtension.class)
public class TotalWeightAndPriceCalculatorTest {
    private final CheckouterFeatureResolverStub checkouterFeature = new CheckouterFeatureResolverStub();
    private TotalWeightAndPriceCalculator totalWeightAndPriceCalculator;

    @Mock
    private ColorConfig colorConfig;
    @Mock
    private SingleColorConfig singleColorConfig;
    @Mock
    private SingleColorConfig whiteConfig;

    private ImmutableMultiCartParameters multiCartParameters;

    @BeforeEach
    public void setUp() {
        mockColorConfig();
        setUpActualizationContext();
        setUpTotalWeightAndPriceCalculator();
        enableUnifiedTariffs();
    }

    @Test
    public void shouldCalcByMultiCartOffersWhenOrderIsFulfillment() {
        var context = makeContext(
                makeOrder(ffOffer()),
                makeOrder(dbsOffer()),
                makeOrder(edaOffer()),
                makeOrder(expressFbsOffer()),
                makeOrder(clickAndCollectOffer())
        );


        var totals = totalWeightAndPriceCalculator.calculateTotalPriceAndTotalWeight(
                context.getCart().getItems().stream()
                        .map(i -> new ImmutableItemStockAvailable(i, i.getCount().orElse(0)))
                        .collect(Collectors.toUnmodifiableList()), ImmutableActualizationContext.of(context));

        assertThat(totals.getPrice(), comparesEqualTo(BigDecimal.valueOf(3)));
        assertThat(totals.getWeight(), comparesEqualTo(BigDecimal.valueOf(3)));
    }

    @Test
    public void shouldCalcByOrderWhenOrderIsEda() {
        var context = makeContext(
                makeOrder(edaOffer()),
                makeOrder(ffOffer()),
                makeOrder(dbsOffer()),
                makeOrder(expressFbsOffer()),
                makeOrder(clickAndCollectOffer())
        );


        var totals = totalWeightAndPriceCalculator.calculateTotalPriceAndTotalWeight(
                context.getCart().getItems().stream()
                        .map(i -> new ImmutableItemStockAvailable(i, i.getCount().orElse(0)))
                        .collect(Collectors.toUnmodifiableList()), ImmutableActualizationContext.of(context));

        assertThat(totals.getPrice(), comparesEqualTo(BigDecimal.valueOf(4)));
        assertThat(totals.getWeight(), comparesEqualTo(BigDecimal.valueOf(4)));
    }

    @Test
    public void shouldCalcByOrderWhenOrderIsExpress() {
        var context = makeContext(
                makeOrder(expressFbsOffer()),
                makeOrder(ffOffer()),
                makeOrder(dbsOffer()),
                makeOrder(edaOffer()),
                makeOrder(clickAndCollectOffer())
        );


        var totals = totalWeightAndPriceCalculator.calculateTotalPriceAndTotalWeight(
                context.getCart().getItems().stream()
                        .map(i -> new ImmutableItemStockAvailable(i, i.getCount().orElse(0)))
                        .collect(Collectors.toUnmodifiableList()), ImmutableActualizationContext.of(context));

        assertThat(totals.getPrice(), comparesEqualTo(BigDecimal.valueOf(8)));
        assertThat(totals.getWeight(), comparesEqualTo(BigDecimal.valueOf(8)));
    }

    @Test
    public void shouldCalcByOrderWhenOrderIsClickAndCollect() {
        var context = makeContext(
                makeOrder(clickAndCollectOffer()),
                makeOrder(ffOffer()),
                makeOrder(dbsOffer()),
                makeOrder(edaOffer()),
                makeOrder(expressFbsOffer())
        );


        var totals = totalWeightAndPriceCalculator.calculateTotalPriceAndTotalWeight(
                context.getCart().getItems().stream()
                        .map(i -> new ImmutableItemStockAvailable(i, i.getCount().orElse(0)))
                        .collect(Collectors.toUnmodifiableList()), ImmutableActualizationContext.of(context));

        assertThat(totals.getPrice(), comparesEqualTo(BigDecimal.valueOf(16)));
        assertThat(totals.getWeight(), comparesEqualTo(BigDecimal.valueOf(16)));
    }

    @Test
    public void shouldSkipWeightIfAbsent() {
        var ffOfferWithoutWeight = ffOffer();
        ffOfferWithoutWeight.setWeight(null);
        var context = makeContext(
                makeOrder(ffOffer(), ffOfferWithoutWeight),
                makeOrder(dbsOffer()),
                makeOrder(edaOffer()),
                makeOrder(expressFbsOffer()),
                makeOrder(clickAndCollectOffer())
        );
        var totals = totalWeightAndPriceCalculator.calculateTotalPriceAndTotalWeight(
                context.getCart().getItems().stream()
                        .map(i -> new ImmutableItemStockAvailable(i, i.getCount().orElse(0)))
                        .collect(Collectors.toUnmodifiableList()), ImmutableActualizationContext.of(context));

        assertThat(totals.getPrice(), comparesEqualTo(BigDecimal.valueOf(4)));
        assertThat(totals.getWeight(), comparesEqualTo(BigDecimal.valueOf(3)));
    }

    private Map.Entry<Order, List<FoundOffer>> makeOrder(FoundOffer first, FoundOffer... offers) {
        List<FoundOffer> offerList = Lists.asList(first, offers);
        Order order = OrderProvider.getBlueOrder();

        if (offerList.stream().allMatch(FoundOffer::isFulfillment)) {
            order.setFulfilment(true);
            order.setRgb(Color.BLUE);
        } else if (offerList.stream().anyMatch(o -> o.isEda() == Boolean.TRUE)) {
            order.setProperty(OrderPropertyType.IS_EDA, true);
        } else if (offerList.stream().anyMatch(OrderTypeUtils::isClickAndCollect)) {
            order.setIsClickAndCollect(true);
        }
        var offerColor = offerList.stream()
                .map(FoundOffer::getRgb)
                .distinct()
                .findFirst().orElseThrow();
        switch (offerColor) {
            case WHITE:
                order.setRgb(Color.WHITE);
                break;
            case BLUE:
                order.setRgb(Color.BLUE);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        order.setItems(offerList.stream().map(this::createOrderItem).collect(Collectors.toUnmodifiableList()));
        return Map.entry(order, offerList);
    }

    @SafeVarargs
    private ActualizationContext makeContext(Map.Entry<Order, List<FoundOffer>> orderEntry,
                                             Map.Entry<Order, List<FoundOffer>>... orders) {
        Set<FeedOfferId> offerIdSet = orderEntry.getValue().stream()
                .map(FoundOffer::getFeedOfferId)
                .collect(Collectors.toUnmodifiableSet());
        MultiCart multiCart = MultiCartProvider.buildMultiCart(Lists.asList(orderEntry, orders).stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableList()));
        MultiCartContext multiCartContext = MultiCartContext.createBy(multiCartParameters, multiCart);
        MultiCartFetchingContext fetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);
        FlowSessionHelper.patchSession(
                fetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setOffersStage(v),
                Lists.asList(orderEntry, orders).stream()
                        .map(Map.Entry::getValue)
                        .flatMap(List::stream)
                        .collect(Collectors.toUnmodifiableList())
        );
        return ActualizationContext.builder()
                .withCart(orderEntry.getKey())
                .withOriginalBuyerCurrency(Currency.RUR)
                .withOfferPredicate(o -> offerIdSet.contains(o.getFeedOfferId()))
                .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart))
                .withCart(orderEntry.getKey())
                .withInitialCart(ImmutableOrder.from(orderEntry.getKey()))
                .withExperiments(getExperiments().with(Map.of(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE)))
                .build();
    }

    private void enableUnifiedTariffs() {
        checkouterFeature.writeValue(ENABLE_UNIFIED_TARIFFS, true);
    }

    private void mockColorConfig() {
        lenient().when(colorConfig.getFor(any(Order.class))).thenCallRealMethod();
        lenient().when(colorConfig.getFor(any(OrderImmutableView.class))).thenCallRealMethod();
        lenient().when(colorConfig.getFor(any(Color.class))).thenAnswer(inv -> {
            Color color = inv.getArgument(0);
            if (Color.WHITE.equals(color)) {
                return whiteConfig;
            } else {
                return singleColorConfig;
            }
        });
        lenient().when(singleColorConfig.useOnlyActualDeliveryOptions()).thenReturn(true);
        lenient().when(whiteConfig.useOnlyActualDeliveryOptions()).thenReturn(true);
        lenient().when(singleColorConfig.selfDeliveryIsClickAndCollect()).thenReturn(true);
        lenient().when(whiteConfig.selfDeliveryIsClickAndCollect()).thenReturn(false);
    }

    private void setUpActualizationContext() {
        multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(10L)
                .withIsMultiCart(false)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withActionId("actionId")
                .build();
    }

    private void setUpTotalWeightAndPriceCalculator() {
        totalWeightAndPriceCalculator = new TotalWeightAndPriceCalculator(colorConfig, checkouterFeature);
    }

    private FoundOffer ffOffer() {
        return FoundOfferBuilder.create()
                .color(BLUE)
                .feedId(ThreadLocalRandom.current().nextLong())
                .offerId(UUID.randomUUID().toString())
                .price(BigDecimal.ONE)
                .weight(1)
                .supplierType(SupplierType.FIRST_PARTY)
                .deliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET.name())
                .isFulfillment(true)
                .atSupplierWarehouse(false)
                .build();
    }

    private FoundOffer dbsOffer() {
        return FoundOfferBuilder.create()
                .color(WHITE)
                .feedId(ThreadLocalRandom.current().nextLong())
                .offerId(UUID.randomUUID().toString())
                .price(BigDecimal.valueOf(2))
                .weight(2)
                .isFulfillment(false)
                .atSupplierWarehouse(true)
                .supplierType(SupplierType.THIRD_PARTY)
                .deliveryPartnerType(DeliveryPartnerType.SHOP.name())
                .build();
    }

    private FoundOffer edaOffer() {
        var offer = FoundOfferBuilder.create()
                .color(WHITE)
                .feedId(ThreadLocalRandom.current().nextLong())
                .offerId(UUID.randomUUID().toString())
                .price(BigDecimal.valueOf(4))
                .weight(4)
                .yandexEda(true)
                .isFulfillment(false)
                .atSupplierWarehouse(true)
                .supplierType(SupplierType.THIRD_PARTY)
                .build();

        assertTrue(OrderTypeUtils.isEda(offer));
        return offer;
    }

    private FoundOffer expressFbsOffer() {
        var offer = FoundOfferBuilder.create()
                .color(BLUE)
                .feedId(ThreadLocalRandom.current().nextLong())
                .offerId(UUID.randomUUID().toString())
                .price(BigDecimal.valueOf(8))
                .weight(8)
                .express(true)
                .isFulfillment(false)
                .supplierType(SupplierType.THIRD_PARTY)
                .deliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET.name())
                .build();

        assertTrue(OrderTypeUtils.isExpress(offer));
        return offer;
    }

    private FoundOffer clickAndCollectOffer() {
        var offer = FoundOfferBuilder.create()
                .color(BLUE)
                .feedId(ThreadLocalRandom.current().nextLong())
                .offerId(UUID.randomUUID().toString())
                .price(BigDecimal.valueOf(16))
                .weight(16)
                .isFulfillment(false)
                .atSupplierWarehouse(true)
                .supplierType(SupplierType.THIRD_PARTY)
                .deliveryPartnerType(DeliveryPartnerType.SHOP.name())
                .build();

        assertTrue(OrderTypeUtils.isClickAndCollect(offer));
        return offer;
    }

    @Nonnull
    private OrderItem createOrderItem(FoundOffer offer) {
        OrderItem orderItem = OrderItemProvider.buildOrderItem(offer.getFeedOfferId());
        orderItem.setBuyerPrice(offer.getPrice());
        orderItem.getPrices().setReportPrice(offer.getPrice());
        orderItem.setWeight(offer.getWeight() != null
                ? offer.getWeight().multiply(BigDecimal.valueOf(1000)).longValue()
                : null);
        return orderItem;
    }
}
