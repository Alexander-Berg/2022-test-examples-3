package ru.yandex.market.rg.asyncreport.shipment.labels;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.DbDeliveryInfoService;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdPartner;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.LabelInfo;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;
import ru.yandex.market.rg.asyncreport.ReportFunctionalTest;
import ru.yandex.market.rg.asyncreport.shipment.FirstMileCheckouterService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "PartnerShipmentOrdersLabels.before.csv")
class PartnerShipmentOrdersPrepareLabelsReportGeneratorTest extends ReportFunctionalTest {

    private static final long SUPPLIER_ID = 101L;
    private static final long DELIVERY_ID = 48L;
    private static final long SORTING_CENTER_ID = 48103L;
    private static final String LEGAL_NAME = "ООО Рога и Копыта";
    private static final String SHOP_NAME = "Рога и Копыта";
    private static final String DELIVERY_NAME = "ООО PickPoint";
    private static final String SC_NAME = "СЦ ПЭК";

    @Autowired
    private CheckouterClient checkouterClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private WwClient wwClient;
    @Autowired
    private FirstMileCheckouterService firstMileCheckouterService;
    @Autowired
    private MarketIdGrpcService marketIdGrpcService;
    @Autowired
    private DbDeliveryInfoService dbDeliveryInfoService;
    private PartnerShipmentOrdersPrepareLabelsReportGenerator generator;

    @BeforeEach
    void init() {
        generator = new PartnerShipmentOrdersPrepareLabelsReportGenerator(wwClient, firstMileCheckouterService,
                marketIdGrpcService, dbDeliveryInfoService);
    }

    @Test
    @Disabled("Исправить после того как перейдем на новый шаблон DBS ярлыка")
    void testCorrectLabelsGenerationA4() throws IOException {
        mockCheckouterClient();
        mockMarketId();
        mockWWClient();
        checkFileContent(PageSize.A4);
    }

    @Test
    @Disabled("Исправить после того как перейдем на новый шаблон DBS ярлыка")
    void testCorrectLabelsGenerationA6() throws IOException {
        mockCheckouterClient();
        mockMarketId();
        mockWWClient();
        checkFileContent(PageSize.A6);
    }

    private void mockCheckouterClient() {
        Order order1 = new Order();
        //доставка идет через СЦ и в пункт выдачи
        OrderItem orderItem11 = new OrderItem();
        OrderItem orderItem12 = new OrderItem();
        orderItem11.setFulfilmentWarehouseId(SORTING_CENTER_ID);
        orderItem12.setFulfilmentWarehouseId(SORTING_CENTER_ID);
        orderItem11.setWarehouseId(44777);
        orderItem12.setWarehouseId(44777);
        order1.setItems(List.of(orderItem11, orderItem12));

        Delivery delivery1 = new Delivery();
        delivery1.setDeliveryServiceId(DELIVERY_ID);
        delivery1.setType(DeliveryType.PICKUP);

        Parcel parcel101 = new Parcel();
        parcel101.setId(1L);
        parcel101.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 5, 10, 13, 30, 15));

        ParcelBox parcelBox101 = new ParcelBox();
        parcelBox101.setId(101101L);
        parcelBox101.setFulfilmentId("1-1");
        parcelBox101.setExternalId("1011");
        parcelBox101.setWeight(2100L);

        ParcelBox parcelBox1012 = new ParcelBox();
        parcelBox1012.setId(1011012L);
        parcelBox1012.setFulfilmentId("1-2");
        parcelBox1012.setExternalId("10122");
        parcelBox1012.setWeight(3000L);

        parcel101.setBoxes(List.of(parcelBox101, parcelBox1012));

        Parcel parcel106 = new Parcel();
        parcel106.setId(2L);
        parcel106.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 5, 10, 12, 0, 15));

        ParcelBox parcelBox106 = new ParcelBox();
        parcelBox106.setId(106106L);
        parcelBox106.setExternalId("106106");
        parcelBox106.setWeight(400L);
        parcel106.setBoxes(Collections.singletonList(parcelBox106));

        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setCity("Москва");
        shopOutlet.setStreet("Южная улица");
        shopOutlet.setHouse("13");
        shopOutlet.setBuilding("1");
        shopOutlet.setPostcode("147115");
        delivery1.setOutlet(shopOutlet);
        delivery1.setParcels(List.of(parcel101, parcel106));

        Buyer buyer1 = new Buyer();
        buyer1.setLastName("Иванов");
        buyer1.setFirstName("Иван");
        buyer1.setMiddleName("Иванович");
        buyer1.setPhone("+7 985 210-14-21");

        AddressImpl address1 = new AddressImpl();
        address1.setCountry("Россия");
        address1.setCity("Москва");
        address1.setStreet("ул.Победы");
        address1.setHouse("20");
        address1.setApartment("18");
        address1.setPostcode("115578");
        delivery1.setBuyerAddress(address1);

        order1.setShopId(SUPPLIER_ID);
        order1.setId(1L);
        order1.setStatus(OrderStatus.PROCESSING);
        order1.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        order1.setShopOrderId("номер1");
        order1.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order1.setBuyer(buyer1);
        order1.setDelivery(delivery1);
        order1.setShopName(SHOP_NAME);

        Order order2 = new Order();
        Delivery delivery2 = new Delivery();
        delivery2.setDeliveryServiceId(DELIVERY_ID);

        Parcel parcel202 = new Parcel();
        parcel202.setId(22L);
        parcel202.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 5, 9, 18, 30, 15));

        ParcelBox parcelBox2222 = new ParcelBox();
        parcelBox2222.setId(2222L);
        parcelBox2222.setExternalId("10222");
        parcelBox2222.setWeight(500L);
        parcel202.setBoxes(Collections.singletonList(parcelBox2222));

        AddressImpl address2 = new AddressImpl();
        address2.setCountry("Russia");
        address2.setCity("Irkutsk");
        address2.setStreet("Baykalskaya");
        address2.setHouse("76");
        address2.setBuilding("2");
        address2.setApartment("124");
        address2.setPostcode("654456");
        delivery2.setBuyerAddress(address2);
        delivery2.setParcels(Collections.singletonList(parcel202));

        // доставка идет через службу доставки курьером
        OrderItem orderItem21 = new OrderItem();
        OrderItem orderItem22 = new OrderItem();
        orderItem21.setFulfilmentWarehouseId(44777L);
        orderItem22.setFulfilmentWarehouseId(44777L);
        orderItem21.setWarehouseId(44777);
        orderItem22.setWarehouseId(44777);

        Buyer buyer2 = new Buyer();
        buyer2.setLastName("Сидоров");
        buyer2.setFirstName("Сидр");
        buyer2.setMiddleName("Сидорович");
        buyer2.setPhone("+7 985 210-14-29");

        order2.setShopId(SUPPLIER_ID);
        order2.setId(2L);
        order2.setStatus(OrderStatus.PROCESSING);
        order2.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        order2.setShopOrderId("номер2");
        order2.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order2.setBuyer(buyer2);
        order2.setDelivery(delivery2);
        order2.setShopName(SHOP_NAME);

        Order order3 = new Order();
        order3.setShopId(SUPPLIER_ID);
        order3.setId(3L);
        order3.setStatus(OrderStatus.PROCESSING);
        order3.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        order3.setShopOrderId("номер3");
        order3.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order3.setShopName(SHOP_NAME);
        Delivery delivery3 = new Delivery();
        order3.setDelivery(delivery3);

        Mockito.when(checkouterClient.getOrders(Mockito.any(), Mockito.any()))
                .thenReturn(new PagedOrders(List.of(order1, order2, order3),
                        Pager.atPage(1, 50)));
    }

    private void mockMarketId() {
        MarketIdPartner partner = MarketIdPartner.newBuilder()
                .setPartnerId(SUPPLIER_ID)
                .setPartnerType(CampaignType.SUPPLIER.getId())
                .build();
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder().setPartner(partner).build();
        // mock ответа MarketID на запрос о ид поставщика
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            MarketAccount marketAccount = MarketAccount.newBuilder().setLegalInfo(
                    LegalInfo.newBuilder()
                            .setLegalName(LEGAL_NAME)
                            .setType("OOO")
                            .build()
            ).build();
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(eq(request), any());

        MarketIdPartner partnerDelivery = MarketIdPartner.newBuilder()
                .setPartnerId(DELIVERY_ID)
                .setPartnerType(CampaignType.DELIVERY.getId())
                .build();
        GetByPartnerRequest request1 = GetByPartnerRequest.newBuilder().setPartner(partnerDelivery).build();
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            MarketAccount marketAccount1 = MarketAccount.newBuilder().setLegalInfo(
                    LegalInfo.newBuilder()
                            .setLegalName(DELIVERY_NAME)
                            .setType("OOO")
                            .build()
            ).build();
            GetByPartnerResponse response1 = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount1).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response1);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(eq(request1), any());

        MarketIdPartner partnerSc = MarketIdPartner.newBuilder()
                .setPartnerId(SORTING_CENTER_ID)
                .setPartnerType(CampaignType.YADELIVERY.getId())
                .build();
        GetByPartnerRequest request2 = GetByPartnerRequest.newBuilder().setPartner(partnerSc).build();
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            MarketAccount marketAccount2 = MarketAccount.newBuilder().setLegalInfo(
                    LegalInfo.newBuilder()
                            .setLegalName(SC_NAME)
                            .setType("ЗАО")
                            .build()
            ).build();
            GetByPartnerResponse response2 = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount2).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response2);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(eq(request2), any());
    }

    private void mockWWClient() {
        when(wwClient.generateLabels(any(), any(), any())).thenReturn(new byte[0]);
    }

    private PartnerShipmentOrderLabelsReportParams buildParamsWithPageSize(PageSize pageSize) {
        return new PartnerShipmentOrderLabelsReportParams(SUPPLIER_ID, List.of(1L, 2L), null, pageSize);
    }

    private void checkFileContent(PageSize pageSize) throws IOException {
        var params = buildParamsWithPageSize(pageSize);
        File reportFile = TempFileUtils.createTempFile("tmpReport", ".pdf");
        ArgumentCaptor<List> labelInfoCaptor = ArgumentCaptor.forClass(List.class);
        try (OutputStream output = new FileOutputStream(reportFile)) {
            generator.generateReport(params, output);
            verify(wwClient).generateLabels(labelInfoCaptor.capture(), any(), eq(pageSize));
            final List<LabelInfo> capturedLabelInfo = labelInfoCaptor.getValue();
            verifyLabelsInfo(capturedLabelInfo);
        }
    }

    private void verifyLabelsInfo(List<LabelInfo> labels) {
        assertThat(labels.size()).isEqualTo(4);

        Set<Long> platformClientIds = labels.stream().map(LabelInfo::getPlatformClientId)
                .collect(Collectors.toSet());
        assertThat(platformClientIds.size()).isEqualTo(1);
        assertThat(platformClientIds.contains(1L)).isTrue();

        Set<String> barcodes = labels.stream().map(LabelInfo::getBarcode)
                .collect(Collectors.toSet());
        assertThat(barcodes).containsExactlyInAnyOrder("1", "2");

        List<LabelInfo.PartnerInfo> sc_order_1 = labels.stream()
                .filter(labelInfo -> labelInfo.getBarcode().equals("1"))
                .map(LabelInfo::getSortingCenter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(sc_order_1.get(0).getReadableName()).isEqualTo(SC_NAME);

        List<LabelInfo.PartnerInfo> sc_order_2 = labels.stream()
                .filter(labelInfo -> labelInfo.getBarcode().equals("2"))
                .map(LabelInfo::getSortingCenter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(sc_order_2).isEmpty();

        List<LabelInfo.PartnerInfo> ds_order_1 = labels.stream()
                .filter(labelInfo -> labelInfo.getBarcode().equals("1"))
                .map(LabelInfo::getDeliveryService)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(ds_order_1.get(0).getReadableName()).isEqualTo(SC_NAME);
        assertThat(ds_order_1.get(0).getLegalName()).isEqualTo("ЗАО " + SC_NAME);

        List<LabelInfo.PartnerInfo> ds_order_2 = labels.stream()
                .filter(labelInfo -> labelInfo.getBarcode().equals("2"))
                .map(LabelInfo::getDeliveryService)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(ds_order_2.get(0).getReadableName()).isEqualTo("PP");
        assertThat(ds_order_2.get(0).getLegalName()).isEqualTo(DELIVERY_NAME);

        List<LabelInfo.PlaceInfo> places_1 = labels.stream()
                .filter(labelInfo -> labelInfo.getBarcode().equals("1"))
                .map(LabelInfo::getPlace)
                .collect(Collectors.toList());

        List<String> place1_ext_ids = places_1.stream()
                .map(LabelInfo.PlaceInfo::getExternalId).collect(Collectors.toList());
        assertThat(place1_ext_ids).containsExactlyInAnyOrder("1011", "10122", "106106");
        List<LabelInfo.PlaceInfo> place1011 = places_1.stream()
                .filter(placeInfo -> placeInfo.getExternalId().equals(
                        "1011"))
                .collect(Collectors.toList());
        assertThat(place1011.size()).isEqualTo(1);
        assertThat(place1011.get(0).getPlacesCount()).isEqualTo(2);
        assertThat(place1011.get(0).getWeight()).isEqualTo(BigDecimal.valueOf(2.1));

        LabelInfo.RecipientInfo recipientInfo = labels.stream()
                .filter(labelInfo -> labelInfo.getBarcode().equals("2"))
                .map(LabelInfo::getRecipient)
                .findAny()
                .orElse(null);
        assertThat(recipientInfo).isNotNull();
        assertThat(recipientInfo.getLastName()).isEqualTo("Сидоров");

        LabelInfo.AddressInfo addressInfo_1 = labels.stream()
                .filter(labelInfo -> labelInfo.getBarcode().equals("1"))
                .map(LabelInfo::getAddress)
                .findAny()
                .orElse(null);

        assertThat(addressInfo_1).isNotNull();
        assertThat(addressInfo_1.getStreet()).isEqualTo("Южная улица");

        LabelInfo.AddressInfo addressInfo_2 = labels.stream()
                .filter(labelInfo -> labelInfo.getBarcode().equals("2"))
                .map(LabelInfo::getAddress)
                .findAny()
                .orElse(null);

        assertThat(addressInfo_2).isNotNull();
        assertThat(addressInfo_2.getLocality()).isEqualTo("Irkutsk");

        List<LabelInfo.SellerInfo> sellers = labels.stream().map(LabelInfo::getSeller)
                .collect(Collectors.toList());
        List<String> ext_ids = sellers.stream().map(LabelInfo.SellerInfo::getNumber).collect(Collectors.toList());
        assertThat(ext_ids).containsOnly("номер1", "номер2");

        assertThat(sellers.get(0).getLegalName()).isEqualTo(LEGAL_NAME);
        assertThat(sellers.get(0).getReadableName()).isEqualTo(SHOP_NAME);

        List<LocalDate> dates = labels.stream().map(LabelInfo::getShipmentDate)
                .collect(Collectors.toList());
        assertThat(dates).containsOnly(LocalDate.of(2021, 5, 10), LocalDate.of(2021, 5, 9));
    }
}
