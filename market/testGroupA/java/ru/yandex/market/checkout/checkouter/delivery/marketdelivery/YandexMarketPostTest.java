package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.PickupOption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.defaultActualDelivery;
import static ru.yandex.market.checkout.test.providers.AddressProvider.getAddressWithoutPostcode;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.ANOTHER_POST_OUTLET_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryResultProvider.getActualDeliveryResult;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class YandexMarketPostTest extends AbstractWebTestBase {

    private static final Long OTHER_POST_OUTLET_ID = 123123123L;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Создание МарДо заказа в почту")
    @CsvSource({"false", "true"})
    @ParameterizedTest
    public void testCreateOrder(boolean enable) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_POST_OUTLET_ACTUALIZER, enable);
        Parameters parameters = prepareParameters();

        assertCartHasMarDoPostOption(parameters);

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getDelivery(), hasProperty("deliveryServiceId", is(RUSPOST_DELIVERY_SERVICE_ID)));
        assertThat(order.getDelivery().getParcels(), hasSize(1));
        assertThat(order.getDelivery().getPostOutlet(), allOf(
                hasProperty("name", is("Почтовое отделение")),
                hasProperty("street", is("Ленинский проспект"))
        ));
        assertThat(order.getDelivery().getOutletStoragePeriod(), is(1));
        assertThat(order.getDelivery().getPostOutletId(), is(DeliveryProvider.POST_OUTLET_ID));

    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void shouldSaveOutletStoragePeriodInEvent(boolean enable) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_POST_OUTLET_ACTUALIZER, enable);
        Parameters parameters = prepareParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());

        List<Delivery> afterDelivery =
                events.getItems().stream().map(e -> e.getOrderAfter().getDelivery()).collect(Collectors.toList());
        MatcherAssert.assertThat(afterDelivery, everyItem(hasProperty("outletStoragePeriod", is(1))));
    }


    @Test
    public void shouldNotCreateOrderWithoutPostcode() {
        Parameters parameters = prepareParameters();
        parameters.configureMultiCart(multiCart -> {
            multiCart.getCarts().forEach(cart -> cart.getDelivery().setBuyerAddress(getAddressWithoutPostcode()));
        });
        assertCartHasMarDoPostOption(parameters);

        parameters.getOrder().getDelivery().setBuyerAddress(getAddressWithoutPostcode());
        parameters.setCheckOrderCreateErrors(false);
        parameters.setExpectedCheckoutReturnCode(400);
        parameters.setErrorMatcher(mvcResult -> {
        });
        orderCreateHelper.createOrder(parameters);
    }

    @Test
    //не может работать из за ошибки NO_POST_OUTLET, так как мапинг в
    //ru.yandex.market.checkout.checkouter.delivery.YandexMarketDeliveryActualizerImpl.createPostOption
    //не мапит оутлеты почты если их больше 1
    @Deprecated
    @Disabled
    public void shouldNotFailCartWithSelectedPostOptionWithoutPostcode() {
        Parameters parameters = prepareParameters();
        parameters.getReportParameters()
                .getActualDelivery().getResults().get(0)
                .getPost().stream().findFirst().get()
                .setOutletIds(Arrays.asList(DeliveryProvider.POST_OUTLET_ID, OTHER_POST_OUTLET_ID));

        parameters.configureMultiCart(multiCart -> {
            multiCart.getCarts().forEach(cart -> cart.getDelivery().setBuyerAddress(getAddressWithoutPostcode()));
        });
        MultiCart response = orderCreateHelper.cart(parameters);
        String postHash = response.getCarts().get(0).getDeliveryOptions().stream()
                .filter(option -> option.getDeliveryPartnerType() == YANDEX_MARKET
                        && option.getType() == DeliveryType.POST)
                .findFirst().get().getHash();
        parameters.configureMultiCart(multiCart -> {
            multiCart.getCarts().forEach(cart -> {
                cart.getDelivery().setId(postHash);
                cart.getDelivery().setHash(postHash);
            });
        });
        orderCreateHelper.cart(parameters);
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Изменение точки получения в почтовом заказе")
    //ignored until 'remove items' is refactored and supported in production
    @Disabled
    @Test
    public void testChangePostOutlet() throws Exception {
        Parameters parameters = prepareParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);

        Delivery deliveryUpdateReq = new Delivery();
        deliveryUpdateReq.setRegionId(DeliveryProvider.REGION_ID);
        deliveryUpdateReq.setPostOutletId(ANOTHER_POST_OUTLET_ID);
        order = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryUpdateReq);

        assertThat(order.getDelivery().getPostOutlet(), allOf(
                hasProperty("name", is("Почтовое отделение 2")),
                hasProperty("street", is("улица Паустовского"))
        ));
        assertThat(order.getDelivery().getPostOutletId(), is(DeliveryProvider.ANOTHER_POST_OUTLET_ID));
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Изменение точки получения в почтовом заказе - нельзя поменять на несуществующую точку")
    @Test
    public void testChangePostOutletFailOnUnknownId() throws Exception {
        Parameters parameters = prepareParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);

        Delivery deliveryUpdateReq = new Delivery();
        deliveryUpdateReq.setRegionId(DeliveryProvider.REGION_ID);
        deliveryUpdateReq.setPostOutletId(30698L);

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(),
                new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 123L), deliveryUpdateReq)
                .andExpect(status().isBadRequest());
    }

    @Tag(Tags.MARKET_DELIVERY)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Переводим в PICKUP по 45, а не 48 чекпоинту")
    @Test
    public void test45CheckpointToPickup() throws Exception {
        Parameters parameters = prepareParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 48, 1));
        assertThat(orderService.getOrder(order.getId()), hasProperty("status", is(OrderStatus.DELIVERY)));

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 45, 2));
        assertThat(orderService.getOrder(order.getId()), hasProperty("status", is(OrderStatus.PICKUP)));
    }

    private void assertCartHasMarDoPostOption(Parameters parameters) {
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Collection<? extends Delivery> marDoPostOption = Iterables.getOnlyElement(multiCart.getCarts())
                .getDeliveryOptions()
                .stream()
                .filter(option -> option.getDeliveryPartnerType() == YANDEX_MARKET
                        && option.getType() == DeliveryType.POST)
                .collect(Collectors.toList());
        assertThat(marDoPostOption, hasSize(1));
        Date today = DateUtil.truncDay(new Date());
        assertThat(marDoPostOption, contains(allOf(
                hasProperty("deliveryServiceId", is(RUSPOST_DELIVERY_SERVICE_ID)),
                hasProperty("type", is(DeliveryType.POST)),
                hasProperty("paymentOptions", containsInAnyOrder(
                        PaymentMethod.YANDEX,
                        PaymentMethod.APPLE_PAY,
                        PaymentMethod.GOOGLE_PAY)
                ),
                hasProperty("price", is(new BigDecimal("101"))),
                hasProperty("deliveryDates", allOf(
                        hasProperty("fromDate", is(today)),
                        hasProperty("toDate", is(DateUtil.addDay(today, 2))))),
                hasProperty("outlets", anyOf(empty(), nullValue())),
                hasProperty("postCodes", is(Collections.singletonList(111222L))),
                hasProperty("postOutletId", is(DeliveryProvider.POST_OUTLET_ID))
        )));
    }

    @Nonnull
    private Parameters prepareParameters() {
        Date fakeNow = DateUtil.convertDotDateFormat("07.07.2027");
        int shipmentDays = (int) Math.ceil(DateUtil.diffInDays(fakeNow, DateUtil.now()));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.POST)
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withPartnerInterface(true)
                .buildParameters();
        // подкладываем опцию в actual_delivery
        PickupOption postOption = new PickupOption();
        postOption.setDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID);
        postOption.setDayFrom(0);
        postOption.setDayTo(2);
        postOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        postOption.setPrice(new BigDecimal("101"));
        postOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        postOption.setShipmentDay(shipmentDays);
        postOption.setOutletIds(Collections.singletonList(DeliveryProvider.POST_OUTLET_ID));
        postOption.setPostCodes(Collections.singletonList(111222L));
        ActualDeliveryResult actualDeliveryResult = getActualDeliveryResult();
        actualDeliveryResult.setPost(Collections.singletonList(postOption));
        ActualDelivery actualDelivery = defaultActualDelivery();
        actualDelivery.setResults(Collections.singletonList(actualDeliveryResult));
        parameters.getReportParameters().setActualDelivery(actualDelivery);
        //убираем подложенную опцию под айтемом
        parameters.getReportParameters().setLocalDeliveryOptions(Collections.emptyMap());
        parameters.setFreeDelivery(false);
        return parameters;
    }
}
