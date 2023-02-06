package ru.yandex.market.checkout.checkouter.service.combinator.consolidatecarts;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.CollectionUtils;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalAndDate;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableMultiCart;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.CommonRequestBuilder;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.DeliveryDestinationDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.GpsCoordinatesDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.OfferDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.OrderItemDto;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveRequestBuilder;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersGps;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsolidateCartsRequestFactoryTest {

    private static final String DEFAULT_LATITUDE = "55.733742";
    private static final String DEFAULT_LONGITUDE = "37.588244";

    private final PersonalDataService personalDataService = Mockito.mock(PersonalDataService.class);

    @Test
    public void buildRequestTest() {
        defaultPersReturn();
        MultiCart mc = new MultiCart();
        mc.setCarts(new ArrayList<>());
        Order cart1 = new Order();
        Delivery d1 = new Delivery(213L);
        d1.setBuyerAddress(buildAddress());
        d1.setType(DeliveryType.DELIVERY);
        cart1.setDelivery(d1);
        cart1.setLabel("cart1");
        cart1.setShopId(12L);
        var i1 = new OrderItem();
        i1.setCount(1);
        i1.setWeight(2000L);
        i1.setHeight(10L);
        i1.setWidth(20L);
        i1.setDepth(30L);
        i1.setWarehouseId(123);
        cart1.setItems(List.of(i1));
        var do1 = new Delivery();
        do1.setType(DeliveryType.DELIVERY);
        var rawInt1 = new RawDeliveryInterval(createDate(22, 2, 2022),
                LocalTime.of(10, 0, 0), LocalTime.of(14, 0, 0));
        var int1 = new RawDeliveryIntervalAndDate(createDate(22, 2, 2022), List.of(rawInt1));
        do1.setRawDeliveryIntervals(new RawDeliveryIntervalsCollection(List.of(int1)));
        cart1.setDeliveryOptions(List.of(do1));
        mc.getCarts().add(cart1);


        var expectedRequest = new ConsolidateCartsRequest();
        var carDest = new CartsAndDestination();
        DeliveryDestinationDto dest2 = new DeliveryDestinationDto();
        dest2.setRegionId(213L);
        dest2.setGpsCoords(
                new GpsCoordinatesDto(Double.parseDouble(DEFAULT_LATITUDE), Double.parseDouble(DEFAULT_LONGITUDE)));

        carDest.setDestination(dest2);
        var cart2 = new CartsAndDestination.Cart();
        cart2.setDates(Set.of(CommonRequestBuilder.buildDate(createDate(22, 2, 2022))));
        cart2.setLabel("cart1");
        var item2 = new OrderItemDto();
        item2.setDimensions(new Long[]{10L, 20L, 30L});
        item2.setRequiredCount(1);
        item2.setWeight(2000L);
        var off2 = new OfferDto();
        off2.setAvailableCount(1);
        off2.setPartnerId(123);
        off2.setShopId(12L);
        item2.setAvailableOffers(List.of(off2));
        cart2.setItems(List.of(item2));
        carDest.setCarts(List.of(cart2));
        expectedRequest.setCartsByDest(List.of(carDest));

        // Act
        var request = ConsolidateCartsRequestFactory
                .buildRequest(ImmutableMultiCart.from(mc), personalDataService);

        // Assert
        assertEquals(expectedRequest, request);
    }

    @Test
    public void buildRequestWithoutDatesForPickup() {
        defaultPersReturn();
        Order order = OrderProvider.getBlueOrder();
        order.getDelivery().setBuyerAddress(buildAddress());
        Delivery delivery = DeliveryProvider.yandexPickupDelivery().build();
        var rawIntervalAndDate = new RawDeliveryInterval(createDate(22, 2, 2022),
                LocalTime.of(10, 0, 0), LocalTime.of(14, 0, 0));
        var intervalAndDate = new RawDeliveryIntervalAndDate(createDate(22, 2, 2022),
                List.of(rawIntervalAndDate));
        delivery.setRawDeliveryIntervals(new RawDeliveryIntervalsCollection(List.of(intervalAndDate)));
        order.setDeliveryOptions(List.of(delivery));
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order));

        var result = ConsolidateCartsRequestFactory
                .buildRequest(ImmutableMultiCart.from(multiCart), personalDataService);

        assertTrue(CollectionUtils.isEmpty(
                result.getCartsByDest().iterator().next().getCarts().iterator().next().getDates()));
    }

    @Test
    public void buildRequestWithoutDatesForOnDemand() {
        defaultPersReturn();
        Order order = OrderProvider.getBlueOrder();
        order.getDelivery().setBuyerAddress(buildAddress());
        Delivery delivery = DeliveryProvider.yandexDelivery().build();
        delivery.addFeature(DeliveryFeature.ON_DEMAND);
        var rawIntervalAndDate = new RawDeliveryInterval(createDate(22, 2, 2022),
                LocalTime.of(10, 0, 0), LocalTime.of(14, 0, 0));
        var intervalAndDate = new RawDeliveryIntervalAndDate(createDate(22, 2, 2022),
                List.of(rawIntervalAndDate));
        delivery.setRawDeliveryIntervals(new RawDeliveryIntervalsCollection(List.of(intervalAndDate)));
        order.setDeliveryOptions(List.of(delivery));
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order));

        var result = ConsolidateCartsRequestFactory
                .buildRequest(ImmutableMultiCart.from(multiCart), personalDataService);

        assertTrue(CollectionUtils.isEmpty(
                result.getCartsByDest().iterator().next().getCarts().iterator().next().getDates()));
    }

    @Test
    public void buildRequestWithoutCartForOnDemand() {
        defaultPersReturn();
        Order order = OrderProvider.getBlueOrder();
        Delivery delivery = DeliveryProvider.yandexDelivery().build();
        delivery.setBuyerAddress(buildAddress());
        delivery.addFeature(DeliveryFeature.ON_DEMAND);
        order.setDelivery(delivery);
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order));

        var result = ConsolidateCartsRequestFactory
                .buildRequest(ImmutableMultiCart.from(multiCart), personalDataService);
        assertTrue(CollectionUtils.isEmpty(result.getCartsByDest()));
    }

    @Test
    public void buildRequestWithoutCartForAddressWithoutGps() {
        Mockito.when(personalDataService.retrieve(Mockito.any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null, null, null));

        Order order = OrderProvider.getBlueOrder();
        Delivery delivery = DeliveryProvider.yandexDelivery().build();
        AddressImpl address = (AddressImpl) AddressProvider.getAddress();
        address.setGps(null);
        address.setPersonalGpsId(null);
        delivery.setBuyerAddress(address);
        order.setDelivery(delivery);
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order));

        var result = ConsolidateCartsRequestFactory
                .buildRequest(ImmutableMultiCart.from(multiCart), personalDataService);
        assertTrue(CollectionUtils.isEmpty(result.getCartsByDest()));
    }

    @Test
    public void shouldNotFilterRequestCartWithoutDeliveryType() {
        defaultPersReturn();
        Order order = OrderProvider.getBlueOrder();
        order.setLabel("label1");
        Delivery delivery = new Delivery();
        delivery.setBuyerAddress(buildAddress());
        order.setDelivery(delivery);
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order));

        var result = ConsolidateCartsRequestFactory
                .buildRequest(ImmutableMultiCart.from(multiCart), personalDataService);
        assertEquals(1, result.getCartsByDest().size());
        assertEquals(order.getLabel(),
                result.getCartsByDest().iterator().next().getCarts().iterator().next().getLabel());
    }

    @Test
    public void shouldBuildPreciseRegion() {
        defaultPersReturn();
        long preciseRegionId = 123112L;
        Order order = OrderProvider.getBlueOrder();
        order.setLabel("label1");
        Delivery delivery = new Delivery();
        AddressImpl buyerAddress = buildAddress();
        buyerAddress.setPreciseRegionId(preciseRegionId);
        delivery.setBuyerAddress(buyerAddress);
        order.setDelivery(delivery);
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order));

        var result = ConsolidateCartsRequestFactory
                .buildRequest(ImmutableMultiCart.from(multiCart), personalDataService);
        assertEquals(1, result.getCartsByDest().size());
        assertEquals(preciseRegionId,
                result.getCartsByDest().iterator().next().getDestination().getRegionId());
    }


    private static Date createDate(int day, int month, int year) {
        return Date.from(LocalDate.of(year, month, day)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
    }

    @NotNull
    private AddressImpl buildAddress() {
        AddressImpl address = (AddressImpl) AddressProvider.getAddress();
        address.setGps(DEFAULT_LONGITUDE + "," + DEFAULT_LATITUDE);
        return address;
    }

    private void defaultPersReturn() {
        Mockito.when(personalDataService.retrieve(
                        PersonalDataRetrieveRequestBuilder.create()
                                .withGpsId(Mockito.any(),
                                        new PersGps().longitude(DEFAULT_LONGITUDE).latitude(DEFAULT_LATITUDE))))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null, null,
                        new PersGps().longitude(DEFAULT_LONGITUDE).latitude(DEFAULT_LATITUDE)));
    }
}
