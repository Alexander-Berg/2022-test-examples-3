package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryResultProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryMethod;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.PickupOptionOutlet;
import ru.yandex.market.common.report.model.specs.Specs;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.common.util.collections.CollectionUtils.emptyIfNull;
import static ru.yandex.common.util.collections.CollectionUtils.firstOrNull;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.ActualDeliveryBuilder.DEFAULT_INTAKE_SHIPMENT_DAYS;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.defaultActualDelivery;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryResultProvider.getActualDeliveryResult;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.DEFAULT_WARE_MD5;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;


/**
 * @author mmetlov
 */
public class YandexMarketDeliveryPickupTest extends AbstractWebTestBase {

    private static final long EXISTING_DELIVERY_SERVICE_OUTLET_NOT_IN_OUTLETS_XML = 741258L;
    private static final long OUTLET_NOT_IN_OUTLETS_XML = 741259L;
    private static final long DELIVERY_SERVICE_NOT_IN_OUTLETS_XML = 147258L;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Tag(Tags.AUTO)
    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("shouldCreateOrderToSelfOutlet")
    @Test
    public void shouldCreateOrderToSelfOutlet() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getId(), notNullValue());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("shouldNotCreateOrderToWrongOutletId")
    @Test
    public void shouldNotCreateOrderToWrongOutletId() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setOutletId(123456789L);
        parameters.setCheckOrderCreateErrors(false);
        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);
        assertNull(order.getOrders());
        assertEquals("Actualization error: delivery options mismatch.",
                order.getOrderFailures().get(0).getErrorDetails());
        assertEquals(OrderFailure.Code.OUT_OF_DATE, order.getOrderFailures().get(0).getErrorCode());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("shouldNotCreateOrderToOtherShopOutletId")
    @Test
    public void shouldNotCreateOrderToOtherShopOutletId() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setOutletId(421448L);
        parameters.setCheckOrderCreateErrors(false);
        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);
        assertNull(order.getOrders());
        assertEquals("Actualization error: delivery options mismatch.",
                order.getOrderFailures().get(0).getErrorDetails());
        assertEquals(OrderFailure.Code.OUT_OF_DATE, order.getOrderFailures().get(0).getErrorCode());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Должны разрешать постоплату для МарДошных заказов")
    @Test
    public void shouldAddPostPaidYandexMarketDelivery() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setColor(BLUE);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertNotNull(cart.getCarts());
        assertThat(cart.getCarts(), hasSize(1));

        Delivery marDoOption = cart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(it -> it.getDeliveryServiceId().equals(MOCK_DELIVERY_SERVICE_ID))
                .findAny()
                .get();

        assertThat(
                marDoOption,
                hasProperty("paymentOptions",
                        containsInAnyOrder(
                                PaymentMethod.YANDEX,
                                PaymentMethod.APPLE_PAY,
                                PaymentMethod.GOOGLE_PAY,
                                PaymentMethod.CASH_ON_DELIVERY,
                                PaymentMethod.CARD_ON_DELIVERY
                        )
                )
        );
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверка допустимого для рецептурки метода доставки")
    @Test
    public void shouldAddInternalSpecs() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        OrderItem item = parameters.getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Должен быть хотя бы один OrderItem"));
        item.setMedicalSpecsInternal(
                Specs.fromSpecValues(Set.of("psychotropic", "baa", "medicine", "prescription")));
        Order order = orderCreateHelper.createOrder(parameters);
        assertNotNull(order.getId());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("shouldCreatePostPaidYandexMarketDeliveryWithPaidByCashAndCommissionPercentageAndFz54Data")
    @Test
    public void shouldCreatePostPaidYandexMarketDeliveryWithPaidByCashAndCommissionPercentageAndFz54Data() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(OrderTypeUtils.isCashProcessedByMarket(order));

        assertNotNull(order.getId());

        Order orderInDB = orderService.getOrder(order.getId());
        assertThat("taxSystem should not be null", orderInDB.getTaxSystem(), notNullValue());
        orderInDB.getItems().forEach(oi -> {
            assertThat("item " + oi.getFeedOfferId() + " vat should not be null", oi.getVat(), notNullValue());
        });
        if (!orderInDB.getDelivery().isFree()) {
            assertThat("delivery vat", orderInDB.getDelivery().getVat(), notNullValue());
        }
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ в ПВЗ с недостаточной суммой для бесплатной доставки")
    @Test
    public void bluePickupOrderWithoutFreeDelivery() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setFreeDelivery(false);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getDelivery().getBuyerPrice().intValue(), greaterThan(0));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ в ПВЗ с бесплатной доставкой")
    @Test
    public void bluePickupOrderWithFreeDelivery() {
        Order order = OrderProvider.getBlueOrder(o -> {
            o.addItem(OrderItemProvider.getOrderItem());
            o.getItems().forEach(oi -> {
                oi.setWareMd5(null);
                oi.setShowInfo(null);
            });
        });

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withOrder(order)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(MOCK_DELIVERY_SERVICE_ID, 1)
                                .withFreeDelivery()
                                .build()
                )
                .buildParameters();
        FeedOfferId firstItemId = Iterables.get(parameters.getOrder().getItems(), 0).getFeedOfferId();
        FeedOfferId secondItemId = Iterables.get(parameters.getOrder().getItems(), 1).getFeedOfferId();
        parameters.getReportParameters().overrideItemInfo(firstItemId).setWareMd5(DEFAULT_WARE_MD5);
        parameters.getReportParameters().overrideItemInfo(secondItemId).setWareMd5(OrderItemProvider.ANOTHER_WARE_MD5);
        parameters.setFreeDelivery(true);

        parameters.setMultiCartAction((mc) -> {
            Order cart = Iterables.getOnlyElement(mc.getCarts());
            cart.getItem(firstItemId).setShowInfo(OrderItemProvider.SHOW_INFO);
            cart.getItem(secondItemId).setShowInfo(OrderItemProvider.ANOTHER_SHOW_INFO);
        });

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        multiCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(option -> option.getDeliveryPartnerType() == YANDEX_MARKET)
                .forEach(option -> assertThat(option.getBuyerPrice(), is(BigDecimal.ZERO)));

        Order created = orderCreateHelper.createOrder(parameters);

        assertThat(created.getRgb(), is(Color.BLUE));
        assertThat(created.getDelivery().getBuyerPrice(), is(BigDecimal.ZERO));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ с самовывозом c опциями из actual_delivery")
    @Test
    public void bluePickupWithActualDeliveryOption() throws Exception {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID)
                .withShipmentDay(shipmentDays)
                .withPartnerInterface(true)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(MOCK_DELIVERY_SERVICE_ID, shipmentDays)
                                .build()
                )
                .buildParameters();

        orderCreateHelper.setupShopsMetadata(parameters);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        List<? extends Delivery> marDoPickupOptions = Iterables.getOnlyElement(multiCart.getCarts())
                .getDeliveryOptions()
                .stream()
                .filter(option -> option.getDeliveryPartnerType() == YANDEX_MARKET)
                .filter(option -> option.getType() == DeliveryType.PICKUP)
                .collect(Collectors.toList());
        assertThat(marDoPickupOptions, hasSize(1));
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        Order order = firstOrNull(emptyIfNull(multiOrder.getOrders()));

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getDelivery().getOutletId(), is(12312301L));
        assertThat(order.getDelivery().getDeliveryServiceId(), is(MOCK_DELIVERY_SERVICE_ID));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ с пикап опциями где Shipment Day null")
    @Test
    public void bluePickupWithActualDeliveryOptionWithShipmentDaysNull() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID)
                .withShipmentDay(1)
                .withPartnerInterface(true)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(MOCK_DELIVERY_SERVICE_ID, null)
                                .build()
                )
                .buildParameters();

        orderCreateHelper.setupShopsMetadata(parameters);
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].validationErrors").value(hasSize(1)))
                .andExpect(
                        jsonPath("$.carts[*].validationErrors[0].code")
                                .value(containsInAnyOrder("NO_ACTUAL_DELIVERY_OPTIONS"))
                )
                .andExpect(
                        jsonPath("$.carts[*].validationErrors[0].message")
                                .value(containsInAnyOrder("No delivery options in actual_delivery"))
                );
        MultiCart multiCart = orderCreateHelper.cart(parameters);
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ с самовывозом без опций из actual_delivery")
    @Test
    public void bluePickupWithoutActualDeliveryOption() {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID)
                .withShipmentDay(shipmentDays)
                .withPartnerInterface(true)
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddress());
        // подкладываем опцию в actual_delivery
        ActualDeliveryResult actualDeliveryResult = getActualDeliveryResult();
        actualDeliveryResult.setPickup(null);
        ActualDelivery actualDelivery = defaultActualDelivery();
        actualDelivery.setResults(singletonList(actualDeliveryResult));
        parameters.getReportParameters().setActualDelivery(actualDelivery);
        //убираем подложенную опцию под айтемом
        parameters.getReportParameters().setLocalDeliveryOptions(Collections.EMPTY_MAP);
        parameters.setFreeDelivery(false);

        orderCreateHelper.setupShopsMetadata(parameters);
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].validationErrors").value(hasSize(1)))
                .andExpect(
                        jsonPath("$.carts[*].validationErrors[0].code")
                                .value(containsInAnyOrder("NO_ACTUAL_DELIVERY_OPTIONS"))
                );
        MultiCart multiCart = orderCreateHelper.cart(parameters);
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Мардошный заказ из региона в который не доставляется по deliveryMethods, но в регион, в который " +
            "доставляется")
    @Test
    public void shouldGetDeliveryMethodsFromDeliveryRegion() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .buildParameters();

        parameters.getBuiltMultiCart().setBuyerRegionId(2L);
        parameters.getReportParameters().setRegionId(2L);
        parameters.getReportParameters().setDeliveryMethods(Collections.emptyList());

        ReportGeneratorParameters deliveryRegionReportParameters = new ReportGeneratorParameters(parameters.getOrder());
        deliveryRegionReportParameters.setRegionId(parameters.getOrder().getDelivery().getRegionId());

        parameters.setDeliveryRegionReportParameters(deliveryRegionReportParameters);
        parameters.setMultiCartAction(mc ->
                mc.getCarts().forEach(c -> {
                    c.getDelivery().setRegionId(parameters.getOrder().getDelivery().getRegionId());
                })
        );

        Order order = orderCreateHelper.createOrder(parameters);
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("В синем маркете не должны учитываться доступные службы пооферно")
    @Test
    public void blueDeliveryShouldIgnoreOfferDeliveryMethods() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID)
                .withShipmentDay(1)
                .withPartnerInterface(true)
                .buildParameters();
        DeliveryMethod onlyDeliveryMethod = new DeliveryMethod();
        onlyDeliveryMethod.setMarketBranded(true);
        onlyDeliveryMethod.setServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID.toString());
        parameters.getReportParameters().setDeliveryMethods(singletonList(onlyDeliveryMethod));

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ с самовывозом c аутлетом, которой нет в shopOutlets.xml")
    @Test
    public void bluePickupWithActualDeliveryOptionWithUnknownOutlets() throws Exception {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID)
                .withShipmentDay(shipmentDays)
                .withPartnerInterface(true)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(
                                        MOCK_DELIVERY_SERVICE_ID,
                                        shipmentDays,
                                        singletonList(EXISTING_DELIVERY_SERVICE_OUTLET_NOT_IN_OUTLETS_XML)
                                )
                                .build()
                )
                .buildParameters();

        orderCreateHelper.setupShopsMetadata(parameters);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        List<? extends Delivery> marDoPickupOptions = Iterables.getOnlyElement(multiCart.getCarts())
                .getDeliveryOptions()
                .stream()
                .filter(option -> option.getDeliveryPartnerType() == YANDEX_MARKET)
                .filter(option -> option.getType() == DeliveryType.PICKUP)
                .collect(Collectors.toList());
        assertThat(marDoPickupOptions, hasSize(1));
        parameters.setOutletId(EXISTING_DELIVERY_SERVICE_OUTLET_NOT_IN_OUTLETS_XML);
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        Order order = Iterables.getOnlyElement(multiOrder.getOrders());

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getDelivery().getOutletId(), is(EXISTING_DELIVERY_SERVICE_OUTLET_NOT_IN_OUTLETS_XML));
        assertThat(order.getDelivery().getDeliveryServiceId(), is(MOCK_DELIVERY_SERVICE_ID));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ с самовывозом cо службой, которой нет в shopOutlets.xml")
    @Test
    public void bluePickupWithActualDeliveryOptionWithUnknownDeliveryService() throws Exception {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(DELIVERY_SERVICE_NOT_IN_OUTLETS_XML)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID)
                .withShipmentDay(shipmentDays)
                .withPartnerInterface(true)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(
                                        DELIVERY_SERVICE_NOT_IN_OUTLETS_XML,
                                        shipmentDays,
                                        singletonList(OUTLET_NOT_IN_OUTLETS_XML)
                                )
                                .build()
                )
                .buildParameters();

        orderCreateHelper.setupShopsMetadata(parameters);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        List<? extends Delivery> marDoPickupOptions = Iterables.getOnlyElement(multiCart.getCarts())
                .getDeliveryOptions()
                .stream()
                .filter(option -> option.getDeliveryPartnerType() == YANDEX_MARKET)
                .filter(option -> option.getType() == DeliveryType.PICKUP)
                .collect(Collectors.toList());
        assertThat(marDoPickupOptions, hasSize(1));
        parameters.setOutletId(OUTLET_NOT_IN_OUTLETS_XML);
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        Order order = Iterables.getOnlyElement(multiOrder.getOrders());

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getDelivery().getOutletId(), is(OUTLET_NOT_IN_OUTLETS_XML));
        assertThat(order.getDelivery().getDeliveryServiceId(), is(DELIVERY_SERVICE_NOT_IN_OUTLETS_XML));
    }

    @Test
    public void shouldNotReturnDeliveryOptionsWithoutOutlets() {
        final Long unknownDeliveryServiceId = -1L;
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryServiceId(unknownDeliveryServiceId)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(2)
                .withActualDelivery(ActualDeliveryProvider
                        .builder()
                        .addPickup(unknownDeliveryServiceId, DEFAULT_INTAKE_SHIPMENT_DAYS, List.of())
                        .addDelivery(unknownDeliveryServiceId)
                        .build())
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddress());

        parameters.getReportParameters().setLocalDeliveryOptions(Collections.emptyMap());
        parameters.setFreeDelivery(true);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertNotNull(cart.getCarts());
        assertThat(cart.getCarts(), hasSize(1));
        final Order order = cart.getCarts().get(0);
        assertThat(order.getDeliveryOptions(), hasSize(1));
        final List<? extends Delivery> pickupOptions = order.getDeliveryOptions()
                .stream()
                .filter(d -> d.getType() == DeliveryType.PICKUP)
                .collect(Collectors.toList());
        assertThat(pickupOptions, hasSize(0));
    }


    @Test
    public void minifyTest() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setMinifyOutlets(true);

        orderCreateHelper.cart(parameters);

        reportMock.findAll(
                        getRequestedFor(anyUrl())
                                .withQueryParam(
                                        "place",
                                        WireMock.equalTo("outlets")
                                ))
                .forEach(r -> assertNull(r.getQueryParams().get("deliveryServiceId")));
    }

    @Test
    public void shouldCreatePickupOrderViaCombinatorFlow() {
        setFixedTime(Instant.parse("2040-07-28T10:15:30Z"));
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);
        JSONAssert.assertEquals(
                DeliveryResultProvider.ROUTE,
                order.getDelivery().getParcels().get(0).getRoute().toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );

        ServeEvent deliveryRouteServeEvent = getDeliveryRouteServeEvent();
        assertEquals("pickup", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-type").values()));
        assertEquals(Long.toString(DeliveryProvider.REGION_ID), getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("rids").values()));
        assertEquals("20400728.0000-20400730.2359", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-interval").values()));
        assertEquals(order.getDelivery().getOutletId().toString(), getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("point_id").values()));
        assertNull(deliveryRouteServeEvent.getRequest().getQueryParams().get("post-index"), "post-index");
        assertEquals("prepayment_card", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("payments").values()));
        assertEquals(DEFAULT_WARE_MD5 + ":1",
                getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("offers-list").values()));
    }

    @Test
    public void shouldntCallDeliveryRouteOnCart() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);

        parameters.getBuiltMultiCart().getCarts().get(0).setDelivery(DeliveryProvider.getShopDelivery());
        parameters.setCheckCartErrors(false);

        reportMock.resetRequests();
        orderCreateHelper.cart(parameters);
        assertFalse(reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && req.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route"))
                .findFirst()
                .isPresent());
    }

    @Test
    public void shouldThrowSpecificErrorWhenDeliveryRouteIsntSuccesfulAndActualDeliveryHasSpecifiedOption() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);

        parameters.getReportParameters().getDeliveryRoute().setCommonProblems(
                singletonList("COMBINATOR_ROUTE_DOESNT_MEET_SPECIFIED_CONDITIONS"));
        parameters.setCheckOrderCreateErrors(false);
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertEquals(
                "DELIVERY_ROUTE_FAILED",
                multiOrder.getOrders().get(0).getValidationErrors().get(0).getCode());
    }

    @Test
    public void shouldThrowUsualChangesWhenDeliveryRouteIsntSuccesfulAndActualDeliveryHasntSpecifiedOption()
            throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);

        parameters.getReportParameters().getDeliveryRoute().setCommonProblems(
                singletonList("COMBINATOR_ROUTE_DOESNT_MEET_SPECIFIED_CONDITIONS"));
        parameters.setCheckOrderCreateErrors(false);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        parameters.getReportParameters().getActualDelivery().getResults().get(0).setPickup(emptyList());
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, parameters.getReportParameters());
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertEquals("Actualization error: delivery options mismatch.",
                multiOrder.getOrderFailures().get(0).getErrorDetails());
        assertEquals(singleton(CartChange.DELIVERY), multiOrder.getOrderFailures().get(0).getOrder().getChanges());
    }

    private ServeEvent getDeliveryRouteServeEvent() {
        return reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && req.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route"))
                .findFirst().get();
    }

    @Test
    public void shouldSaveRouteForDsbsToBrandedPickup() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, SwitchWithWhitelist.enabledForAll());
        checkouterProperties.setEnableDbsThroughMarketBrandedPickup(true);

        var parameters = WhiteParametersProvider.dbsPickupOrderWithCombinatorParameters(po -> {
            po.setMarketBranded(true);
        });

        Order order = orderCreateHelper.createOrder(parameters);
        assertNotNull(order.getDelivery().getParcels().get(0).getRoute());
        assertEquals(1, order.getDelivery().getParcels().size());
    }

    @Test
    @DisplayName("Для DBS заказов в drop off должен сохранятся delivery route")
    public void shouldSaveRouteForDbsWithDbsWithRouteFeature() {
        checkouterProperties.setEnableDbsThroughMarketBrandedPickup(true);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, SwitchWithWhitelist.enabledForAll());
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE, true);

        var parameters = WhiteParametersProvider.dbsPickupOrderWithCombinatorParameters(po -> {
            po.setMarketBranded(false);
            po.setIsExternalLogistics(false);
            po.setOutlet(new PickupOptionOutlet() {{
                setId(1L);
            }});
        });

        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.getDelivery().containsFeature(DeliveryFeature.DBS_WITH_ROUTE));
        assertNotNull(order.getDelivery().getParcels().get(0).getRoute());
        assertEquals(1, order.getDelivery().getParcels().size());
    }

    @Test
    public void shouldSendActualDeliveryRequestWithCombinatorParameterIsFalse() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.FORCE_COMBINATOR_CHECK, false);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        orderCreateHelper.cart(parameters);
        ServeEvent serveEvent = reportMock.getServeEvents().getRequests()
                .stream()
                .filter(r -> r.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery"))
                .findFirst()
                .get();
        assertThat(serveEvent.getRequest().getQueryParams().get("combinator").values().get(0), is("0"));
    }

    @Test
    public void shouldNotEnablePaymentOnDeliveryForDsbsToBrandedPickup() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, SwitchWithWhitelist.enabledForAll());
        checkouterProperties.setEnableDbsThroughMarketBrandedPickup(true);
        checkouterProperties.setForcePostpaid(CheckouterProperties.ForcePostpaid.FORCE);

        var parameters = WhiteParametersProvider.dbsPickupOrderWithCombinatorParameters(po -> {
            po.setPaymentMethods(Set.of(PaymentMethod.YANDEX.name()));
            po.setMarketBranded(true);
        });
        parameters.getPushApiDeliveryResponses().forEach(r -> r.setPaymentOptions(Set.of(PaymentMethod.YANDEX)));
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        Assertions.assertThrows(AssertionError.class, () -> orderCreateHelper.createOrder(parameters));
    }

    @Test
    public void shouldAllowDsbsDeliveryToBrandedPickup() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(BlueParametersProvider.PICKUP_SERVICE_ID)
                                .build()
                )
                .withDeliveryPartnerType(SHOP)
                .buildParameters();
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getPickup()
                .forEach(p -> {
                    p.setMarketBranded(true);
                    p.setPartnerType("regular");
                });
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.getDelivery().isMarketBranded());
        assertEquals(SHOP, order.getDelivery().getDeliveryPartnerType());
    }
}
