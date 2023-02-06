package ru.yandex.market.checkout.checkouter.yauslugi.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.FullName;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersGps;
import ru.yandex.market.checkout.checkouter.yauslugi.model.AddressDto;
import ru.yandex.market.checkout.checkouter.yauslugi.model.ClientDto;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.RecipientProvider;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

public class YaServiceDtoUtilsTest {

    private final PersonalDataService personalDataService = Mockito.mock(PersonalDataService.class);

    @Test
    public void testPickupDeliveryType() {
        var delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryDates(DeliveryProvider.getDeliveryDates());
        var outlet = new ShopOutlet();
        outlet.setGps("1,2");
        delivery.setOutlet(outlet);
        var order = new Order();
        order.setDelivery(delivery);
        ItemService itemService = ItemServiceProvider.defaultItemService();
        OrderItem orderItem = OrderItemProvider.defaultOrderItem();
        orderItem.getServices().add(itemService);
        order.setItems(singleton(orderItem));

        Mockito.when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null,
                        PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress()),
                        PersGps.convertToPersonal("1,2")));

        Mockito.when(personalDataService.getPersGps(any())).thenReturn(PersGps.convertToPersonal("1,2"));

        var actual = YaServiceDtoUtils.createYaServiceDto(itemService, order,
                TestableClock.systemDefaultZone(), personalDataService, null);
        assertEquals(DeliveryType.PICKUP, actual.getDeliveryType());
        assertNotNull(actual.getAddress());
        assertEquals("1.0,2.0", actual.getAddress().getGps());
    }

    @Test
    public void testDeliveryDeliveryType() {
        var delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(DeliveryProvider.getDeliveryDates());
        var outlet = new ShopOutlet();
        outlet.setGps("1,2");
        delivery.setOutlet(outlet);
        Address address = AddressProvider.getAddress();
        ((AddressImpl) address).setPersonalAddressId(null);
        ((AddressImpl) address).setPersonalGpsId(null);

        delivery.setBuyerAddress(address);
        var order = new Order();
        order.setDelivery(delivery);
        ItemService itemService = ItemServiceProvider.defaultItemService();
        OrderItem orderItem = OrderItemProvider.defaultOrderItem();
        orderItem.getServices().add(itemService);
        order.setItems(singleton(orderItem));

        Mockito.when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(new FullName().forename("Leo").surname("Tolstoy"),
                        null, "leo@ya.ru", PersAddress.convertToPersonal(address),
                        null));

        Mockito.when(personalDataService.getPersGps(any())).thenReturn(null);

        var actual = YaServiceDtoUtils.createYaServiceDto(itemService, order,
                TestableClock.systemDefaultZone(), personalDataService, null);
        assertEquals(DeliveryType.DELIVERY, actual.getDeliveryType());
        assertNotNull(actual.getAddress());
        var actualAddress = actual.getAddress();
        checkAddress(delivery.getBuyerAddress(), actualAddress);
    }

    @Test
    public void testRecipient() {
        var delivery = new Delivery();
        delivery.setRecipient(RecipientProvider.getDefaultRecipient());
        delivery.setDeliveryDates(DeliveryProvider.getDeliveryDates());
        var order = new Order();
        order.setDelivery(delivery);
        order.setBuyer(BuyerProvider.getBuyer());
        ItemService itemService = ItemServiceProvider.defaultItemService();
        OrderItem orderItem = OrderItemProvider.defaultOrderItem();
        orderItem.getServices().add(itemService);
        order.setItems(singleton(orderItem));

        Mockito.when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(
                        new FullName().forename("Leo").surname("Tolstoy"),
                        "+71234567891", "leo@ya.ru",
                        new PersAddress(), new PersGps()));

        Mockito.when(personalDataService.getPersGps(any())).thenReturn(null);

        var actual = YaServiceDtoUtils.createYaServiceDto(itemService, order,
                TestableClock.systemDefaultZone(), personalDataService, null);

        var expectedClient = new ClientDto();
        expectedClient.setFirstName("Leo");
        expectedClient.setLastName("Tolstoy");
        expectedClient.setMiddleName(null);
        expectedClient.setPhone("+71234567891");
        expectedClient.setEmail("leo@ya.ru");
        checkClientDto(expectedClient, actual.getClient());
    }

    @Test
    public void testRecipientIsNotPresent() {
        var delivery = new Delivery();
        delivery.setDeliveryDates(DeliveryProvider.getDeliveryDates());
        delivery.setRecipient(null);
        var order = new Order();
        order.setDelivery(delivery);
        var buyer = BuyerProvider.getBuyer();
        buyer.setFirstName("BuyerName");
        buyer.setLastName("BuyerLastName");
        buyer.setMiddleName("BuyerMiddleName");
        buyer.setPhone("+1234567890");
        buyer.setEmail("buyer@mail.ru");
        order.setBuyer(buyer);
        ItemService itemService = ItemServiceProvider.defaultItemService();
        OrderItem orderItem = OrderItemProvider.defaultOrderItem();
        orderItem.getServices().add(itemService);
        order.setItems(singleton(orderItem));

        Mockito.when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(
                        new FullName().forename("BuyerName").surname("BuyerLastName").patronymic("BuyerMiddleName"),
                        "+1234567890", "buyer@mail.ru", new PersAddress(), new PersGps()));

        Mockito.when(personalDataService.getPersGps(any())).thenReturn(null);

        var actual = YaServiceDtoUtils.createYaServiceDto(itemService, order,
                TestableClock.systemDefaultZone(), personalDataService, null);

        var expectedClient = new ClientDto();
        expectedClient.setFirstName("BuyerName");
        expectedClient.setMiddleName("BuyerMiddleName");
        expectedClient.setLastName("BuyerLastName");
        expectedClient.setPhone("+1234567890");
        expectedClient.setEmail("buyer@mail.ru");
        checkClientDto(expectedClient, actual.getClient());
    }

    private void checkAddress(Address expected, AddressDto actual) {
        assertEquals(expected.getCountry(), actual.getCountry());
        assertEquals(expected.getPostcode(), actual.getPostcode());
        assertEquals(expected.getCity(), actual.getCity());
        assertEquals(expected.getDistrict(), actual.getDistrict());
        assertEquals(expected.getSubway(), actual.getSubway());
        assertEquals(expected.getStreet(), actual.getStreet());
        assertEquals(expected.getHouse(), actual.getHouse());
        assertEquals(expected.getBlock(), actual.getBlock());
        assertEquals(expected.getEntrance(), actual.getEntrance());
        assertEquals(expected.getEntryPhone(), actual.getEntryPhone());
        assertEquals(expected.getFloor(), actual.getFloor());
        assertEquals(expected.getRecipientPerson().getFirstName(), actual.getRecipientFirstName());
        assertEquals(expected.getRecipientPerson().getLastName(), actual.getRecipientLastName());
        assertEquals(expected.getApartment(), actual.getApartment());
        assertEquals(expected.getRecipientEmail(), actual.getRecipientEmail());
        assertEquals(expected.getGps(), actual.getGps());
    }

    private void checkClientDto(ClientDto expected, ClientDto actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getMiddleName(), actual.getMiddleName());
        assertEquals(expected.getPhone(), actual.getPhone());
        assertEquals(expected.getEmail(), actual.getEmail());
    }
}
