package ru.yandex.market.api.user.order;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.OfferIdEncodingService;
import ru.yandex.market.api.domain.v2.ThumbnailSize;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.carter.domain.CartItem;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.matchers.OrderOutletMatcher;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.user.order.builders.MultiCartOrderBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartOrderDeliveryBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartOrderItemBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartTotalsBuilder;
import ru.yandex.market.api.user.order.builders.MultiOrderBuilder;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;
import ru.yandex.market.api.user.order.preorder.OrderOptionsResponse;
import ru.yandex.market.api.user.order.service.OrderItemService;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.credit.CreditInformation;
import ru.yandex.market.checkout.checkouter.credit.CreditOption;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCustomizer;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalAndDate;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsInfo;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsOption;
import ru.yandex.market.checkout.checkouter.installments.MonthlyPayment;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by fettsery on 17.07.18.
 */
@ActiveProfiles("OrderControllerTest")
public class OrderControllerTest extends BaseTest {

    private static final long SERVICE_ID = 1000;
    private static final Date SERVICE_DATE = new Date();
    private static final LocalTime SERVICE_FROM_TIME = LocalTime.of(8, 0);
    private static final LocalTime SERVICE_TO_TIME = LocalTime.of(18, 0);
    private static final String SERVICE_REQ_ID = "someReqId";
    private static final BigDecimal SERVICE_PRICE = BigDecimal.TEN;
    private static final OfferId OFFER_ID = new OfferId("newWareMd5_1", "feeshow1", "wareMd5_1");
    private static final Long[] OUTLET_IDS = {1L, 2L};
    private static final Collection<String> TEST_RGBS = Collections.singletonList("white");
    @Inject
    private CheckouterAPI checkouter;
    @Inject
    private ReportTestClient reportTestClient;
    @Inject
    private OfferIdEncodingService offerIdEncodingService;
    @Inject
    private OrderController controller;

    @Test
    public void testOrderOptionsWithOutlets() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        reportTestClient.getOutlets(Arrays.asList(OUTLET_IDS), "get-outlets.json");

        OrderOptionsRequest request = getOrderOptionsRequest();
        MultiOrder multiCart = getCheckouterResponse();

        when(
                checkouter.cart(
                        any(MultiOrder.class),
                        any(CartParameters.class)
                )
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                null,
                true,
                false,
                false,
                new GenericParamsBuilder()
                        .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );

        OutletDeliveryOption deliveryOption =
                (OutletDeliveryOption) orderOptions.getCart(774).getDeliveryOptions().get(0);

        assertThat(deliveryOption.getOutlets(),
                containsInAnyOrder(
                        OrderOutletMatcher.outlet(
                                OrderOutletMatcher.id(1),
                                OrderOutletMatcher.name("Пункт выдачи посылок 1")),
                        OrderOutletMatcher.outlet(
                                OrderOutletMatcher.id(2),
                                OrderOutletMatcher.name("Пункт выдачи посылок 2"))
                )
        );
    }

    @Test
    public void testOrderOptionsWithoutOutlets() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        OrderOptionsRequest request = getOrderOptionsRequest();
        MultiOrder multiCart = getCheckouterResponse();

        when(
                checkouter.cart(
                        any(MultiOrder.class),
                        any(CartParameters.class)
                )
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                null,
                false,
                false,
                false,
                new GenericParamsBuilder()
                        .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );

        OutletDeliveryOption deliveryOption =
                (OutletDeliveryOption) orderOptions.getCart(774).getDeliveryOptions().get(0);

        assertThat(deliveryOption.getOutlets(),
                containsInAnyOrder(
                        OrderOutletMatcher.outlet(
                                OrderOutletMatcher.id(1),
                                OrderOutletMatcher.name(null)),
                        OrderOutletMatcher.outlet(
                                OrderOutletMatcher.id(2),
                                OrderOutletMatcher.name(null))
                )
        );
    }

    @Test
    public void testOrderOptionsWithoutOfferId() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        OrderOptionsRequest request = getOrderOptionsRequest();
        OrderOptionsRequest.OrderItem item = request.getShopOrders().get(0).getItems().get(0);
        item.setOfferId(null);
        item.setWareMd5(OFFER_ID.getWareMd5());
        item.setFeeShow(OFFER_ID.getFeeShow());
        MultiOrder multiCart = getCheckouterResponse();

        when(
                checkouter.cart(
                        any(MultiOrder.class),
                        any(CartParameters.class)
                )
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                null,
                false,
                false,
                false,
                new GenericParamsBuilder()
                        .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );

        OutletDeliveryOption deliveryOption =
                (OutletDeliveryOption) orderOptions.getCart(774).getDeliveryOptions().get(0);

        assertThat(deliveryOption.getOutlets(),
                containsInAnyOrder(
                        OrderOutletMatcher.outlet(
                                OrderOutletMatcher.id(1),
                                OrderOutletMatcher.name(null)),
                        OrderOutletMatcher.outlet(
                                OrderOutletMatcher.id(2),
                                OrderOutletMatcher.name(null))
                )
        );
    }

    @Test
    public void testOrderOptionsWithDeliveryError() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        OrderOptionsRequest request = getOrderOptionsRequestWithTwoOrders();
        MultiOrder multiCart = getCheckouterResponseWithTwoOrders();
        multiCart.getCarts().get(0).addValidation(new ValidationResult("ACTUAL_DELIVERY_OFFER_PROBLEMS",
                ValidationResult.Severity.ERROR));

        when(
                checkouter.cart(
                        any(MultiOrder.class),
                        any(CartParameters.class)
                )
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                new MockHttpServletResponse(),
                false,
                false,
                false,
                new GenericParamsBuilder()
                        .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );

        assertThat(orderOptions.getSummary().getTotalAmount(),
                is(BigDecimal.valueOf(237068.32))
        );
    }

    @Test
    public void findCartItemsBySku() {
        Collection<Long> items123 = controller.findCartItems(findCartItemsData(), "123", null);
        assertThat(
                items123,
                Matchers.containsInAnyOrder(100L, 300L)
        );
        Collection<Long> items456 = controller.findCartItems(findCartItemsData(), "456", null);
        assertThat(
                items456,
                Matchers.containsInAnyOrder(200L)
        );
    }

    @Test
    public void findCartItemsByOfferId() {
        Collection<Long> itemsABC = controller.findCartItems(findCartItemsData(), null, "abc");
        assertThat(
                itemsABC,
                Matchers.containsInAnyOrder(100L, 200L)
        );
        Collection<Long> itemsDEF = controller.findCartItems(findCartItemsData(), null, "def");
        assertThat(
                itemsDEF,
                Matchers.containsInAnyOrder(300L)
        );
    }

    @Test
    public void findCartItemsPreferedBySku() {
        Collection<Long> items123 = controller.findCartItems(findCartItemsData(), "123", "abc");
        assertThat(
                items123,
                Matchers.containsInAnyOrder(100L, 300L)
        );
        Collection<Long> items456 = controller.findCartItems(findCartItemsData(), "456", "def");
        assertThat(
                items456,
                Matchers.containsInAnyOrder(200L)
        );
    }

    @Test
    public void testOrderOptionsWithBnplInfo() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        BnplInfo bnplInfo = new BnplInfo();
        bnplInfo.setAvailable(true);
        bnplInfo.setSelected(true);

        OrderOptionsRequest request = getOrderOptionsRequest();
        MultiOrder multiCart = getCheckouterResponse();
        multiCart.setBnplInfo(bnplInfo);

        when(
                checkouter.cart(
                        any(MultiOrder.class),
                        any(CartParameters.class)
                )
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                null,
                false,
                false,
                false,
                new GenericParamsBuilder()
                        .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );

        assertThat(orderOptions.getSummary().getBnplInformation().isAvailable(), Matchers.is(true));
        assertThat(orderOptions.getSummary().getBnplInformation().isSelected(), Matchers.is(true));
    }

    @Test
    public void testOrderOptionsWithDeliveryCustomizers() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        OrderOptionsRequest request = getOrderOptionsRequest();
        MultiOrder multiCart = getCheckouterResponse();

        when(
                checkouter.cart(
                        any(MultiOrder.class),
                        any(CartParameters.class)
                )
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                null,
                false,
                false,
                false,
                new GenericParamsBuilder()
                        .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );
        List<DeliveryCustomizer> customizers = orderOptions.getCart(774L).getDeliveryOptions().get(0).getCustomizers();
        assertThat(customizers, hasSize(2));
        assertThat(customizers, containsInAnyOrder(
                new DeliveryCustomizer("leave_at_the_door", "Оставить у двери", "boolean"),
                new DeliveryCustomizer("no_call", "Не звонить", "boolean")
        ));
    }

    @Test
    public void testOrderOptionsWithService() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        OrderOptionsRequest request = getOrderOptionsWithServiceRequest();
        MultiCart multiCart = getCheckouterResponse();

        when(
                checkouter.cart(
                        any(MultiCart.class),
                        any(CartParameters.class)
                )
        ).thenReturn(multiCart);

        controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                null,
                false,
                false,
                false,
                new GenericParamsBuilder()
                        .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );

        ArgumentCaptor<MultiCart> captor = ArgumentCaptor.forClass(MultiCart.class);
        verify(checkouter, atLeastOnce()).cart(captor.capture(), any());

        ItemService service = captor.getValue()
                .getCarts()
                .get(0)
                .getItems()
                .iterator().next()
                .getServices()
                .iterator().next();

        assertThat(service.getServiceId(), equalTo(SERVICE_ID));
        assertThat(service.getDate(), equalTo(SERVICE_DATE));
        assertThat(service.getFromTime(), equalTo(SERVICE_FROM_TIME));
        assertThat(service.getToTime(), equalTo(SERVICE_TO_TIME));
        assertThat(service.getPrice(), equalTo(SERVICE_PRICE));
        assertThat(service.getYaUslugiTimeslotReqId(), equalTo(SERVICE_REQ_ID));
    }

    @Test
    public void testOrderOptionsWithInstallmentsInformation() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        InstallmentsInfo installmentsInfo = new InstallmentsInfo(ImmutableList.of(
                new InstallmentsOption("6", new MonthlyPayment(ru.yandex.common.util.currency.Currency.RUR, "600")),
                new InstallmentsOption("12", new MonthlyPayment(ru.yandex.common.util.currency.Currency.RUR, "363.5"))),
                null);

        OrderOptionsRequest request = getOrderOptionsRequest();
        MultiOrder multiCart = getCheckouterResponse();
        multiCart.setInstallmentsInfo(installmentsInfo);

        when(checkouter.cart(
                any(MultiOrder.class),
                argThat(new RequestGeneratorHelper.CartParametersMatcherWithShowInstallments(true)))
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                null,
                false,
                false,
                false,
                new GenericParamsBuilder()
                        .setShowInstallments(true)
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );

        assertThat(orderOptions.getSummary().getInstallmentsInformation(),
                hasProperty("options", Matchers.hasItems(
                        allOf(
                                hasProperty("term", is("6")),
                                hasProperty("monthlyPayment", allOf(
                                        hasProperty("currency", is(Currency.RUR)),
                                        hasProperty("value", is("600"))))),
                        allOf(
                                hasProperty("term", is("12")),
                                hasProperty("monthlyPayment", allOf(
                                        hasProperty("currency", is(Currency.RUR)),
                                        hasProperty("value", is("363.5"))))))));
    }

    @Test
    public void testOrderOptionsWithCreditInformation() {
        reportTestClient.getOffersV2(new OfferId[]{OFFER_ID}, "get-offers.json")
                .times(2);

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setOptions(ImmutableList.of(
                new CreditOption("6", new MonthlyPayment(ru.yandex.common.util.currency.Currency.RUR, "600")),
                new CreditOption("12", new MonthlyPayment(ru.yandex.common.util.currency.Currency.RUR, "363.5"))));
        creditInformation.setSelected(null);

        OrderOptionsRequest request = getOrderOptionsRequest();
        MultiOrder multiCart = getCheckouterResponse();
        multiCart.setCreditInformation(creditInformation);

        when(checkouter.cart(
                any(MultiOrder.class),
                argThat(new RequestGeneratorHelper.CartParametersMatcherWithShowCreditBroker(true)))
        ).thenReturn(multiCart);

        OrderOptionsResponse orderOptions = controller.getOrderOptions(
                request,
                new User(new OauthUser(123), null, new Uuid("12345678901234567890123456789012"), null),
                null,
                false,
                false,
                false,
                new GenericParamsBuilder()
                        .setShowCredits(true)
                        .setShowCreditBroker(true)
                        .build(),
                new ValidationErrors(),
                TEST_RGBS,
                false,
                false,
                Collections.emptyList(),
                true
        );

        assertThat(orderOptions.getSummary().getCreditInformation(),
                hasProperty("options", Matchers.hasItems(
                        allOf(
                                hasProperty("term", is("6")),
                                hasProperty("monthlyPayment", allOf(
                                        hasProperty("currency", is(Currency.RUR)),
                                        hasProperty("value", is("600"))))),
                        allOf(
                                hasProperty("term", is("12")),
                                hasProperty("monthlyPayment", allOf(
                                        hasProperty("currency", is(Currency.RUR)),
                                        hasProperty("value", is("363.5"))))))));
        assertThat(orderOptions.getSummary().getCreditInformation(), hasProperty("selected", notNullValue()));
    }

    private OrderOptionsRequest getOrderOptionsRequest() {
        OrderOptionsRequest request = new OrderOptionsRequest();
        request.setRegionId(213);
        request.setCurrency(Currency.RUR);

        OrderOptionsRequest.ShopOrder shopOrder = new OrderOptionsRequest.ShopOrder();
        request.setShopOrders(Collections.singletonList(shopOrder));

        OrderOptionsRequest.OrderItem shopOrderItem = new OrderOptionsRequest.OrderItem();
        shopOrder.setItems(Collections.singletonList(shopOrderItem));
        shopOrderItem.setOfferId(OFFER_ID);
        shopOrderItem.setPrice(BigDecimal.valueOf(123.0));
        shopOrderItem.setCount(1);

        shopOrder.setShopId(774);
        OrderOptionsRequest.OutletDeliveryPoint deliveryPoint = new OrderOptionsRequest.OutletDeliveryPoint();
        deliveryPoint.setRegionId(213);
        shopOrder.setDeliveryPoint(deliveryPoint);

        return request;
    }

    private OrderOptionsRequest getOrderOptionsWithServiceRequest() {
        OrderOptionsRequest request = new OrderOptionsRequest();
        request.setRegionId(213);
        request.setCurrency(Currency.RUR);

        OrderOptionsRequest.ShopOrder shopOrder = new OrderOptionsRequest.ShopOrder();
        request.setShopOrders(Collections.singletonList(shopOrder));

        OrderOptionsRequest.OrderItem shopOrderItem = new OrderOptionsRequest.OrderItem();
        OrderItemService service = new OrderItemService();
        service.setServiceId(SERVICE_ID);
        service.setDate(SERVICE_DATE);
        service.setFromTime(SERVICE_FROM_TIME);
        service.setToTime(SERVICE_TO_TIME);
        service.setPrice(SERVICE_PRICE);
        service.setYaUslugiTimeslotReqId(SERVICE_REQ_ID);

        shopOrder.setItems(Collections.singletonList(shopOrderItem));
        shopOrderItem.setOfferId(OFFER_ID);
        shopOrderItem.setPrice(BigDecimal.valueOf(123.0));
        shopOrderItem.setCount(1);
        shopOrderItem.setServices(Collections.singleton(service));

        shopOrder.setShopId(774);
        OrderOptionsRequest.OutletDeliveryPoint deliveryPoint = new OrderOptionsRequest.OutletDeliveryPoint();
        deliveryPoint.setRegionId(213);
        shopOrder.setDeliveryPoint(deliveryPoint);

        return request;
    }

    private OrderOptionsRequest getOrderOptionsRequestWithTwoOrders() {
        OrderOptionsRequest request = new OrderOptionsRequest();
        request.setRegionId(213);
        request.setCurrency(Currency.RUR);

        OrderOptionsRequest.OrderItem shopOrderItem = new OrderOptionsRequest.OrderItem();
        shopOrderItem.setOfferId(OFFER_ID);
        shopOrderItem.setPrice(BigDecimal.valueOf(123.0));
        shopOrderItem.setCount(1);

        OrderOptionsRequest.OutletDeliveryPoint deliveryPoint = new OrderOptionsRequest.OutletDeliveryPoint();
        deliveryPoint.setRegionId(213);

        OrderOptionsRequest.ShopOrder shopOrder1 = new OrderOptionsRequest.ShopOrder();
        shopOrder1.setItems(Collections.singletonList(shopOrderItem));
        shopOrder1.setShopId(774);
        shopOrder1.setDeliveryPoint(deliveryPoint);

        OrderOptionsRequest.ShopOrder shopOrder2 = new OrderOptionsRequest.ShopOrder();
        shopOrder2.setItems(Collections.singletonList(shopOrderItem));
        shopOrder2.setShopId(226);
        shopOrder2.setDeliveryPoint(deliveryPoint);

        request.setShopOrders(Arrays.asList(shopOrder1, shopOrder2));
        return request;
    }

    private MultiOrder getCheckouterResponse() {
        List<DeliveryCustomizer> customizers = new ArrayList<>();
        customizers.add(new DeliveryCustomizer("leave_at_the_door", "Оставить у двери", "boolean"));
        customizers.add(new DeliveryCustomizer("no_call", "Не звонить", "boolean"));
        List<RawDeliveryIntervalAndDate> deliveryIntervalsAndDate = new ArrayList<>();
        List<RawDeliveryInterval> deliveryIntervals = new ArrayList<>();
        LocalTime twelve = LocalTime.of(12, 0);
        deliveryIntervals.add(new RawDeliveryInterval(new Date(), twelve, twelve.plus(3, ChronoUnit.HOURS)));
        deliveryIntervalsAndDate.add(new RawDeliveryIntervalAndDate(new Date(), deliveryIntervals));
        return new MultiOrderBuilder()
                .random()
                .withOrders(new MultiCartOrderBuilder()
                        .withId(123456L)
                        .withShopId(774L)
                        .withShopOrderId("123456")
                        .withItem(new MultiCartOrderItemBuilder()
                                .random()
                                .withWareMd5(OFFER_ID.getWareMd5())
                                .withOfferId("shopOfferId" + OFFER_ID.getWareMd5())
                                .withFeedId(567L)
                                .withCount(1)
                                .build()
                        )
                        .withDeliveryOptions(
                                new MultiCartOrderDeliveryBuilder(DeliveryType.PICKUP)
                                        .random()
                                        .withOutletId(null)
                                        .withOutletIds(OUTLET_IDS)
                                        .withPaymentOptions(PaymentMethod.YANDEX)
                                        .withDeliveryIntervals(new RawDeliveryIntervalsCollection(deliveryIntervalsAndDate))
                                        .withCustomizers(customizers)
                                        .build()
                        )
                        .build()
                )
                .build();
    }

    private MultiOrder getCheckouterResponseWithTwoOrders() {

        return new MultiOrderBuilder()
                .random()
                .withOrders(new MultiCartOrderBuilder()
                                .withId(123456L)
                                .withShopId(774L)
                                .withShopOrderId("123456")
                                .withItem(new MultiCartOrderItemBuilder()
                                        .random()
                                        .withWareMd5(OFFER_ID.getWareMd5())
                                        .withOfferId("shopOfferId" + OFFER_ID.getWareMd5())
                                        .withFeedId(567L)
                                        .withCount(1)
                                        .build()
                                )
                                .withDeliveryOptions(
                                        new MultiCartOrderDeliveryBuilder(DeliveryType.PICKUP)
                                                .random()
                                                .withOutletId(null)
                                                .withOutletIds(OUTLET_IDS)
                                                .withPaymentOptions(PaymentMethod.YANDEX)
                                                .build()
                                )
                                .build(),
                        new MultiCartOrderBuilder()
                                .withId(987654L)
                                .withShopId(226L)
                                .withShopOrderId("987654")
                                .withItem(new MultiCartOrderItemBuilder()
                                        .random()
                                        .withWareMd5(OFFER_ID.getWareMd5())
                                        .withOfferId("shopOfferId" + OFFER_ID.getWareMd5())
                                        .withFeedId(567L)
                                        .withCount(1)
                                        .build()
                                )
                                .withDeliveryOptions(
                                        new MultiCartOrderDeliveryBuilder(DeliveryType.PICKUP)
                                                .random()
                                                .withOutletId(null)
                                                .withOutletIds(OUTLET_IDS)
                                                .withPaymentOptions(PaymentMethod.YANDEX)
                                                .build()
                                )
                                .build()
                )
                .withTotals(new MultiCartTotalsBuilder()
                        .withBuyerItemsTotal(BigDecimal.valueOf(236000))
                        .withBuyerDeliveryTotal(BigDecimal.valueOf(1200))
                        .withBuyerTotal(BigDecimal.valueOf(237200))
                        .build()
                )
                .build();
    }

    private Collection<CartItem> findCartItemsData() {
        CartItem item1 = new CartItem();
        item1.setObjId("abc");
        item1.setMsku(123L);
        item1.setId(100L);

        CartItem item2 = new CartItem();
        item2.setObjId("abc");
        item2.setMsku(456L);
        item2.setId(200L);

        CartItem item3 = new CartItem();
        item3.setObjId("def");
        item3.setMsku(123L);
        item3.setId(300L);

        return Arrays.asList(item1, item2, item3);
    }

    @Profile("OrderControllerTest")
    @org.springframework.context.annotation.Configuration
    public static class Configuration {
        @Bean
        @Primary
        public CheckouterAPI checkouter() {
            return Mockito.mock(CheckouterAPI.class);
        }
    }

}
