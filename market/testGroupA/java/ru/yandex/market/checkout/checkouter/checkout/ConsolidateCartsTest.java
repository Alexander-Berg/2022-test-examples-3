package ru.yandex.market.checkout.checkouter.checkout;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ConsolidatedCarts;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.service.combinator.consolidatecarts.ConsolidateCartsResponse;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.bundles.controller.DsbsCheckoutControllerCartTest.requestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithShopDelivery;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_WAREHOUSE_ID;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class ConsolidateCartsTest extends AbstractWebTestBase {

    private static final Long DSBS_SHOP_ID = 774L;

    @Autowired
    private WireMockServer combinatorMock;

    @BeforeEach
    public void createOrder() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    @Test
    public void withRealCombiRequest() throws JsonProcessingException {
        createAndCheckWithCombinator();
    }

    @Test
    public void withFilteredDsbsCart() throws JsonProcessingException {
        var firstOffer = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .weight(null)
                .supplierId(DSBS_SHOP_ID)
                .offer(FIRST_OFFER)
                .price(1000);

        Order order = orderWithShopDelivery()
                .color(Color.WHITE)
                .itemBuilder(similar(firstOffer).price(900)).build();
        order.getDelivery().setBuyerAddress(AddressProvider.getAddress());
        MultiCart cart = MultiCartProvider.single(order);
        FoundOffer foundOffer = FoundOfferBuilder.createFrom(firstOffer.build())
                .build();
        foundOffer.setSupplierId(null);
        foundOffer.setSupplierType(null);
        foundOffer.setSupplierDescription(null);
        foundOffer.setSupplierWorkSchedule(null);
        foundOffer.setCpa(null);
        foundOffer.setWeight(null);
        foundOffer.setHeight(null);
        foundOffer.setWidth(null);
        foundOffer.setDepth(null);
        Parameters props = requestFor(cart, List.of(foundOffer));

        props.setFromDate(DeliveryDates.daysOffsetToDate(getClock(), 4));
        String label = props.getOrder().getLabel();

        var combinatorResponse = getConsolidateCartResponse(props.getOrder());
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/consolidate-carts"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));
        combinatorMock.resetRequests();

        MultiCart multiCart = orderCreateHelper.cart(props);

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> consolidateCartCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals("/consolidate-carts"))
                .collect(Collectors.toList());
        assertEquals(0, consolidateCartCalls.size());


        assertNotNull(multiCart.getGrouping());
        assertEquals(1, multiCart.getGrouping().size());
        ConsolidatedCarts group = multiCart.getGrouping().iterator().next();
        assertEquals(1, group.getCartLables().size());
        assertTrue(label.length() > 0);
        assertTrue(group.getCartLables().contains(label));
        assertEquals(1, group.getAvailableDates().size());
    }

    private void createAndCheckWithCombinator() throws JsonProcessingException {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setFromDate(DeliveryDates.daysOffsetToDate(getClock(), 4));
        parameters.setWarehouseForAllItems(TEST_WAREHOUSE_ID);
        Delivery delivery = DeliveryProvider.yandexDelivery().build();
        delivery.setBuyerAddress(AddressProvider.getAddress());
        parameters.getOrder().setDelivery(delivery);
        String label = parameters.getOrder().getLabel();

        var combinatorResponse = getConsolidateCartResponse(parameters.getOrder());
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/consolidate-carts"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));
        combinatorMock.resetRequests();
        MultiCart cart = orderCreateHelper.cart(parameters);

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> consolidateCartCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals("/consolidate-carts"))
                .collect(Collectors.toList());
        assertEquals(1, consolidateCartCalls.size());

        assertNotNull(cart.getGrouping());
        assertEquals(1, cart.getGrouping().size());
        ConsolidatedCarts group = cart.getGrouping().iterator().next();
        assertEquals(1, group.getCartLables().size());
        assertTrue(label.length() > 0);
        assertTrue(group.getCartLables().contains(label));
        assertEquals(1, group.getAvailableDates().size());
    }

    private ConsolidateCartsResponse getConsolidateCartResponse(Order order) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        var resp = new ConsolidateCartsResponse();
        var group = new ConsolidateCartsResponse.ConsolidateCartGroup();
        group.setCartLables(List.of(order.getLabel()));
        group.setAvailableDates(List.of(
                sdf.format(DeliveryDates.daysOffsetToDate(getClock(), 4))
        ));
        resp.setGroups(List.of(group));

        return resp;
    }

}
