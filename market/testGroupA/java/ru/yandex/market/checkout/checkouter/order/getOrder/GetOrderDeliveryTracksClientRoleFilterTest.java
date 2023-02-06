package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.common.util.StreamUtils;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;

public class GetOrderDeliveryTracksClientRoleFilterTest extends AbstractWebTestBase {

    private static final long SHOP_CLIENT_ID = 1234L;
    private static final long REFEREE_CLIENT_ID = 5678L;

    private Order orderWithScTrack;
    private Order orderWithCarrierTrack;
    private Map<DeliveryServiceType, List<Long>> trackIdsByType;

    @Autowired
    private OrderDeliveryHelper deliveryHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderPayHelper payHelper;
    private Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier;

    public static Stream<Arguments> parameterizedTestData() {
        return Arrays.asList(
                () ->
                        get("/orders")
                                .param("rgb", "WHITE", "BLUE"),
                (Supplier<MockHttpServletRequestBuilder>) () ->
                        post("/get-orders")
                                .content("{\"rgbs\": [\"WHITE\",\"BLUE\"]}")
                                .contentType(MediaType.APPLICATION_JSON_UTF8)

        ).stream().map(Arguments::of);
    }

    @BeforeEach
    public void init() throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .build();
        payHelper.payForOrder(order);
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        Parcel shipment = new Parcel();
        shipment.setTracks(Arrays.asList(
                new Track("asdasd", MOCK_DELIVERY_SERVICE_ID),
                new Track("qweqwe", MOCK_SORTING_CENTER_HARDCODED)
        ));
        delivery.setParcels(Collections.singletonList(shipment));
        orderWithScTrack = deliveryHelper.updateOrderDelivery(order.getId(), delivery);

        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getBuyer().setUid(555L);
        parameters.setShopId(775L);
        order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        delivery = new Delivery();
        shipment = new Parcel();
        shipment.setTracks(Collections.singletonList(TrackProvider.createTrack()));
        delivery.setParcels(Collections.singletonList(shipment));
        orderWithCarrierTrack = deliveryHelper.updateOrderDelivery(order.getId(), delivery);

        trackIdsByType = Stream.of(orderWithScTrack, orderWithCarrierTrack)
                .map(Order::getDelivery)
                .map(Delivery::getParcels)
                .flatMap(StreamUtils::stream)
                .map(Parcel::getTracks)
                .flatMap(StreamUtils::stream)
                .collect(groupingBy(Track::getDeliveryServiceType, mapping(Track::getId, Collectors.toList())));
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить треки по заказам с ролью SYSTEM")
    @ParameterizedTest(name = "{index}")
    @MethodSource("parameterizedTestData")
    public void testClientRoleSystem(Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier) throws Exception {
        this.requestBuilderSupplier = requestBuilderSupplier;
        Matcher[] matchers = trackIdsByType.values().stream().flatMap(List::stream)
                .map(id -> hasEntry("id", id.intValue()))
                .toArray(Matcher[]::new);
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SYSTEM, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].delivery.tracks[*]",
                        containsInAnyOrder(matchers)));
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить треки по заказам с ролью SHOP")
    @ParameterizedTest(name = "{index}")
    @MethodSource("parameterizedTestData")
    public void testClientRoleShop(Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier) throws Exception {
        this.requestBuilderSupplier = requestBuilderSupplier;
        Matcher[] firstOrderCarrierTracks = orderWithScTrack.getDelivery()
                .getParcels()
                .stream()
                .flatMap(s -> s.getTracks().stream())
                .map(t -> hasEntry("id", t.getId().intValue()))
                .toArray(Matcher[]::new);
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, orderWithScTrack.getShopId(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].delivery.tracks[*]",
                        containsInAnyOrder(firstOrderCarrierTracks)));

        Matcher[] secondOrderCarrierTracks = orderWithCarrierTrack.getDelivery()
                .getParcels()
                .stream()
                .flatMap(s -> s.getTracks().stream())
                .map(t -> hasEntry("id", t.getId().intValue()))
                .toArray(Matcher[]::new);
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, orderWithCarrierTrack.getShopId(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].delivery.tracks[*]",
                        containsInAnyOrder(secondOrderCarrierTracks)));
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить треки по заказам с ролью SHOP_USER")
    @ParameterizedTest(name = "{index}")
    @MethodSource("parameterizedTestData")
    public void testClientRoleShopUser(Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier)
            throws Exception {
        this.requestBuilderSupplier = requestBuilderSupplier;
        Matcher[] firstOrderCarrierTracks = orderWithScTrack.getDelivery()
                .getParcels()
                .stream()
                .flatMap(s -> s.getTracks().stream())
                .map(t -> hasEntry("id", t.getId().intValue()))
                .toArray(Matcher[]::new);
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID, orderWithScTrack.getShopId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].delivery.tracks[*]",
                        containsInAnyOrder(firstOrderCarrierTracks)));

        Matcher[] secondOrderCarrierTracks = orderWithCarrierTrack.getDelivery()
                .getParcels()
                .stream()
                .flatMap(s -> s.getTracks().stream())
                .map(t -> hasEntry("id", t.getId().intValue()))
                .toArray(Matcher[]::new);
        mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID,
                orderWithCarrierTrack.getShopId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].delivery.tracks[*]",
                        containsInAnyOrder(secondOrderCarrierTracks)));
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить треки по заказам с ролью USER")
    @ParameterizedTest(name = "{index}")
    @MethodSource("parameterizedTestData")
    public void testClientRoleUser(Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier) throws Exception {
        this.requestBuilderSupplier = requestBuilderSupplier;
        Matcher[] firstOrderCarrierTracks = orderWithScTrack.getDelivery()
                .getParcels()
                .stream()
                .flatMap(s -> s.getTracks().stream())
                .filter(t -> t.getDeliveryServiceType() == DeliveryServiceType.CARRIER)
                .map(t -> hasEntry("id", t.getId().intValue()))
                .toArray(Matcher[]::new);
        mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, orderWithScTrack.getBuyer().getUid(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].delivery.tracks[*]",
                        containsInAnyOrder(firstOrderCarrierTracks)));

        Matcher[] secondOrderCarrierTracks = orderWithCarrierTrack.getDelivery()
                .getParcels()
                .stream()
                .flatMap(s -> s.getTracks().stream())
                .map(t -> hasEntry("id", t.getId().intValue()))
                .toArray(Matcher[]::new);
        mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, orderWithCarrierTrack.getBuyer().getUid(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].delivery.tracks[*]",
                        containsInAnyOrder(secondOrderCarrierTracks)));
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить треки по заказам с ролью REFEREE")
    @ParameterizedTest(name = "{index}")
    @MethodSource("parameterizedTestData")
    public void testClientRoleReferee(Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier) throws Exception {
        this.requestBuilderSupplier = requestBuilderSupplier;
        Matcher[] matchers = trackIdsByType.values().stream().flatMap(List::stream)
                .map(id -> hasEntry("id", id.intValue()))
                .toArray(Matcher[]::new);
        mockMvc.perform(builderWithClientRoleParams(ClientRole.REFEREE, REFEREE_CLIENT_ID, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].delivery.tracks[*]",
                        containsInAnyOrder(matchers)));
    }

    private MockHttpServletRequestBuilder builderWithClientRoleParams(ClientRole role, Long clientId, Long shopId) {
        return withClientParams(requestBuilderSupplier.get(), role, clientId, shopId);
    }

    private MockHttpServletRequestBuilder withClientParams(MockHttpServletRequestBuilder builder, ClientRole role,
                                                           Long clientId, Long shopId) {
        if (role != null) {
            builder.param(CheckouterClientParams.CLIENT_ROLE, role.name());
        }

        if (clientId != null) {
            builder.param(CheckouterClientParams.CLIENT_ID, clientId.toString());
        }

        if (shopId != null) {
            builder.param(CheckouterClientParams.SHOP_ID, shopId.toString());
        }

        builder.param(CheckouterClientParams.RGB, Color.BLUE.name());

        return builder;
    }
}
