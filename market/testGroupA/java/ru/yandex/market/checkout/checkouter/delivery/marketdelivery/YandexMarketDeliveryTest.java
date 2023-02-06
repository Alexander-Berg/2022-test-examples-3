package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.auth.AuthService;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.controllers.oms.OrderController;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryUtils;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.shop.PrescriptionManagementSystem;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryResultProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.util.report.generators.ActualDeliveryJsonGenerator;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryOffer;
import ru.yandex.market.common.report.model.DeliveryPromo;
import ru.yandex.market.common.report.model.DeliveryPromoType;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.specs.Specs;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.collections.CollectionUtils.first;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.WrongDeliveryOption.CURRENT_DELIVERY_OPTION_EXPIRED;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.DELIVERY_PRICE;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.defaultActualDelivery;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.DROPOFF_PARTNER_WAREHOUSE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_ONLY_SELF_EXPORT_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;
import static ru.yandex.market.checkout.test.providers.DeliveryResultProvider.getActualDeliveryResult;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.DEFAULT_WARE_MD5;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;


/**
 * @author mmetlov
 */
public class YandexMarketDeliveryTest extends AbstractWebTestBase {

    public static final long ANOTHER_MOCK_DELIVERY_SERVICE_ID = 1005011L;
    private static final String DATE_2040_07_28T10_15_30 = "2040-07-28T10:15:30Z";
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private OrderHistoryEventsTestHelper historyEventsTestHelper;
    @Autowired
    private AuthService authService;
    @Autowired
    private OrderController orderController;

    @BeforeEach
    public void createOrder() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    /**
     * https://testpalm.yandex-team.ru/testcase/checkouter-18
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-18.
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @Tag(Tags.MARKET_DELIVERY)
    @DisplayName("Можно создать заказ МарДо")
    @Test
    public void shouldCreateOrderToYandexMarketDelivery() throws Exception {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID);
        assertThat(order.getId(), notNullValue());

        assertFalse(order.getDelivery().getParcels().isEmpty());
        assertNotNull(order.getDelivery().getParcels().get(0).getShopShipmentId());

        ParcelItem shipmentItem = order.getDelivery().getParcels().get(0).getParcelItems().get(0);
        assertEquals(first(order.getItems()).getId(), shipmentItem.getItemId());
        assertEquals(1, (int) shipmentItem.getCount());

        Parcel parcel = order.getDelivery().getShipment();
        assertEquals(ParcelStatus.NEW, parcel.getStatus());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверка доставки без ресептурки")
    @Test
    public void deliveryOrderWithoutPrescription() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Order order = orderCreateHelper.createMultiOrder(parameters).getOrders().get(0);
        assertNotNull(order.getId());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверка попытки доставки заказа с признаком рецептурки")
    @Test
    public void deliveryOrderWithPrescription() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        OrderItem item = parameters.getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Должен быть хотя бы один OrderItem"));
        item.setMedicalSpecsInternal(
                Specs.fromSpecValues(Set.of("psychotropic", "baa", "medicine", "prescription")));

        // В идеале нужно проверить, что отсеклись опции доставки type = DELIVERY,
        // но нет возможности отследить их в результирующем ордере.
        // Поэтому ожидаем, что если отсекётся deliveryOption = DELIVERY и при наличии delivery.type = DELIVERY
        // мы получим Exception
        assertThrows(RuntimeException.class, () -> orderCreateHelper.createOrder(parameters));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверка доставки заказа с признаком рецептурки при наличии интеграции с Медикатой")
    @Test
    @Disabled
    public void deliveryPrescriptionWithPrescriptionManagementSystem() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        // Нужно прописать в магазин признак интеграции с сервисом обработки электронного рецепта
        Map<Long, ShopMetaData> shopMap = parameters.getShopMetaData();
        ShopMetaData newMeta = ShopMetaDataBuilder.createCopy(
                        shopMap.get(parameters.getOrder().getShopId()))
                .withPrescriptionManagementSystem(PrescriptionManagementSystem.MEDICATA)
                .build();
        shopMap.put(parameters.getOrder().getShopId(), newMeta);

        OrderItem orderItem = parameters.getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Должен быть хотя бы один OrderItem"));
        orderItem.setMedicalSpecsInternal(Specs.fromSpecValues(Set.of("prescription")));

        Consumer<MultiCart> betweenCartAndCheckoutHook = multiCart -> {
            multiCart.getBuyer().setUserEsiaToken("user_esia_token");
            multiCart.getCarts().forEach(order -> {
                order.setBuyer(multiCart.getBuyer());
                order.getItems().forEach(item -> item.setPrescriptionGuids(Set.of("guid_1", "guid_2")));
            });
        };
        // В идеале нужно проверить, что не отсеклись опции доставки type = DELIVERY,
        // но нет возможности отследить их в результирующем ордере.
        // Поэтому рассчитываем на то, что если отсекётся deliveryOption = DELIVERY и при наличии
        // delivery.type = DELIVERY мы получим Exception.
        // Ожидаем, что отсечения не произойдёт, т.к. магазин имеет возможность доставки рецептурки
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, betweenCartAndCheckoutHook);
        Order order = multiOrder.getOrders().get(0);

        orderController.updateOrderStatus(order.getId(), ClientRole.SYSTEM, order.getUserClientInfo().getId(),
                null, 774L, order.getBusinessId(),
                OrderStatus.DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED, List.of(BLUE), null);

        Order resultOrder = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertTrue(resultOrder.getItems().stream()
                .allMatch(item -> Optional.ofNullable(item.getPrescriptionGuids())
                        .orElse(new HashSet<>())
                        .size() > 0));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Можно создать заказ с оплатой CARD_ON_DELIVERY, хотя магазин не отдает такой способ в PushAPI")
    @Test
    @Disabled // TODO перенести в кроссдок/дропшип
    public void shouldCreateCardOnDeliveryYandexMarketDeliveryWithPaidByCashAndCommissionPercentageAndFz54Data() {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.GREEN); // Green must die
        parameters.configureMultiCart(multiCart -> {
            multiCart.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
            multiCart.setPaymentType(PaymentType.POSTPAID);
        });
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        parameters.getPushApiDeliveryResponses().forEach(dr -> dr.getPaymentOptions().remove(PaymentMethod
                .CARD_ON_DELIVERY));
        parameters.getBuiltMultiCart().getCarts().stream().forEach(o -> {
            o.setDelivery(new Delivery(o.getDelivery().getRegionId()));
        });

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getPaymentOptions(), containsInAnyOrder(PaymentMethod.YANDEX,
                PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));

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
    @DisplayName("Не подмешиваем предоплату в МарДо опции если она отключена")
    @Test
    @Disabled // TODO перенести в кроссдок/дропшип ???
    public void shouldNotAddYandexMarketDeliveryWhenPrepaidOff() {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.GREEN); // Green must die
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        parameters.addShopMetaData(OrderProvider.SHOP_ID, ShopSettingsHelper.getPostpayMeta());

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertNotNull(cart.getCarts());
        assertThat(cart.getCarts(), hasSize(1));

        // в службе с НПП только POSTPAID
        List<Delivery> marDoOptions = cart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(it -> it.getDeliveryServiceId().equals(MOCK_DELIVERY_SERVICE_ID))
                .collect(Collectors.toList());

        assertThat(marDoOptions, containsInAnyOrder(
                allOf(
                        hasProperty("paymentOptions", containsInAnyOrder(PaymentMethod.CASH_ON_DELIVERY)),
                        hasProperty("outlets", contains(hasProperty("id", is(12312301L))))),
                hasProperty("paymentOptions", containsInAnyOrder(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod
                        .CARD_ON_DELIVERY)),
                hasProperty("paymentOptions", containsInAnyOrder(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod
                        .CARD_ON_DELIVERY))
        ));

        // нет опции для службы без НПП
        assertThat(cart.getCarts().get(0).getDeliveryOptions(),
                not(contains(
                        allOf(hasProperty("deliveryServiceId", is(MOCK_INTAKE_DELIVERY_SERVICE_ID)),
                                hasProperty("deliveryPartnerType", is(YANDEX_MARKET))))));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Можно создать заказ в курьерку МарДо")
    @Test
    public void shouldCreateDeliveryOrderToYandexMarketDelivery() throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();
        assertThat(order.getId(), notNullValue());

        assertNotNull(order.getDelivery().getShipment());
        assertNotNull(order.getDelivery().getShipment().getShopShipmentId());

        ParcelItem shipmentItem = order.getDelivery().getParcels().get(0).getParcelItems().get(0);
        assertEquals(first(order.getItems()).getId(), shipmentItem.getItemId());
        assertEquals(1, (int) shipmentItem.getCount());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Не подмешиваем курьерку МарДо если опции из репорта c partnerType != market_delivery")
    @Test
    public void shouldNotCreateDeliveryOrderToYandexMarketDeliveryWithWrongPartnerType() {
        LocalDeliveryOption localDeliveryOption = new LocalDeliveryOption();
        localDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        localDeliveryOption.setDayFrom(0);
        localDeliveryOption.setDayTo(2);
        localDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        localDeliveryOption.setPrice(new BigDecimal("100"));
        localDeliveryOption.setPartnerType(DeliveryOptionPartnerType.REGULAR.getReportName());
        localDeliveryOption.setShipmentDay(1);


        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(BLUE)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withPartnerInterface(false)
                .buildParameters();
        Delivery delivery = new Delivery();
        delivery.setRegionId(DeliveryProvider.REGION_ID);
        parameters.getOrder().setDelivery(delivery);
        FeedOfferId feedOfferId = parameters.getOrder().getItems().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No items in default order"))
                .getFeedOfferId();
        parameters.getReportParameters().setLocalDeliveryOptions(
                ImmutableMap.of(feedOfferId, singletonList(localDeliveryOption))
        );

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertNotNull(cart.getCarts());
        assertThat(cart.getCarts(), hasSize(1));

        Order order = cart.getCarts().get(0);
        assertThat(order.getDeliveryOptions(), not(contains(allOf(
                hasProperty("type", is(DeliveryType.DELIVERY)),
                hasProperty("deliveryPartnerType", is(YANDEX_MARKET))))));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Не подмешиваем курьерку МарДо если в опции из репорта нет даты отгрузки")
    @Test
    public void shouldNotCreateDeliveryOrderToYandexMarketDeliveryWithShipmentDaysNull() {
        LocalDeliveryOption localDeliveryOption = new LocalDeliveryOption();
        localDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        localDeliveryOption.setDayFrom(0);
        localDeliveryOption.setDayTo(2);
        localDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        localDeliveryOption.setPrice(new BigDecimal("100"));
        localDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        localDeliveryOption.setShipmentDay(null);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withPartnerInterface(false)
                .buildParameters();
        Delivery delivery = new Delivery();
        delivery.setRegionId(DeliveryProvider.REGION_ID);
        parameters.getOrder().setDelivery(delivery);
        FeedOfferId feedOfferId = parameters.getOrder().getItems().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No items in default order"))
                .getFeedOfferId();
        parameters.getReportParameters().setLocalDeliveryOptions(
                ImmutableMap.of(feedOfferId, singletonList(localDeliveryOption))
        );

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertNotNull(cart.getCarts());
        assertThat(cart.getCarts(), hasSize(1));

        Order order = cart.getCarts().get(0);
        assertThat(order.getDeliveryOptions(), not(contains(allOf(
                hasProperty("type", is(DeliveryType.DELIVERY)),
                hasProperty("deliveryPartnerType", is(YANDEX_MARKET))))));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Не подмешиваем курьерку МарДо если в опции из репорта нет dayFrom или dayTo")
    @Test
    public void shouldNotCreateDeliveryOrderToYandexMarketDeliveryWithDayToNull() {
        LocalDeliveryOption localDeliveryOption = new LocalDeliveryOption();
        localDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        localDeliveryOption.setDayFrom(0);
        localDeliveryOption.setDayTo(null);
        localDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        localDeliveryOption.setPrice(new BigDecimal("100"));
        localDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        localDeliveryOption.setShipmentDay(1);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withPartnerInterface(false)
                .buildParameters();
        Delivery delivery = new Delivery();
        delivery.setRegionId(DeliveryProvider.REGION_ID);
        parameters.getOrder().setDelivery(delivery);
        FeedOfferId feedOfferId = parameters.getOrder().getItems().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No items in default order"))
                .getFeedOfferId();
        parameters.getReportParameters().setLocalDeliveryOptions(
                ImmutableMap.of(feedOfferId, singletonList(localDeliveryOption))
        );

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertNotNull(cart.getCarts());
        assertThat(cart.getCarts(), hasSize(1));

        Order order = cart.getCarts().get(0);
        assertThat(order.getDeliveryOptions(), not(contains(allOf(
                hasProperty("type", is(DeliveryType.DELIVERY)),
                hasProperty("deliveryPartnerType", is(YANDEX_MARKET))))));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ в курьерку с недостаточной суммой для бесплатной доставки")
    @Test
    public void blueDeliveryOrderWithoutFreeDelivery() {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(shipmentDays)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addDelivery(MOCK_DELIVERY_SERVICE_ID, shipmentDays)
                                .build()
                )
                .buildParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getDelivery().getBuyerPrice().intValue(), greaterThan(0));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ в курьерку с бесплатной доставкой")
    @Test
    public void blueDeliveryOrderWithFreeDelivery() {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(shipmentDays)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addDelivery(MOCK_DELIVERY_SERVICE_ID, shipmentDays)
                                .withFreeDelivery()
                                .build()
                )
                .buildParameters();
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        multiCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(option -> option.getDeliveryPartnerType() == YANDEX_MARKET)
                .forEach(option -> assertThat(option.getBuyerPrice(), is(BigDecimal.ZERO)));

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getDelivery().getBuyerPrice(), is(BigDecimal.ZERO));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ в курьерку c опцией из actual_delivery")
    @Test
    public void blueDeliveryWithActualDeliveryOption() {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(shipmentDays)
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddress());
        //убираем подложенную опцию под айтемом
        parameters.getReportParameters().setLocalDeliveryOptions(Collections.emptyMap());
        parameters.setFreeDelivery(false);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
    }

    private List<ActualDeliveryOption> makeFree(Parameters parameters) {
        List<ActualDeliveryOption> localDeliveryOptions = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        ).getDelivery();
        localDeliveryOptions.forEach(ldo -> {
            BigDecimal oldPrice = ldo.getPrice();
            DeliveryPromo deliveryPromo = new DeliveryPromo();
            deliveryPromo.setOldPrice(oldPrice);
            deliveryPromo.setDiscountType(DeliveryPromoType.PRIME);

            ldo.setPrice(BigDecimal.ZERO);
            ldo.setDiscount(deliveryPromo);
        });
        parameters.setUserHasPrime(true);
        return localDeliveryOptions;
    }

    /**
     * пока поддержки нескольких коробок нет в репорте и чекаутере, в случае если придет более одной коробки выбираем 1ю
     */
    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Синий заказ в курьерку c опцией из actual_delivery с несколькими коробками")
    @Test
    public void blueDeliveryWithActualDeliveryOptionMultipleParcels() {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(shipmentDays)
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddress());
        //подкладываем опции в actual_delivery
        ActualDeliveryResult actualDeliveryResult = getActualDeliveryResult();
        actualDeliveryResult.setDelivery(
                mapToActualDeliveryOptions(
                        parameters.getReportParameters().getLocalDeliveryOptions().values().stream().findFirst().get()
                )
        );
        ActualDeliveryOption anotherLocalDeliveryOption = new ActualDeliveryOption();
        anotherLocalDeliveryOption.setDeliveryServiceId(MOCK_ONLY_SELF_EXPORT_DELIVERY_SERVICE_ID);
        anotherLocalDeliveryOption.setDayFrom(0);
        anotherLocalDeliveryOption.setDayTo(2);
        anotherLocalDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        anotherLocalDeliveryOption.setPrice(new BigDecimal("100"));
        anotherLocalDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        anotherLocalDeliveryOption.setShipmentDay(1);
        ActualDeliveryResult anotherActualDeliveryResult = getActualDeliveryResult();
        anotherActualDeliveryResult.setDelivery(singletonList(anotherLocalDeliveryOption));
        ActualDelivery actualDelivery = defaultActualDelivery();
        actualDelivery.setResults(Arrays.asList(actualDeliveryResult, anotherActualDeliveryResult));
        parameters.getReportParameters().setActualDelivery(actualDelivery);
        //убираем подложенную опцию под айтемом
        parameters.getReportParameters().setLocalDeliveryOptions(Collections.emptyMap());
        parameters.setFreeDelivery(false);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getDelivery().getDeliveryServiceId(), is(MOCK_DELIVERY_SERVICE_ID));
    }

    private List<ActualDeliveryOption> mapToActualDeliveryOptions(List<LocalDeliveryOption>
                                                                          localDeliveryOptions) {
        return localDeliveryOptions.stream()
                .map(localDeliveryOption -> {
                    ActualDeliveryOption actualDeliveryOption = new ActualDeliveryOption();
                    actualDeliveryOption.setPrice(localDeliveryOption.getPrice());
                    actualDeliveryOption.setDayFrom(localDeliveryOption.getDayFrom());
                    actualDeliveryOption.setDayTo(localDeliveryOption.getDayTo());
                    actualDeliveryOption.setDeliveryServiceId(localDeliveryOption.getDeliveryServiceId());
                    actualDeliveryOption.setPaymentMethods(localDeliveryOption.getPaymentMethods());
                    actualDeliveryOption.setPartnerType(localDeliveryOption.getPartnerType());
                    actualDeliveryOption.setShipmentDay(localDeliveryOption.getShipmentDay());
                    actualDeliveryOption.setShipmentDate(localDeliveryOption.getShipmentDate());
                    actualDeliveryOption.setDiscount(localDeliveryOption.getDiscount());
                    actualDeliveryOption.setCost(localDeliveryOption.getCost());
                    actualDeliveryOption.setCurrency(localDeliveryOption.getCurrency());
                    actualDeliveryOption.setOrderBefore(localDeliveryOption.getOrderBefore());
                    actualDeliveryOption.setOrderBeforeShopTz(localDeliveryOption.getOrderBeforeShopTz());
                    return actualDeliveryOption;
                })
                .collect(Collectors.toList());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("При изменении посылки не должен перетираться buyerAddress")
    @Test
    public void shouldKeepBuyerAddressOnDeliveryUpdate() throws Exception {
        freezeTimeAt("2027-07-06T10:15:30Z");
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withShipmentDay(1)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        assertThat(order.getId(), notNullValue());
        Address buyerAddress = order.getDelivery().getBuyerAddress();

        Parcel orderShipment = new Parcel();
        orderShipment.setWeight(10L);
        orderShipment.setWidth(10L);
        orderShipment.setHeight(10L);
        orderShipment.setDepth(10L);
        Delivery dimensionsReq = new Delivery();
        dimensionsReq.setParcels(singletonList(orderShipment));
        order = orderDeliveryHelper.updateOrderDelivery(order.getId(), dimensionsReq);
        assertThat(order.getDelivery().getBuyerAddress(), notNullValue());
        assertThat(order.getDelivery().getBuyerAddress(), is(buyerAddress));

        order = orderService.getOrder(order.getId());
        assertThat(order.getDelivery().getBuyerAddress(), notNullValue());
        assertThat(order.getDelivery().getBuyerAddress(), is(buyerAddress));
    }

    @Test
    public void shouldCreateShipmentToWarehouseId() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems().stream()
                .forEach(i -> i.setCargoTypes(Sets.newHashSet(1, 2, 3)));

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getItems().stream().findFirst().get().getCargoTypes(),
                containsInAnyOrder(is(1), is(2), is(3)));
    }

    @Test
    public void shouldSetDefaultInletIdForUnknownWarehouseId() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems().stream().forEach(i ->
                i.setCargoTypes(Sets.newHashSet(1, 2, 3)));

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(300501999));

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getItems().stream().findFirst().get().getCargoTypes(),
                containsInAnyOrder(is(1), is(2), is(3)));
    }

    @Test
    public void shouldntAllowOffersFromDifferentWarehousesInOneOrder() {
        Order orderSetup = OrderProvider.getBlueOrder();
        orderSetup.getItems().forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));
        List<OrderItem> items = new ArrayList<>(orderSetup.getItems());

        OrderItem orderItem = OrderItemProvider.getAnotherOrderItem();
        orderItem.setWarehouseId(400501);

        items.add(orderItem);
        orderSetup.setItems(items);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .withOrder(orderSetup)
                .buildParameters();

        parameters.getReportParameters().getOrder().setItems(items);

        parameters.setCheckCartErrors(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getValidationErrors(),
                contains(hasProperty("code", is("DIFFERENT_WAREHOUSES_ERROR"))));
    }

    @Test
    public void shouldSaveCargoTypesForEveryItem() {
        Order orderSetup = OrderProvider.getBlueOrder();
        List<OrderItem> items = new ArrayList<>(orderSetup.getItems());
        orderSetup.getItems().forEach(oi -> oi.setCargoTypes(Sets.newHashSet(1, 2)));

        OrderItem orderItem = OrderItemProvider.getAnotherOrderItem();
        orderItem.setCargoTypes(Sets.newHashSet(3, 4));
        orderItem.setCount(1);

        items.add(orderItem);
        orderSetup.setItems(items);
        orderSetup.getItems().forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .withOrder(orderSetup)
                .buildParameters();

        parameters.getReportParameters().getOrder().setItems(items);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getItems(), containsInAnyOrder(
                hasProperty("cargoTypes", containsInAnyOrder(is(1), is(2))),
                hasProperty("cargoTypes", containsInAnyOrder(is(3), is(4)))
        ));
    }

    @Test
    public void shouldRequestActualDeliverySecondTimeAndTakePreciseShipmentDate() throws Exception {
        // Добавим несколько курьерских опций в actual_delivery
        ActualDeliveryOption secondDeliveryOption = new ActualDeliveryOption();
        secondDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        secondDeliveryOption.setDayFrom(3);
        secondDeliveryOption.setDayTo(3);
        secondDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        secondDeliveryOption.setPrice(DELIVERY_PRICE);
        secondDeliveryOption.setCurrency(Currency.RUR);
        secondDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        secondDeliveryOption.setShipmentDay(1);

        ActualDeliveryOption thirdDeliveryOption = new ActualDeliveryOption();
        thirdDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        thirdDeliveryOption.setDayFrom(4);
        thirdDeliveryOption.setDayTo(5);
        thirdDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        thirdDeliveryOption.setPrice(DELIVERY_PRICE);
        thirdDeliveryOption.setCurrency(Currency.RUR);
        thirdDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        thirdDeliveryOption.setShipmentDay(1);

        ActualDeliveryProvider.ActualDeliveryBuilder actualDeliveryBuilder = ActualDeliveryProvider
                .builder()
                .addPost(1)
                .addPickup(MOCK_DELIVERY_SERVICE_ID, 1)
                .addDelivery(MOCK_DELIVERY_SERVICE_ID, 1)
                .addDelivery(secondDeliveryOption)
                .addDelivery(thirdDeliveryOption);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .withActualDelivery(actualDeliveryBuilder.build())
                .buildParameters();

        parameters.setFromDate(DeliveryDates.daysOffsetToDate(getClock(), 4));

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));

        //Дополнительный мок на второй запрос actual_delivery
        parameters.getReportParameters().setConfigurePreciseActualDelivery(false);
        ActualDeliveryOption preciseDeliveryOption = new ActualDeliveryOption();
        preciseDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        preciseDeliveryOption.setDayFrom(4);
        preciseDeliveryOption.setDayTo(5);
        preciseDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        preciseDeliveryOption.setPrice(DELIVERY_PRICE);
        preciseDeliveryOption.setCurrency(Currency.RUR);
        preciseDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        preciseDeliveryOption.setShipmentDay(3);

        ActualDeliveryProvider.ActualDeliveryBuilder preciseActualDeliveryBuilder =
                ActualDeliveryProvider.builder()
                        .addDelivery(preciseDeliveryOption);

        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.ACTUAL_DELIVERY.getId()))
                .withQueryParam("preferable-courier-delivery-day", equalTo("5"))
                .withQueryParam("preferable-courier-delivery-service", equalTo(MOCK_DELIVERY_SERVICE_ID.toString()));
        JsonObject object = new JsonObject();
        ActualDeliveryJsonGenerator actualDeliveryJsonGenerator = new ActualDeliveryJsonGenerator();
        object = actualDeliveryJsonGenerator.patch(
                object,
                new ReportGeneratorParameters(
                        null,
                        preciseActualDeliveryBuilder.build()
                )
        );
        reportMock.stubFor(builder
                .willReturn(new ResponseDefinitionBuilder().withBody(object.toString()))
        );

        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(LocalDate.ofInstant(getClock().instant(), getClock().getZone()).plusDays(3),
                order.getDelivery().getShipment().getShipmentDate());
    }

    @Test
    public void shouldSaveDeliveryParametersInParcel() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));

        Order order = orderCreateHelper.createOrder(parameters);

        LocalDate expectedShipmentDate = getClock().instant().plus(1, ChronoUnit.DAYS)
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Instant expectedPackagingTime = expectedShipmentDate.atTime(23, 0)
                .atZone(ZoneId.systemDefault()).toInstant();
        Parcel parcel = order.getDelivery().getParcels().get(0);
        assertEquals(expectedShipmentDate, parcel.getShipmentDate());
        assertEquals(expectedPackagingTime, parcel.getPackagingTime());
    }

    @Test
    void shouldSaveShipmentDatesInParcel() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHIPMENT_DATE_BY_SUPPLIER_ENABLED, true);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .withShipmentDateTimeBySupplier(ZonedDateTime.parse("2020-04-04T12:00:00-05:00"))
                .withReceptionDateTimeByWarehouse(ZonedDateTime.parse("2020-04-04T16:00:00-05:00"))
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));

        Order order = orderCreateHelper.createOrder(parameters);
        Parcel parcel = CollectionUtils.extractSingleton(order.getDelivery().getParcels());

        OrderHistoryEvent lastEvent = historyEventsTestHelper.getAllEvents(order.getId()).get(0);
        Parcel parcelHistory = CollectionUtils.extractSingleton(lastEvent.getOrderAfter().getDelivery().getParcels());

        // Проверяем, что поля сохраняются в заказе и в истории
        List.of(parcel, parcelHistory).forEach(p -> {
            assertEquals(LocalDateTime.parse("2020-04-04T12:00:00"), p.getShipmentDateTimeBySupplier());
            assertEquals(LocalDateTime.parse("2020-04-04T16:00:00"), p.getReceptionDateTimeByWarehouse());
        });

        // Эти поля вычисляются в логике актуализации (правильнее было бы настроить мок репорта)
        LocalDateTime expectedShipmentBySupplier = parcel.getParcelItems().get(0).getShipmentDateTimeBySupplier();
        LocalDateTime expectedReceptionByWarehouse = parcel.getParcelItems().get(0).getReceptionDateTimeByWarehouse();

        // Проверяем, что поля сохраняются в заказе и в истории
        Stream.concat(parcel.getParcelItems().stream(), parcelHistory.getParcelItems().stream()).forEach(item -> {
            assertEquals(expectedShipmentBySupplier, item.getShipmentDateTimeBySupplier());
            assertEquals(expectedReceptionByWarehouse, item.getReceptionDateTimeByWarehouse());
        });
    }

    @Test
    public void shouldSaveDeliveryParametersInParcelWhenSupplierShipmentDateTimeExists() {
        Instant expectedSupplierShipmentDateTime = Instant.parse("2019-10-05T12:00:00Z");
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withSupplierShipmentDateTime(expectedSupplierShipmentDateTime)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));

        Order order = orderCreateHelper.createOrder(parameters);

        LocalDate expectedShipmentDate = getClock().instant().plus(1, ChronoUnit.DAYS)
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Instant expectedPackagingTime = expectedShipmentDate.atTime(23, 00)
                .atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(expectedShipmentDate, order.getDelivery().getParcels().get(0).getShipmentDate());
        assertEquals(expectedPackagingTime, order.getDelivery().getParcels().get(0).getPackagingTime());
    }

    @Test
    public void ignoreDeliveryServiceIdChangeBetweenCartAndCheckout() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        ActualDelivery actualDelivery = parameters.getReportParameters().getActualDelivery();
        actualDelivery.getResults().get(0).getDelivery()
                .forEach(o -> o.setDeliveryServiceId(ANOTHER_MOCK_DELIVERY_SERVICE_ID));
        orderCreateHelper.initializeMock(parameters);
        Order order = orderCreateHelper.checkout(multiCart, parameters).getOrders().get(0);

        assertEquals(ANOTHER_MOCK_DELIVERY_SERVICE_ID, order.getDelivery().getDeliveryServiceId());
    }

    @Test
    public void shouldntFailWithoutDeliveryServiceIdOnCart() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();

        ActualDelivery actualDelivery = parameters.getReportParameters().getActualDelivery();
        actualDelivery.getResults().get(0).getDelivery()
                .forEach(o -> o.setDeliveryServiceId(null));
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        actualDelivery.getResults().get(0).getDelivery()
                .forEach(o -> o.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID));
        orderCreateHelper.initializeMock(parameters);
        Order order = orderCreateHelper.checkout(multiCart, parameters).getOrders().get(0);

        assertEquals(MOCK_DELIVERY_SERVICE_ID, order.getDelivery().getDeliveryServiceId());
    }

    @Test
    public void shouldCreateDeliveryOrderViaCombinatorFlow() {
        freezeTimeAt(DATE_2040_07_28T10_15_30);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setMinifyOutlets(true);

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);
        JSONAssert.assertEquals(
                DeliveryResultProvider.ROUTE,
                order.getDelivery().getParcels().get(0).getRoute().toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );

        ServeEvent deliveryRouteServeEvent = getDeliveryRouteServeEvent();
        assertEquals("courier", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-type").values()));
        assertEquals(Long.toString(DeliveryProvider.REGION_ID), getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("rids").values()));
        assertEquals("20400728.1000-20400728.1800", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-interval").values()));
        assertNull(deliveryRouteServeEvent.getRequest().getQueryParams().get("point_id"), "point_id");
        assertNull(deliveryRouteServeEvent.getRequest().getQueryParams().get("post-index"), "post-index");
        assertEquals("prepayment_card", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("payments").values()));
        assertEquals(DEFAULT_WARE_MD5 + ":1",
                getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("offers-list").values()));
        assertEquals("1",
                getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("show-preorder").values()));
        assertEquals("ordinary",
                getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-subtype").values()));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @Test
    public void shouldNotCallDeliveryRouteForPreorder() throws Exception {
        reportMock.resetRequests();

        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist<>(true,
                Collections.singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        parameters.getOrder().setPreorder(true);
        parameters.setStockStorageMockType(MockConfiguration.StockStorageMockType.PREORDER_OK);

        OrderItem orderItem = parameters.getItems().iterator().next();
        orderItem.setPrice(BigDecimal.valueOf(1000));
        orderItem.setBuyerPrice(BigDecimal.valueOf(1000));
        FoundOfferBuilder from = FoundOfferBuilder.createFrom(orderItem).preorder(true);
        parameters.getReportParameters().setOffers(List.of(from.build()));
        parameters.getReportParameters().getActualDelivery().getResults().get(0)
                .setDelivery(singletonList(buildDeliveryOption()));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        multiCart.getCarts().get(0).setPreorder(true);
        reportMock.resetRequests();
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);

        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();


        Collection<ServeEvent> actualDeliveryCalls = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId())
                )
                .collect(Collectors.toList());

        assertFalse(hasDeliveryRouteBeenCalled());
        assertEquals(1, actualDeliveryCalls.size());
        assertNotNull(multiOrder.getCarts().get(0).getDelivery().getShipmentForJson().getShipmentDate());
    }

    private ActualDeliveryOption buildDeliveryOption() {
        ActualDeliveryOption actualDeliveryOption = new ActualDeliveryOption();
        actualDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        actualDeliveryOption.setCost(BigDecimal.TEN);
        actualDeliveryOption.setDayFrom(0);
        actualDeliveryOption.setDayTo(2);
        actualDeliveryOption.setCurrency(Currency.RUR);
        actualDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        return actualDeliveryOption;
    }

    @Test
    public void shouldCreateDeliveryOrderViaCombinatorFlowWithNegativeShipmentDay() {
        freezeTimeAt(DATE_2040_07_28T10_15_30);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setMinifyOutlets(true);
        parameters.getReportParameters().getDeliveryRoute().getResults().get(0).getOption().setShipmentDay(-2);

        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(LocalDate.of(2040, 07, 26),
                order.getDelivery().getParcels().get(0).getShipmentDate());
    }

    @Test
    public void shouldCreatePostOrderViaCombinatorFlow() {
        freezeTimeAt(DATE_2040_07_28T10_15_30);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.POST)
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
        assertEquals("post", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-type").values()));
        assertEquals(Long.toString(DeliveryProvider.REGION_ID), getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("rids").values()));
        assertEquals("20400728.0000-20400730.2359", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-interval").values()));
        assertEquals(DeliveryUtils.getPostcode(order, personalDataService::getPersAddress).get(), getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("post-index").values()));
        assertNull(deliveryRouteServeEvent.getRequest().getQueryParams().get("point_id"), "point_id");
        assertEquals("prepayment_card", getOnlyElement(
                deliveryRouteServeEvent.getRequest().getQueryParams().get("payments").values()));
        assertEquals(DEFAULT_WARE_MD5 + ":1",
                getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("offers-list").values()));
    }

    @Test
    public void combinatorFlowShouldBeEnabled() throws Exception {
        Set<String> whitelist = Set.of("A=1", "B=0", "C=1");
        Experiments experiments1 = Experiments.fromString("A=1; B=0; C=1");
        Experiments experiments2 = Experiments.fromString("A=1; B=0; C=1; X=1");

        testDeliveryRouteCall(whitelist, experiments1, true);
        testDeliveryRouteCall(whitelist, experiments2, true);
    }

    @Test
    public void combinatorFlowShouldNotBeEnabled() throws Exception {
        Set<String> whitelist = Set.of("A=1", "B=0", "C=1");
        Experiments experiments = Experiments.fromString("A=1; C=1");

        testDeliveryRouteCall(whitelist, experiments, false);
    }

    private void testDeliveryRouteCall(
            Set<String> whitelist,
            Experiments experiments,
            boolean deliveryRouteShouldBeCalled
    ) throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                whitelist));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(deliveryRouteShouldBeCalled)
                .buildParameters();
        parameters.setExperiments(experiments);
        parameters.setMinifyOutlets(true);

        reportMock.resetRequests();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        orderCreateHelper.checkout(multiCart, parameters);

        assertEquals(deliveryRouteShouldBeCalled, hasDeliveryRouteBeenCalled());
    }

    private boolean hasDeliveryRouteBeenCalled() {
        return reportMock.getServeEvents().getRequests().stream()
                .anyMatch(r -> r.getRequest().getQueryParams().get("place").isSingleValued()
                        && r.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route"));
    }

    @Test
    public void testPreciseRegionPassedDuringDeliveryRouteSearch() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddressWithPreciseRegionId());

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        reportMock.resetRequests();
        orderCreateHelper.checkout(multiCart, parameters);

        long preciseRegionId = parameters.getOrder().getDelivery().getBuyerAddress().getPreciseRegionId();
        boolean preciseRegionPassed = reportMock.getServeEvents().getRequests().stream()
                .anyMatch(r -> r.getRequest().getQueryParams().get("place").isSingleValued()
                        && r.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route")
                        && r.getRequest().getQueryParams().get("rids").isSingleValued()
                        && r.getRequest().getQueryParams().get("rids")
                        .firstValue().equals(String.valueOf(preciseRegionId))
                );

        assertTrue(preciseRegionPassed);
    }

    @Test
    @Disabled
    public void testNotCombinatorOrderWhenMinifyOutletsDisabled() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);

        parameters.setMinifyOutlets(false);

        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.getDelivery().getParcels().get(0).getRoute().isNull());
    }

    @Test
    public void deliveryIdForcedDuringCart() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        parameters.setForceDeliveryId(111L);

        reportMock.resetAll();
        orderCreateHelper.cart(parameters);

        boolean deliveryIdForced = reportMock.getServeEvents().getRequests().stream()
                .anyMatch(r -> r.getRequest().getQueryParams().get("place").isSingleValued()
                        && r.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery")
                        && r.getRequest().getQueryParams().get("force-delivery-id").isSingleValued()
                        && r.getRequest().getQueryParams().get("force-delivery-id").firstValue().equals("111")
                );

        assertTrue(deliveryIdForced);
    }

    @Test
    public void deliveryIdForcedDuringCheckout() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        parameters.setForceDeliveryId(111L);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        reportMock.resetAll();
        orderCreateHelper.checkout(multiCart, parameters);

        boolean deliveryIdForced = reportMock.getServeEvents().getRequests().stream()
                .anyMatch(r -> r.getRequest().getQueryParams().get("place").isSingleValued()
                        && r.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route")
                        && r.getRequest().getQueryParams().get("force-delivery-id").isSingleValued()
                        && r.getRequest().getQueryParams().get("force-delivery-id").firstValue().equals("111")
                );

        assertTrue(deliveryIdForced);
    }

    @Test
    public void necessaryParametersPassedDuringCart() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);

        parameters.getReportParameters().getActualDelivery().getResults().stream()
                .map(ActualDeliveryResult::getDelivery)
                .flatMap(List::stream)
                .forEach(option -> option.setIsOnDemand(true));

        assertFalse(authService.isNoAuth(parameters.getBuyer().getUid()));
        reportMock.resetAll();
        orderCreateHelper.cart(parameters);

        List<ServeEvent> events = reportMock.getServeEvents().getRequests();
        ServeEvent actualDeliveryEvent = events.stream()
                .filter(r -> r.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery"))
                .findFirst()
                .orElseThrow();

        Map<String, QueryParameter> actualDeliveryQueryParams = actualDeliveryEvent.getRequest().getQueryParams();
        assertTrue(actualDeliveryQueryParams.get("gps").isSingleValued());
        assertEquals(actualDeliveryQueryParams.get("gps").firstValue(), "lat:48.218271;lon:46.135321");
        assertTrue(actualDeliveryQueryParams.get("logged-in").isSingleValued());
        assertEquals(actualDeliveryQueryParams.get("logged-in").firstValue(), "1");
    }

    @Test
    @Disabled //Для предзаказа не ходим в delivery_route
    public void badRequestDuringCombinatorOrderStatusTransitionWithExpiredDeliveryOptions() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.getReportParameters().getOrder().getItems()
                .forEach(i -> {
                    i.setCargoTypes(Sets.newHashSet(1, 2, 3));
                    i.setPreorder(true);
                });
        parameters.setStockStorageMockType(MockConfiguration.StockStorageMockType.PREORDER_OK);

        // Создаём комбинаторный заказ и переводим его в PENDING
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PENDING);

        // Говорим репорту не возвращать маршрут ввиду протухшей опции доставки
        parameters.getReportParameters().getDeliveryRoute().setResults(Collections.emptyList());
        parameters.getReportParameters().getDeliveryRoute().setCommonProblems(
                Collections.singletonList("COMBINATOR_ROUTE_UNAVAILABLE")
        );
        orderCreateHelper.initializeMock(parameters);

        // Ожидаем 400 ошибку при попытке перевести статус из PENDING в PROCESSING
        ResultActionsContainer container = new ResultActionsContainer()
                .andExpect(status().isBadRequest())
                .andDo(r ->
                        assertTrue(r.getResponse().getContentAsString().contains(CURRENT_DELIVERY_OPTION_EXPIRED))
                );
        orderStatusHelper.updateOrderStatus(
                order.getId(),
                ClientInfo.SYSTEM,
                OrderStatus.PROCESSING,
                null,
                container,
                null
        );
    }

    @Test
    public void failedDeliveryRouteDuringCombinatorOrderCheckoutWithExpiredDeliveryOptions() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        // Говорим репорту не возвращать маршрут ввиду протухшей опции доставки
        parameters.getReportParameters().getDeliveryRoute().setResults(Collections.emptyList());
        parameters.getReportParameters().getDeliveryRoute().setCommonProblems(
                Collections.singletonList("COMBINATOR_ROUTE_UNAVAILABLE")
        );
        orderCreateHelper.initializeMock(parameters);

        // Ожидаем ошибку DELIVERY_ROUTE_FAILED при создании заказа
        parameters.setUseErrorMatcher(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(true);
        parameters.setErrorMatcher(result ->
                assertTrue(result.getResponse().getContentAsString().contains("DELIVERY_ROUTE_FAILED"))
        );

        orderCreateHelper.checkout(multiCart, parameters);
    }

    private ServeEvent getDeliveryRouteServeEvent() {
        return reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && req.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route"))
                .findFirst().get();
    }

    @Test
    public void shouldCreateDropoffOrderViaCombinatorFlow() {
        freezeTimeAt(DATE_2040_07_28T10_15_30);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setExperiments("some_other_experiment=1");
        parameters.setMinifyOutlets(true);

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(DROPOFF_PARTNER_WAREHOUSE_ID.intValue()));

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);
        JSONAssert.assertEquals(
                DeliveryResultProvider.ROUTE,
                order.getDelivery().getParcels().get(0).getRoute().toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );

        assertTrue(order.getProperty(OrderPropertyType.EXPERIMENTS).contains(Constants.COMBINATOR_EXPERIMENT));
        assertTrue(order.getProperty(OrderPropertyType.EXPERIMENTS).contains("some_other_experiment=1"));

        assertTrue(reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && (req.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery")
                        || req.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route")))
                .allMatch(req -> req.getRequest().getQueryParams().get("rearr-factors").firstValue()
                        .contains(Constants.COMBINATOR_EXPERIMENT)));
    }

    @Test
    public void shouldCreateFakeDropoffOrderViaCombinatorFlow() {
        freezeTimeAt(DATE_2040_07_28T10_15_30);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setExperiments("some_other_experiment=1");
        parameters.setMinifyOutlets(true);
        parameters.setContext(Context.SANDBOX);

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(DROPOFF_PARTNER_WAREHOUSE_ID.intValue()));

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);
        JSONAssert.assertEquals(
                DeliveryResultProvider.ROUTE,
                order.getDelivery().getParcels().get(0).getRoute().toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );

        assertTrue(order.getProperty(OrderPropertyType.EXPERIMENTS).contains(Constants.COMBINATOR_EXPERIMENT));
        assertTrue(order.getProperty(OrderPropertyType.EXPERIMENTS).contains("some_other_experiment=1"));

        assertTrue(reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && (req.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery")
                        || req.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route")))
                .allMatch(req -> req.getRequest().getQueryParams().get("rearr-factors").firstValue()
                        .contains(Constants.COMBINATOR_EXPERIMENT)));

        ServeEvent deliveryRouteServeEvent = getDeliveryRouteServeEvent();
        assertEquals("1",
                getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("ignore-has-gone").values()));
        assertEquals(DEFAULT_WARE_MD5 + ":1;w:1.000;d:10x10x10;p:250;wh:400504;ff:1;ffWh:400504",
                getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("offers-list").values()));
    }

    @Test
    public void shouldSetOrderItemsFulfillmentWarehouseId() {
        freezeTimeAt(DATE_2040_07_28T10_15_30);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setMinifyOutlets(true);
        parameters.getReportParameters().getDeliveryRoute().getResults().get(0)
                .setOffers(Collections.singletonList(createOffer(123L)));

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(123L, order.getItems().stream().findFirst().get().getFulfilmentWarehouseId());
    }

    @Test
    public void shouldNotSetOrderItemsFulfillmentWarehouseIdIfNull() {
        freezeTimeAt(DATE_2040_07_28T10_15_30);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setMinifyOutlets(true);
        parameters.getReportParameters().getDeliveryRoute().getResults().get(0)
                .setOffers(Collections.singletonList(createOffer(null)));

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(300501L, order.getItems().stream().findFirst().get().getFulfilmentWarehouseId());
    }

    @Test
    public void shouldSendActualDeliveryRequestWithCombinatorParameterIsTrue() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.FORCE_COMBINATOR_CHECK, true);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        orderCreateHelper.cart(parameters);
        ServeEvent serveEvent = reportMock.getServeEvents().getRequests()
                .stream()
                .filter(r -> r.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery"))
                .findFirst()
                .get();
        assertThat(serveEvent.getRequest().getQueryParams().get("combinator").values().get(0), is("1"));
    }

    private DeliveryOffer createOffer(Long fulfillmentWarehouseId) {
        DeliveryOffer deliveryOffer = new DeliveryOffer();
        deliveryOffer.setMarketSku(123456789L);
        deliveryOffer.setSellerPrice(100600L);
        deliveryOffer.setCurrency(Currency.RUR);
        deliveryOffer.setFulfillmentWarehouseId(fulfillmentWarehouseId);
        return deliveryOffer;
    }
}
