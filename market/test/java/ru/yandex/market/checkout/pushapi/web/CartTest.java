package ru.yandex.market.checkout.pushapi.web;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.service.EnvironmentService;
import ru.yandex.market.checkout.pushapi.settings.DataType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.pushapi.service.shop.postprocessors.FillDeliveryIntervalsPostprocessor.FILL_DBS_DELIVERY_INTERVALS_FLAG;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;

public class CartTest extends AbstractWebTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CartTest.class);

    @Autowired
    private PushApiCartHelper pushApiCartHelper;
    @Autowired
    private WireMockServer shopadminStubMock;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private EnvironmentService environmentService;

    @Test
    public void testCart() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        Assertions.assertFalse(cartResponse.isShopAdmin());
        assertThat(cartResponse.getItems(), allOf(notNullValue(), hasSize(1)));

        OrderItem item = Iterables.getOnlyElement(cartResponse.getItems());
        Assertions.assertEquals(new BigDecimal(250), item.getPrice());
        Assertions.assertEquals(1, item.getCount().intValue());
        Assertions.assertTrue(item.getDelivery());

        assertThat(cartResponse.getDeliveryOptions(), hasSize(3));
        Assertions.assertTrue(cartResponse.getDeliveryOptions().stream()
                .map(DeliveryResponse::getDeliveryServiceId)
                .allMatch(Predicate.isEqual(Delivery.SELF_DELIVERY_SERVICE_ID)), "all shop options has delivery " +
                "service id = 99");
        Assertions.assertTrue(cartResponse.getDeliveryOptions().stream()
                .map(DeliveryResponse::getDeliveryPartnerType)
                .allMatch(Predicate.isEqual(DeliveryPartnerType.SHOP)), "all shop options has delivery partner type " +
                "== SHOP");
        Assertions.assertTrue(cartResponse.getDeliveryOptions().stream()
                .map(DeliveryResponse::getDeliveryOptionId)
                .allMatch(Objects::nonNull), "all shop options has non null deliveryOptionId");
        Assertions.assertEquals(cartResponse.getDeliveryOptions().size(), cartResponse.getDeliveryOptions().stream()
                .map(DeliveryResponse::getDeliveryOptionId)
                .distinct()
                .count(), "all shop options has unique deliveryOptionId");

        List<ServeEvent> servedEvents = shopadminStubMock.getAllServeEvents();
        assertThat(servedEvents, hasSize(1));
        ServeEvent event = Iterables.getOnlyElement(servedEvents);

        String token = ServeEventUtils.extractTokenParameter(event);
        Assertions.assertEquals(SettingsProvider.DEFAULT_TOKEN, token);

        LoggedRequest request = event.getRequest();
        byte[] requestBytes = request.getBodyAsString().getBytes(StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        var objectMap = (Map<String, String>) objectMapper.readValue(requestBytes, Map.class);

        Assertions.assertEquals(JsonUtil.getByPath(objectMap, "/cart/delivery/region/id"), 213);
    }

    @Test
    public void shouldNotSendContextToExternalApi() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        pushApiCartParameters.setPartnerInterface(false);
        pushApiCartParameters.getRequest().setRgb(Color.GREEN);
        pushApiCartParameters.setContext(Context.SANDBOX);
        pushApiCartParameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        List<ServeEvent> serveEvents = pushApiCartHelper.getServeEvents();
        assertThat(serveEvents, hasSize(1));

        ServeEvent event = Iterables.getOnlyElement(serveEvents);

        logger.debug("{}", event.getRequest());
        String body = event.getRequest().getBodyAsString();

        JsonPathUtils.jpath("$.cart.context")
                .doesNotExist(body);
        JsonPathUtils.jpath("$.cart.items[0].warehouseId")
                .assertValue(body, MOCK_SORTING_CENTER_HARDCODED);
        JsonPathUtils.jpath("$.cart.items[0].partnerWarehouseId")
                .assertValue(body, "super-sklad");
    }

    @Test
    public void shouldNotSendContextToShopadminStub() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        pushApiCartParameters.setPartnerInterface(true);
        pushApiCartParameters.getRequest().setRgb(Color.BLUE);
        pushApiCartParameters.setContext(Context.SANDBOX);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        Assertions.assertTrue(cartResponse.isShopAdmin());

        List<ServeEvent> serveEvents = pushApiCartHelper.getServeEvents();
        assertThat(serveEvents, hasSize(0));
    }

    @Test
    public void shouldNotSendRgbToExternalApi() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        pushApiCartParameters.setPartnerInterface(false);
        pushApiCartParameters.getRequest().setRgb(Color.GREEN);

        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        List<ServeEvent> serveEvents = pushApiCartHelper.getServeEvents();
        assertThat(serveEvents, hasSize(1));

        ServeEvent event = Iterables.getOnlyElement(serveEvents);

        logger.debug("{}", event.getRequest());
        byte[] body = event.getRequest().getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        var objectMap = (Map<String, String>) objectMapper.readValue(body, Map.class);

        Assertions.assertNull(JsonUtil.getByPath(objectMap, "/cart/rgb"));
    }

    @Test
    public void shouldNotSendRgbToShopadminStub() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        pushApiCartParameters.setPartnerInterface(true);
        pushApiCartParameters.getRequest().setRgb(Color.BLUE);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        Assertions.assertTrue(cartResponse.isShopAdmin());

        List<ServeEvent> serveEvents = pushApiCartHelper.getServeEvents();
        assertThat(serveEvents, hasSize(0));
    }

    @Test
    public void shouldNotSendPreorderToShopadminStub() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        pushApiCartParameters.setPartnerInterface(true);
        pushApiCartParameters.getRequest().setPreorder(true);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        Assertions.assertTrue(cartResponse.isShopAdmin());

        List<ServeEvent> serveEvents = pushApiCartHelper.getServeEvents();
        assertThat(serveEvents, hasSize(0));
    }

    @Test
    public void shouldGoToStubIfApiSettingsStub() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        pushApiCartParameters.setApiSettings(ApiSettings.STUB);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        pushApiCartHelper.cartForActions(pushApiCartParameters)
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void shouldEraseDeliveryTimeOptionsForOnDemand() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        pushApiCartParameters.getShopCartResponse().getDeliveryOptions()
                .forEach(dr -> {
                    dr.setFeatures(Set.of(DeliveryFeature.ON_DEMAND));
                    dr.getDeliveryDates().setFromTime(LocalTime.now());
                    dr.getDeliveryDates().setToTime(LocalTime.now().plusHours(1));
                });

        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        cartResponse.getDeliveryOptions().forEach(dr -> {
            assertThat(dr.getDeliveryDates().getFromTime(), nullValue());
            assertThat(dr.getDeliveryDates().getToTime(), nullValue());
        });
    }

    @Test
    public void shouldFillToDateIfItIsNotInResponse() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        pushApiCartParameters.getShopCartResponse().getDeliveryOptions()
                .forEach(dr -> dr.getDeliveryDates().setToDate(null));
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        cartResponse.getDeliveryOptions().forEach(dr -> {
            assertThat(dr.getDeliveryDates().getToDate(), notNullValue());
        });
    }

    @Test
    public void shouldFillDeliveryServiceId() throws Exception {
        long id = 1;

        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        for (DeliveryResponse dr : pushApiCartParameters.getShopCartResponse().getDeliveryOptions()) {
            dr.setId(String.valueOf(id++));
            dr.setDeliveryServiceId(null);
        }
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        cartResponse.getDeliveryOptions().forEach(dr -> {
            assertThat(dr.getDeliveryServiceId(), notNullValue());
        });
    }

    @Test
    public void shouldFillDatesForDigitalDelivery() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        CartResponse cartResponseMock = pushApiCartParameters.getShopCartResponse();

        pushApiCartParameters.getRequest().getItems().forEach(item -> item.setDigital(true));
        for (DeliveryResponse dr : cartResponseMock.getDeliveryOptions()) {
            dr.setDeliveryDates(null);
            dr.setPaymentOptions(Set.of(PaymentMethod.YANDEX, PaymentMethod.APPLE_PAY, PaymentMethod.GOOGLE_PAY));
        }
        cartResponseMock.setPaymentMethods(
                List.of(PaymentMethod.YANDEX, PaymentMethod.APPLE_PAY, PaymentMethod.GOOGLE_PAY));
        pushApiCartParameters.setShopCartResponse(cartResponseMock);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        cartResponse.getDeliveryOptions().forEach(dr -> {
            assertThat(dr.getDeliveryDates(), notNullValue());
            assertThat(dr.getDeliveryDates().getFromDate(), notNullValue());
            assertThat(dr.getDeliveryDates().getToDate(), notNullValue());
        });
    }

    @Test
    public void shouldFillDeliveryOptionIdForSeparatedIntervals() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        CartResponse cartResponse = pushApiCartParameters.getShopCartResponse();
        List<DeliveryResponse> deliveryOptions = cartResponse.getDeliveryOptions();
        DeliveryResponse deliveryOption = deliveryOptions.get(0);

        Date fromDate = new Date();
        Date toDate = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        deliveryOption.setDeliveryDates(new DeliveryDates(fromDate, toDate));

        RawDeliveryIntervalsCollection deliveryIntervals = deliveryOption.getRawDeliveryIntervals();
        deliveryIntervals.add(new RawDeliveryInterval(fromDate, LocalTime.of(9, 0), LocalTime.of(22, 0)));
        deliveryIntervals.add(new RawDeliveryInterval(toDate, LocalTime.of(10, 0), LocalTime.of(18, 0)));

        cartResponse.setDeliveryOptions(Collections.singletonList(deliveryOption));
        pushApiCartParameters.setShopCartResponse(cartResponse);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse actualCartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        List<DeliveryResponse> actualDeliveryOptions = actualCartResponse.getDeliveryOptions();
        assertThat(actualDeliveryOptions, hasSize(2));

        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String date1 = formatter.format(fromDate);
        String date2 = formatter.format(toDate);
        String deliveryOptionIdFormat = "242102_DELIVERY_Доставка_%s_noreserve_%s_null_56_99_SHOP_%s_TRYING_OFF";
        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date1, date1, "0900-2200")))));
        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date2, date2, "1000-1800")))));
    }

    @Test
    public void shouldMarkDefaultDeliveryInterval() throws Exception {
        var pushApiCartParameters = new PushApiCartParameters();
        var cartResponse = pushApiCartParameters.getShopCartResponse();
        var deliveryOptions = cartResponse.getDeliveryOptions();
        var deliveryOption = deliveryOptions.get(0);

        var date = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        deliveryOption.setDeliveryDates(new DeliveryDates(date, date));

        var deliveryIntervals = deliveryOption.getRawDeliveryIntervals();
        deliveryIntervals.add(new RawDeliveryInterval(date, LocalTime.of(9, 0), LocalTime.of(22, 0)));
        deliveryIntervals.add(new RawDeliveryInterval(date, LocalTime.of(10, 0), LocalTime.of(18, 0)));

        cartResponse.setDeliveryOptions(Collections.singletonList(deliveryOption));
        pushApiCartParameters.setShopCartResponse(cartResponse);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        var actualCartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        var actualDeliveryOptions = actualCartResponse.getDeliveryOptions();
        assertThat(actualDeliveryOptions, hasSize(1));

        var intervalsCollection = actualDeliveryOptions.get(0).getRawDeliveryIntervals();
        var intervals = new LinkedList<>(intervalsCollection.getIntervalsByDate(date));
        assertThat(intervals, hasSize(2));

        var firstInterval = intervals.get(0);
        assertThat(firstInterval.getFromTime(), is(LocalTime.of(9, 0)));
        assertThat(firstInterval.isDefault(), equalTo(true));

        var secondInterval = intervals.get(1);
        assertThat(secondInterval.getFromTime(), is(LocalTime.of(10, 0)));
        assertThat(secondInterval.isDefault(), equalTo(false));
    }

    @Test
    public void testShouldFillDbsDeliveryIntervals() throws Exception {
        environmentService.upsert(FILL_DBS_DELIVERY_INTERVALS_FLAG, "true");

        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        Delivery delivery = pushApiCartParameters.getRequest().getDelivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        CartResponse cartResponse = pushApiCartParameters.getShopCartResponse();
        List<DeliveryResponse> deliveryOptions = cartResponse.getDeliveryOptions();

        DeliveryResponse firstDeliveryOption = deliveryOptions.get(0);
        firstDeliveryOption.setPaymentOptions(Set.of(PaymentMethod.YANDEX));

        LocalDate startDate = LocalDate.now();
        firstDeliveryOption.setDeliveryDates(
                new DeliveryDates(
                        DateUtil.asDate(startDate),
                        DateUtil.asDate(startDate)
                )
        );

        DeliveryResponse secondDeliveryOption = new DeliveryResponse(deliveryOptions.get(0));
        secondDeliveryOption.setPaymentOptions(Set.of(PaymentMethod.YANDEX));
        secondDeliveryOption.setDeliveryDates(
                new DeliveryDates(
                        DateUtil.asDate(startDate.plusDays(2)),
                        DateUtil.asDate(startDate.plusDays(4))
                )
        );

        cartResponse.setDeliveryOptions(List.of(firstDeliveryOption, secondDeliveryOption));

        pushApiCartParameters.setShopCartResponse(cartResponse);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse actualCartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        List<DeliveryResponse> actualDeliveryOptions = actualCartResponse.getDeliveryOptions();
        assertThat(actualDeliveryOptions, hasSize(4));

        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String date1 = formatter.format(DateUtil.asDate(startDate));
        String date2 = formatter.format(DateUtil.asDate(startDate.plusDays(2)));
        String date3 = formatter.format(DateUtil.asDate(startDate.plusDays(3)));
        String date4 = formatter.format(DateUtil.asDate(startDate.plusDays(4)));
        String deliveryOptionIdFormat = "242102_DELIVERY_Доставка_%s_noreserve_%s_null_32_99_SHOP_%s_TRYING_OFF";

        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date1, date1, "null")))));

        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date2, date2, "0900-2100")))));

        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date3, date3, "0900-2100")))));

        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date4, date4, "0900-2100")))));
    }

    @Test
    public void testShouldntFilIntervalsOnEmptyToDate() throws Exception {
        environmentService.upsert(FILL_DBS_DELIVERY_INTERVALS_FLAG, "true");

        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
        Delivery delivery = pushApiCartParameters.getRequest().getDelivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        CartResponse cartResponse = pushApiCartParameters.getShopCartResponse();
        List<DeliveryResponse> deliveryOptions = cartResponse.getDeliveryOptions();

        DeliveryResponse firstDeliveryOption = deliveryOptions.get(0);
        firstDeliveryOption.setPaymentOptions(Set.of(PaymentMethod.YANDEX));

        LocalDate startDate = LocalDate.now();
        firstDeliveryOption.setDeliveryDates(
                new DeliveryDates(DateUtil.asDate(startDate), null)
        );

        firstDeliveryOption.setDeliveryDates(
                new DeliveryDates(DateUtil.asDate(startDate), null)
        );

        DeliveryResponse secondDeliveryOption = new DeliveryResponse(deliveryOptions.get(0));
        secondDeliveryOption.setPaymentOptions(Set.of(PaymentMethod.YANDEX));
        secondDeliveryOption.setDeliveryDates(
                new DeliveryDates(
                        DateUtil.asDate(startDate.plusDays(2)),
                        DateUtil.asDate(startDate.plusDays(3))
                )
        );

        cartResponse.setDeliveryOptions(List.of(firstDeliveryOption, secondDeliveryOption));

        pushApiCartParameters.setShopCartResponse(cartResponse);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse actualCartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        List<DeliveryResponse> actualDeliveryOptions = actualCartResponse.getDeliveryOptions();
        assertThat(actualDeliveryOptions, hasSize(3));

        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String date1 = formatter.format(DateUtil.asDate(startDate));
        String date2 = formatter.format(DateUtil.asDate(startDate.plusDays(2)));
        String date3 = formatter.format(DateUtil.asDate(startDate.plusDays(3)));
        String deliveryOptionIdFormat = "242102_DELIVERY_Доставка_%s_noreserve_%s_null_32_99_SHOP_%s_TRYING_OFF";

        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date1, date1, "null")))));

        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date2, date2, "0900-2100")))));

        assertThat(actualDeliveryOptions, hasItem(hasProperty("deliveryOptionId",
                is(String.format(deliveryOptionIdFormat, date3, date3, "0900-2100")))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testSellerInn(boolean json) throws Exception {
        var inn = "7710140679";
        var pushApiCartParameters = new PushApiCartParameters();
        if (json) {
            pushApiCartParameters.setDataType(DataType.JSON);
        }
        var shopCartResponse = pushApiCartParameters.getShopCartResponse();
        shopCartResponse.getItems().forEach(i -> i.setSellerInn(inn));
        pushApiCartParameters.setShopCartResponse(shopCartResponse);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cartResponse = pushApiCartHelper.cart(pushApiCartParameters);

        assertThat(cartResponse.getItems(), everyItem(hasProperty("sellerInn", is(inn))));
    }
}
