package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.LoggerFactory;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.ItemActualizer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.CurrencyRates;
import ru.yandex.market.checkout.checkouter.promo.blueset.BlueSetPromoFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.promo.flash.FlashPromoFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.util.OfferItemUtils;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.QuantityLimits;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.checkouter.actualization.utils.PushApiUtils.mapToPushApiCartResponse;
import static ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType.RESELLERS;

@ExtendWith(MockitoExtension.class)
public class ItemsPriceActualizerTest {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(Loggers.CART_DIFF_JSON_LOG);
    @Mock
    ItemActualizer itemActualizer;
    @Mock
    CurrencyRates currencyRates;
    @Mock
    ShopService shopService;
    @Mock
    FlashPromoFeatureSupportHelper flashPromoFeatureSupportHelper;
    @Mock
    BlueSetPromoFeatureSupportHelper blueSetPromoFeatureSupportHelper;
    @Spy
    CheckouterFeatureResolverStub properties = new CheckouterFeatureResolverStub();
    @Mock
    private SingleColorConfig singleColorConfig;
    @Mock
    private ColorConfig colorConfig;
    @InjectMocks
    private ItemsCountActualizer itemsCountActualizer;
    @Mock
    private Appender<ILoggingEvent> appender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captor;
    private Level oldLevel;


    @BeforeEach
    public void setUp() throws Exception {
        LOG.addAppender(appender);
        oldLevel = LOG.getLevel();
        LOG.setLevel(Level.INFO);

    }

    @AfterEach
    public void tearDown() {
        LOG.detachAppender(appender);
        LOG.setLevel(oldLevel);
    }

    @Test
    public void shouldWriteCartDiffCorrectly() throws Throwable {
        OrderItem orderItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));

        Order order = new Order();
        order.setItems(Collections.singletonList(orderItem));
        order.setDelivery(DeliveryProvider.getShopDelivery());
        order.setExchangeRate(BigDecimal.ONE);
        order.setContext(Context.MARKET);
        order.setIgnoreStocks(true);

        OrderItem responseItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));
        responseItem.setCount(0);
        CartResponse cartResponse = new CartResponse(Collections.singletonList(responseItem), null, null);

        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());
        boolean result = itemsCountActualizer.actualize(order, mapToPushApiCartResponse(cartResponse),
                ActualizationContext.builder()
                        .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext,
                                MultiCartProvider.single(order)))
                        .withCart(order)
                        .withInitialCart(ImmutableOrder.from(order))
                        .withOriginalBuyerCurrency(Currency.RUR)
                        .build());

        verify(appender).doAppend(captor.capture());
        LoggingEvent event = captor.getValue();

        assertFalse(result, "Cart should not be actual because of 0 count");
        assertThat(order.getItems(), hasSize(1));
        assertThat(order.getItems().iterator().next().getChanges(), hasItem(ItemChange.MISSING));
        JSONAssert.assertEquals(
                "{\"logType\":\"CART_DIFF\",\"event\":\"ITEM_COUNT\"," +
                        "\"cart\":{\"acceptMethod\":\"PUSH_API\",\"global\":false," +
                        "\"items\":[{\"feedOfferId\":{\"id\":\"1\"," +
                        "\"feedId\":1},\"offerName\":\"OfferName\",\"count\":1," +
                        "\"wareMd5\":\"-_40VqaS9BpXO1qaTtweBA\"," +
                        "\"buyerPrice\":250,\"feedId\":1,\"offerId\":\"1\"}],\"delivery\":{\"regionId\":213," +
                        "\"shopAddress\":{\"country\":\"Русь\",\"postcode\":\"131488\",\"city\":\"Питер\"," +
                        "\"street\":\"Победы\",\"building\":\"222\"}}},\"item\":{\"feedOfferId\":{\"id\":\"1\"," +
                        "\"feedId\":1}" +
                        ",\"offerName\":\"OfferName\",\"count\":1,\"wareMd5\":\"-_40VqaS9BpXO1qaTtweBA\"," +
                        "\"buyerPrice\":250," +
                        "\"feedId\":1,\"offerId\":\"1\"},\"additionalLoggingInfo\":{\"actualCartItemCount\":0}}",
                event.getMessage(),
                false
        );
    }

    @Test
    public void shouldIgnoreVat() throws Throwable {
        OrderItem orderItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));

        Order order = new Order();
        order.setItems(Collections.singletonList(orderItem));
        order.setDelivery(DeliveryProvider.getShopDelivery());
        order.setExchangeRate(BigDecimal.ONE);
        order.setContext(Context.MARKET);
        order.setIgnoreStocks(true);

        OrderItem responseItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));

        VatType clientVat = orderItem.getVat();
        assertFalse(clientVat == VatType.VAT_20_120);
        responseItem.setVat(VatType.VAT_20_120);

        CartResponse cartResponse = new CartResponse(Collections.singletonList(responseItem), null, null);


        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());
        itemsCountActualizer.actualize(order, mapToPushApiCartResponse(cartResponse), ActualizationContext.builder()
                .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext,
                        MultiCartProvider.single(order)))
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(order.getBuyerCurrency())
                .build());

        VatType vat = order.getItems().iterator().next().getVat();
        assertTrue(vat == clientVat);
    }

    @Test
    public void shouldFillItemSellerInnForReseller() throws Throwable {
        String sellerInn = "7710140679";
        String sellerInn2 = "7710140680";
        OrderItem orderItem = OrderItemProvider.defaultOrderItem();
        OrderItem orderItem2 = OrderItemProvider.defaultOrderItem();
        orderItem.setSellerInn(sellerInn);
        orderItem2.setSellerInn(sellerInn2);
        Order order = new Order();
        order.setItems(List.of(orderItem, orderItem2));
        order.setDelivery(DeliveryProvider.getShopDelivery());
        order.setIgnoreStocks(true);

        properties.writeValue(RESELLERS, List.of(orderItem.getSupplierId()));
        CartResponse cartResponse = new CartResponse(Collections.singletonList(orderItem), null, null);

        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());
        itemsCountActualizer.actualize(order, mapToPushApiCartResponse(cartResponse), ActualizationContext.builder()
                .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext,
                        MultiCartProvider.single(order)))
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(order.getBuyerCurrency())
                .build());

        assertThat(order.getItems(), hasItems(
                hasProperty("sellerInn", Matchers.is(sellerInn)),
                hasProperty("sellerInn", Matchers.is(sellerInn2))));
    }

    @ParameterizedTest
    @CsvSource({
            "2,1,0,false,MISSING",
            "2,2,2,true,COUNT",
            "2,3,2,false,COUNT",
            "10,1,0,false,MISSING",
            "10,2,0,false,MISSING",
            "10,3,3,true,COUNT",
            "10,4,3,false,COUNT",
            "10,8,8,true,COUNT",
            "10,10,10,true,COUNT",
            "10,13,10,false,COUNT",
    })
    public void shouldAddRemainingCountByStep(int stocksAmount,
                                              int initialCount,
                                              int finalCount,
                                              boolean shouldBeSuccess,
                                              ItemChange itemChange) {
        properties.writeValue(BooleanFeatureType.ENABLE_QUANTITY_LIMITS_ACTUALIZATION, true);
        lenient().doReturn(singleColorConfig).when(colorConfig).getFor(any(Order.class));
        Order order = OrderProvider.getBlueOrder();
        OrderItem orderItem = order.getItems().iterator().next();
        orderItem.setCount(initialCount);
        orderItem.setQuantity(BigDecimal.valueOf(initialCount));
        orderItem.setQuantityLimits(new QuantityLimits(3, 5));

        ActualizationContext actualizationContext = prepareActualizationContext(stocksAmount, order);
        PushApiCartResponse pushApiCartResponse = preparePushApiCartResponse(order);

        boolean actualActualizeResult = itemsCountActualizer.actualize(order, pushApiCartResponse,
                actualizationContext);

        assertEquals(shouldBeSuccess, actualActualizeResult);
        if (shouldBeSuccess) {
            assertNull(orderItem.getChanges());
        } else {
            assertThat(orderItem.getChanges(), hasItem(itemChange));
        }
        assertEquals(finalCount, orderItem.getCount());
        assertEquals(BigDecimal.valueOf(finalCount), orderItem.getQuantity());
    }

    @NotNull
    private PushApiCartResponse preparePushApiCartResponse(Order order) {
        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );
        return mapToPushApiCartResponse(cartResponse);
    }

    private ActualizationContext prepareActualizationContext(int stocksAmount, Order order) {
        MultiCartContext multiCartContext =
                MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(), Map.of());
        ImmutableMultiCartContext immutableMultiCartContext = spy(
                ImmutableMultiCartContext.from(multiCartContext, MultiCartProvider.single(order))
        );
        doReturn(Optional.of(stocksAmount)).when(immutableMultiCartContext).getItemStocksAmount(any(), any());
        return ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(order.getBuyerCurrency())
                .build();
    }
}
