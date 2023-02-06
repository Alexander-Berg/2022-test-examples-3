package ru.yandex.market.checkout.checkouter.warranty;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressLanguage;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.ShopOutletProvider;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.pdf.generator.PdfGeneratorService;
import ru.yandex.market.pdf.generator.PdfGeneratorServiceImpl;
import ru.yandex.market.shopinfo.SupplierInfo;
import ru.yandex.market.shopinfo.SupplierInfoService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@Disabled("Тест для отладки рендеринга PDF с гарантийником")
public class WarrantyPdfAdjustmentTest {

    public static final long SUPPLIER_ID = 123L;
    public static final long UNKNOWN_SUPPLIER_ID = 55L;
    @Mock
    private SupplierInfoService supplierInfoService;
    @Mock
    private PersonalDataService personalDataService;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;
    @Mock
    private CheckouterFeatureWriter checkouterFeatureWriter;
    private CheckouterProperties checkouterProperties = new CheckouterPropertiesImpl(checkouterFeatureReader,
            checkouterFeatureWriter);

    @BeforeEach
    public void setup() {
        initMocks(this);
        when(supplierInfoService.getShopInfo(anyLong())).thenReturn(Optional.empty());
        SupplierInfo info = new SupplierInfo();
        info.setOgrn("1027700229193");
        info.setSupplierName("ООО \"ЯНДЕКС\"");
        info.setShopPhoneNumber("+7(812)123-13-13");
        when(supplierInfoService.getShopInfo(SUPPLIER_ID)).thenReturn(Optional.of(info));
        when(personalDataService.getBuyerPhone(any(Buyer.class))).thenReturn("+70000000000");
    }

    @Test
    public void generateWarranty() throws Exception {
        PdfGeneratorService service = new PdfGeneratorServiceImpl();
        Order order = OrderProvider.getFulfilmentOrder();
        order.getDelivery().setBuyerAddress(createAddress());
        order.getDelivery().setPrice(BigDecimal.valueOf(200));
        order.setBuyer(createBuyer());
        order.setId(3456789L);
        order.setItems(createItems(5));
        setOrderTotalPrice(order);
        Warranty warranty = new WarrantyMapper(supplierInfoService, personalDataService, checkouterProperties)
                .map(order);
        try (FileOutputStream os = new FileOutputStream("warranty.pdf")) {
            service.generatePdf("beru.warranty", new ObjectMapper().writeValueAsString(warranty), os);
        }
    }

    @Test
    public void generateWarrantyNewLogo() throws Exception {
        PdfGeneratorService service = new PdfGeneratorServiceImpl();
        Order order = OrderProvider.getFulfilmentOrder();
        order.getDelivery().setBuyerAddress(createAddress());
        order.getDelivery().setPrice(BigDecimal.valueOf(200));
        order.setBuyer(createBuyer());
        order.setId(3456789L);
        order.setItems(createItems(5));
        setOrderTotalPrice(order);
        checkouterProperties.setNewLogoForWarranty(true);
        Warranty warranty = new WarrantyMapper(supplierInfoService, personalDataService, checkouterProperties)
                .map(order);
        try (FileOutputStream os = new FileOutputStream("warranty_new_logo.pdf")) {
            service.generatePdf("beru.warranty", new ObjectMapper().writeValueAsString(warranty), os);
        }
    }

    @Test
    public void generatePickupOrderWarranty() throws Exception {
        PdfGeneratorService service = new PdfGeneratorServiceImpl();
        Order order = OrderProvider.getFulfillmentOrderWithPickupType();
        order.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        order.getDelivery().setBuyerAddress(null);
        order.getDelivery().setShopAddress(null);
        order.getDelivery().setOutlet(ShopOutletProvider.getShopOutlet());
        order.getDelivery().setPrice(BigDecimal.valueOf(200));
        order.setBuyer(createBuyer());
        order.setId(3456789L);
        order.setItems(Stream.of(0, 1, 2, 3, 4).map(this::createSimpleItem).collect(Collectors.toList()));
        setOrderTotalPrice(order);
        Warranty warranty = new WarrantyMapper(supplierInfoService, personalDataService, checkouterProperties)
                .map(order);
        FileOutputStream os = new FileOutputStream("pickup.pdf");
        service.generatePdf("beru.warranty", new ObjectMapper().writeValueAsString(warranty), os);
        os.close();
    }

    @Test
    public void emptyWarrantyRendering() throws Exception {
        PdfGeneratorService service = new PdfGeneratorServiceImpl();

        Warranty warranty = new Warranty();

        FileOutputStream os = new FileOutputStream("empty.pdf");
        service.generatePdf("beru.warranty", new ObjectMapper().writeValueAsString(warranty), os);
        os.close();
    }

    @Test
    public void warrantyWithEmptyDeliveryBuyerAndItemRendering() throws Exception {
        PdfGeneratorService service = new PdfGeneratorServiceImpl();

        Warranty warranty = new Warranty();
        warranty.setItems(Collections.singletonList(new WarrantyItem()));
        warranty.setBuyer(new WarrantyBuyer());
        warranty.setDelivery(new WarrantyDelivery());

        FileOutputStream os = new FileOutputStream("dummy.pdf");
        service.generatePdf("beru.warranty", new ObjectMapper().writeValueAsString(warranty), os);
        os.close();
    }

    private Buyer createBuyer() {
        Buyer buyer = new Buyer();
        buyer.setLastName("Римский-Корсаков");
        buyer.setFirstName("Николай");
        buyer.setMiddleName("Андреевич");
        buyer.setPhone("+7 (921) 123-12-12");
        buyer.setEmail("niko-rome-corsa@yandex.ru");
        return buyer;
    }

    private Address createAddress() {
        AddressImpl address = new AddressImpl();
        address.setCountry("Россия");
        address.setPostcode("131488");
        address.setCity("Санкт-Петербург");
        address.setSubway("Невский проспект");
        address.setStreet("пер. Сергея Тюленина");
        address.setHouse("4");
        address.setBuilding("1");
        address.setBlock("444");
        address.setEntrance("404");
        address.setEntryPhone("007");
        address.setFloor("8");
        address.setApartment("22");
        address.setRecipient("000");
        address.setPhone("+7 921 111-11-11");
        address.setLanguage(AddressLanguage.RUS);
        return address;
    }

    private List<OrderItem> createItems(int n) {
        List<OrderItem> items = new ArrayList<>();
        items.add(createLongTitledItem());
        items.add(createItemWithLargeCount());
        items.add(createLongPriceAndCountItem());
        items.add(createItemWithoutSupplierId());
        for (int i = 0; i < n; i++) {
            items.add(createSimpleItem(i));
        }
        return items;
    }


    private OrderItem createLongTitledItem() {
        OrderItem item = OrderItemProvider.getOrderItem();
        item.setSupplierId(SUPPLIER_ID);
        item.setFeedOfferId(new FeedOfferId("Offer-1", 1234567L));
        item.setOfferName("Картридж CF283A OEM для LaserJet M125a Pro MFP (CZ172A),LaserJet M125R Pro,LaserJet " +
                "M125rnw Pro (CZ178A),LaserJet M125RA Pro,LaserJet M127 Pro,LaserJet M127fn Pro (CZ181A),LaserJet " +
                "M200 series,LaserJet M127fw Pro (CZ183A),LaserJet M125nw Pro,LaserJet M201dw Pro (CF456A),LaserJet " +
                "M201n Pro (CF455A),LaserJet M202dw Pro (C6N21A),LaserJet M202n Pro (C6N20A),LaserJet M225dn Pro MFP " +
                "(CF484A),LaserJet M225dw Pro MFP (CF485A),LaserJet M125,LaserJet M225 Pro MFP,LaserJet M225rdn Pro " +
                "MFP (CF486A),LaserJet M125 Pro");
        item.setCount(10);
        item.setBuyerPrice(BigDecimal.valueOf(1000L));
        return item;
    }

    private OrderItem createItemWithLargeCount() {
        OrderItem item = OrderItemProvider.getOrderItem();
        item.setSupplierId(SUPPLIER_ID);
        item.setFeedOfferId(new FeedOfferId("Offer-2", 1234567L));
        item.setOfferName("Ро");
        item.setCount(123456789);
        item.setBuyerPrice(BigDecimal.valueOf(9.99));
        return item;
    }

    private OrderItem createLongPriceAndCountItem() {
        OrderItem item = OrderItemProvider.getOrderItem();
        item.setSupplierId(SUPPLIER_ID);
        item.setFeedOfferId(new FeedOfferId("Offer-3", 1234567L));
        item.setOfferName("Масса для лепки Play-Doh Набор для праздника 10 банок (22037)");
        item.setCount(1);
        item.setBuyerPrice(BigDecimal.valueOf(1234567890));
        return item;
    }

    private OrderItem createSimpleItem(int i) {
        OrderItem item = OrderItemProvider.getOrderItem();
        item.setSupplierId(SUPPLIER_ID);
        item.setFeedOfferId(new FeedOfferId("SimpleOffer-" + i, 1234567L));
        item.setOfferName("Товар заглушка " + i);
        item.setCount(1);
        item.setBuyerPrice(BigDecimal.valueOf(99.99));
        return item;
    }

    private OrderItem createItemWithoutSupplierId() {
        OrderItem item = OrderItemProvider.getOrderItem();
        item.setSupplierId(UNKNOWN_SUPPLIER_ID);
        item.setFeedOfferId(new FeedOfferId("SimpleOfferWithUnknownSupplier", 1234567L));
        item.setOfferName("Нет информации о магазине");
        item.setCount(1);
        item.setBuyerPrice(BigDecimal.valueOf(99.99));
        return item;
    }

    public void setOrderTotalPrice(Order order) {
        order.setBuyerTotal(
                order.getItems().stream()
                        .map(orderItem -> orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getCount())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }
}
