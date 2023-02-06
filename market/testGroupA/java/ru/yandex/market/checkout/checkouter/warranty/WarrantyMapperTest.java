package ru.yandex.market.checkout.checkouter.warranty;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveRequestBuilder;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.FullName;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersGps;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.shopinfo.SupplierInfo;
import ru.yandex.market.shopinfo.SupplierInfoService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


public class WarrantyMapperTest {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    public static final String SUPPLIER_NAME = "ООО \"ЯНДЕКС\"";
    public static final String SUPPLIER_OGRN = "1027700229193";
    public static final String SUPPLIER_PHONE = "+7(812)123-13-13";
    public static final String BUYER_FIRST_NAME = "Leo";
    public static final String BUYER_MIDDLE_NAME = "N";
    public static final String BUYER_LAST_NAME = "Tolstoy";
    public static final String BUYER_EMAIL = "a@b.com";
    public static final String BUYER_PHONE = "+70000000000";
    public static final String BUYER_GPS = "123,432";
    @Mock
    private SupplierInfoService supplierInfoService;
    @Mock
    private PersonalDataService personalDataService;
    private WarrantyMapper warrantyMapper;

    @BeforeEach
    public void setup() {
        initMocks(this);
        mockSupplierInfoService();
        mockPersonalDataService();

        warrantyMapper = new WarrantyMapper(supplierInfoService, personalDataService, new CheckouterPropertiesImpl());
    }

    @Test
    public void testMapper() {
        Order order = OrderProvider.getFulfilmentOrder();
        order.setRgb(Color.BLUE);
        Warranty warranty = warrantyMapper.map(order);
        assertThat(warranty.getPaymentType(), equalTo(order.getPaymentMethod().getPrintableName()));
        assertThat(warranty.getPrice(), equalTo(order.getBuyerTotal()));
        assertThat(warranty.getCreationDate(), equalTo(DATE_FORMAT.format(order.getCreationDate())));
        assertThat(warranty.getOrderId(), equalTo(order.getId()));

        assertBuyer(warranty.getBuyer(), order.getBuyer());
        assertDelivery(warranty.getDelivery(), order.getDelivery());
        assertItems(warranty.getItems(), order.getItems());
    }

    @Test
    public void testShopAddress() {
        Order order = OrderProvider.getFulfilmentOrder();
        order.getDelivery().setBuyerAddress(null);
        order.setRgb(Color.BLUE);
        Warranty warranty = warrantyMapper.map(order);
        assertDelivery(warranty.getDelivery(), order.getDelivery());
    }

    @Test
    public void testNoOutletAddressOrAnything() {
        Order order = OrderProvider.getFulfillmentOrderWithPickupType();
        order.getDelivery().setBuyerAddress(null);
        order.getDelivery().setShopAddress(null);
        order.getDelivery().setOutlet(null);
        Warranty warranty = warrantyMapper.map(order);
        assertThat(warranty.getDelivery().getAddress(), isEmptyOrNullString());
    }

    private void assertBuyer(WarrantyBuyer buyer, Buyer orderBuyer) {
        assertThat(buyer, notNullValue());
        assertThat(buyer.getFirstName(), equalTo(BUYER_FIRST_NAME));
        assertThat(buyer.getLastName(), equalTo(BUYER_LAST_NAME));
        assertThat(buyer.getPatronymic(), equalTo(BUYER_MIDDLE_NAME));
        assertThat(buyer.getEmail(), equalTo(BUYER_EMAIL));
        assertThat(buyer.getPhoneNumber(), equalTo(BUYER_PHONE));
    }

    private void assertDelivery(WarrantyDelivery delivery, Delivery orderDelivery) {
        assertThat(delivery, notNullValue());
        if (orderDelivery.getBuyerAddress() != null) {
            assertAddress(delivery.getAddress(), orderDelivery.getBuyerAddress());
        } else {
            assertAddress(delivery.getAddress(), orderDelivery.getShopAddress());
        }
        assertThat(delivery.getType(), equalTo(orderDelivery.getType().getPrintableName()));
        assertThat(delivery.getPrice(), equalTo(orderDelivery.getPrice()));
    }

    private void assertAddress(String actualAddress, Address expectedAddress) {
        assertThat(actualAddress, containsString(expectedAddress.getCity()));
        assertThat(actualAddress, containsString(expectedAddress.getStreet()));
        assertThat(actualAddress, containsString(expectedAddress.getBuilding()));
        assertThat(actualAddress, containsString(expectedAddress.getHouse()));
        assertThat(actualAddress, containsString(expectedAddress.getApartment()));
    }


    private void assertItems(List<WarrantyItem> items, Collection<OrderItem> orderItems) {
        assertThat(items, not(empty()));
        assertThat(items, hasSize(orderItems.size()));
        items.forEach(i -> {
            assertThat(i.getSupplierName(), equalTo(SUPPLIER_NAME));
            assertThat(i.getSupplierOgrn(), equalTo(SUPPLIER_OGRN));
            assertThat(i.getSupplierPhone(), equalTo(SUPPLIER_PHONE));
        });
    }

    private void mockSupplierInfoService() {
        SupplierInfo info = new SupplierInfo();
        info.setOgrn(SUPPLIER_OGRN);
        info.setSupplierName(SUPPLIER_NAME);
        info.setShopPhoneNumber(SUPPLIER_PHONE);
        when(supplierInfoService.getShopInfo(anyLong())).thenReturn(Optional.of(info));
    }

    private void mockPersonalDataService() {
        Address preciseRegionId = AddressProvider.getAddressWithPreciseRegionId();

        PersAddress address = new PersAddress();
        address.setCity(preciseRegionId.getCity());
        address.setStreet(preciseRegionId.getStreet());
        address.setBuilding(preciseRegionId.getBuilding());
        address.setHouse(preciseRegionId.getHouse());
        address.setApartment(preciseRegionId.getApartment());

        when(personalDataService.retrieve(any(PersonalDataRetrieveRequestBuilder.class)))
                .thenReturn(new PersonalDataRetrieveResult(
                        new FullName().forename(BUYER_FIRST_NAME)
                                .patronymic(BUYER_MIDDLE_NAME).surname(BUYER_LAST_NAME),
                        BUYER_PHONE, BUYER_EMAIL, address, new PersGps()));

        when(personalDataService.getPersAddress(any())).thenReturn(address);
    }
}
