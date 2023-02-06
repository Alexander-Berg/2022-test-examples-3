package ru.yandex.market.ff.model.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.enums.CisHandleMode;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.enums.FulfillmentServiceType;
import ru.yandex.market.ff.model.dto.AddressDTO;
import ru.yandex.market.ff.model.dto.courier.Car;
import ru.yandex.market.ff.model.dto.courier.Person;
import ru.yandex.market.ff.model.dto.courier.Phone;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.FulfillmentInfo;
import ru.yandex.market.ff.model.entity.Identifier;
import ru.yandex.market.ff.model.entity.LogisticsPoint;
import ru.yandex.market.ff.model.entity.RequestCourier;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.RequestItemCargoType;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.enums.IdentifierType;
import ru.yandex.market.ff.repository.RequestRealSupplierInfoRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.logistic.gateway.common.model.common.Address;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Inbound;
import ru.yandex.market.logistic.gateway.common.model.common.InboundType;
import ru.yandex.market.logistic.gateway.common.model.common.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.common.Location;
import ru.yandex.market.logistic.gateway.common.model.common.LogisticPoint;
import ru.yandex.market.logistic.gateway.common.model.common.Outbound;
import ru.yandex.market.logistic.gateway.common.model.common.OutboundType;
import ru.yandex.market.logistic.gateway.common.model.common.Party;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Contractor;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LgwClientConverterTest {

    private static final String CONTRACTOR_ID = "test_contractor_id";
    private static final String CONTRACTOR_NAME = "test_contractor_name";
    private static final String ARTICLE = "ARTICLE";
    private static final long SUPPLIER_ID = 1000L;
    private static final String SUPPLIER_NAME = "supplier1000";
    private static final long SKU_ID = 100L;
    private static final String COMMENT = "COMMENT";
    private static final String NAME = "NAME";
    ConcreteEnvironmentParamService concreteEnvironmentParamService =
            mock(ConcreteEnvironmentParamService.class);
    RequestRealSupplierInfoRepository requestRealSupplierInfoRepository =
            mock(RequestRealSupplierInfoRepository.class);
    private final ShopRequestFetchingService shopRequestFetchingService = mock(ShopRequestFetchingService.class);
    private final LgwClientStatusConverter statusConverter = new LgwClientStatusConverter();
    private final RequestSubTypeService subTypeService = mock(RequestSubTypeService.class);
    private final LgwClientOutboundConverter outboundCut =
            new LgwClientOutboundConverter(concreteEnvironmentParamService, statusConverter, subTypeService,
                    shopRequestFetchingService);
    private final ShopRequestRepository shopRequestRepository = mock(ShopRequestRepository.class);
    private final FulfillmentInfoService fulfillmentInfoService = mock(FulfillmentInfoService.class);
    private final LgwClientInboundConverter cut = new LgwClientInboundConverter(concreteEnvironmentParamService,
            statusConverter, shopRequestFetchingService, requestRealSupplierInfoRepository,
        subTypeService, shopRequestRepository, fulfillmentInfoService);


    @Before
    public void init() {
        LgwClientInboundConverter inboundCut = Mockito.spy(cut);
        Mockito.doReturn(null).when(outboundCut).convertCourier(any());
        Mockito.doReturn(null).when(outboundCut).convertLegalEntity(any());
        Mockito.doReturn(null).when(statusConverter).convertStatus(any());
        Mockito.doReturn(null).when(cut).convertStockType(any());
        Mockito.doReturn(null).when(cut).convertXDocStatus(any());
    }

    @Test
    void realSupplierInfoProvidedTest() {
        RequestItem requestItem = new RequestItem();

        requestItem.setRealSupplierId(CONTRACTOR_ID);
        requestItem.setRealSupplierName(CONTRACTOR_NAME);
        requestItem.setVatRate(VatRate.VAT_10);

        Item item = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Collections.emptySet());

        assertSoftly(s -> s.assertThat(item.getContractor()).isNotNull());
        assertSoftly(s -> s.assertThat(item.getContractor().getId()).isEqualTo(CONTRACTOR_ID));
        assertSoftly(s -> s.assertThat(item.getContractor().getName()).isEqualTo(CONTRACTOR_NAME));
    }

    @Test
    void contractorInfoSetFromSupplierIfRealSupplierNameIsNullTest() {
        RequestItem requestItem = new RequestItem();
        requestItem.setVatRate(VatRate.VAT_10);
        requestItem.setRealSupplierId(CONTRACTOR_ID);
        requestItem.setRealSupplierName(null);

        Item item = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Collections.emptySet());

        assertSoftly(s -> s.assertThat(item.getContractor()).isNotNull());
        assertSoftly(s -> s.assertThat(item.getContractor().getId()).isEqualTo(String.valueOf(SUPPLIER_ID)));
        assertSoftly(s -> s.assertThat(item.getContractor().getName()).isEqualTo(SUPPLIER_NAME));
    }

    @Test
    void contractorInfoSetFromSupplierIfRealSupplierIdIsNullTest() {
        RequestItem requestItem = new RequestItem();
        requestItem.setVatRate(VatRate.VAT_10);

        requestItem.setRealSupplierId(null);
        requestItem.setRealSupplierName(CONTRACTOR_NAME);

        Item item = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Collections.emptySet());

        assertSoftly(s -> s.assertThat(item.getContractor()).isNotNull());
        assertSoftly(s -> s.assertThat(item.getContractor().getId()).isEqualTo(String.valueOf(SUPPLIER_ID)));
        assertSoftly(s -> s.assertThat(item.getContractor().getName()).isEqualTo(SUPPLIER_NAME));
    }

    @Test
    void contractorInfoSetFromSupplierIfRealSupplierIsNullTest() {
        RequestItem requestItem = new RequestItem();
        requestItem.setVatRate(VatRate.VAT_10);

        requestItem.setRealSupplierId(null);
        requestItem.setRealSupplierName(null);

        Item item = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Collections.emptySet());

        assertSoftly(s -> s.assertThat(item.getContractor()).isNotNull());
        assertSoftly(s -> s.assertThat(item.getContractor().getId()).isEqualTo(String.valueOf(SUPPLIER_ID)));
        assertSoftly(s -> s.assertThat(item.getContractor().getName()).isEqualTo(SUPPLIER_NAME));
    }

    @Test
    void shouldSuccessConvertRemainingLifetimes() {
        RequestItem requestItem = new RequestItem();
        requestItem.setVatRate(VatRate.VAT_10);

        requestItem.setInboundRemainingLifetimeDays(20);
        requestItem.setOutboundRemainingLifetimeDays(15);
        requestItem.setInboundRemainingLifetimePercentage(30);
        requestItem.setOutboundRemainingLifetimePercentage(30);

        Item item = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Collections.emptySet());

        assertSoftly(s -> {
            s.assertThat(item.getRemainingLifetimes()).isNotNull();
            s.assertThat(item.getRemainingLifetimes().getInbound().getDays().getValue()).isEqualTo(20);
            s.assertThat(item.getRemainingLifetimes().getInbound().getPercentage().getValue()).isEqualTo(30);
            s.assertThat(item.getRemainingLifetimes().getOutbound().getDays().getValue()).isEqualTo(15);
            s.assertThat(item.getRemainingLifetimes().getOutbound().getPercentage().getValue()).isEqualTo(30);
        });
    }

    @Test
    void shouldConvertCisRelatedFields() {
        RequestItem requestItem = new RequestItem();
        requestItem.setId(1L);
        requestItem.setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED);
        requestItem.setRequestItemIdentifiers(Set.of(
                new Identifier(requestItem.getId(), new RegistryUnitId(Set.of(
                        UnitPartialId.builder().type(RegistryUnitIdType.CIS).value("cis-1").build(),
                        UnitPartialId.builder().type(RegistryUnitIdType.CIS).value("cis-2").build(),
                        UnitPartialId.builder().type(RegistryUnitIdType.IMEI).value("imei-1").build()
                )), IdentifierType.DECLARED, null),
                new Identifier(requestItem.getId(), new RegistryUnitId(Set.of(
                        UnitPartialId.builder().type(RegistryUnitIdType.CIS).value("cis-3").build(),
                        UnitPartialId.builder().type(RegistryUnitIdType.CIS).value("cis-4").build(),
                        UnitPartialId.builder().type(RegistryUnitIdType.CIS).value("cis-5").build(),
                        UnitPartialId.builder().type(RegistryUnitIdType.IMEI).value("imei-2").build()
                )), IdentifierType.DECLARED, null),
                new Identifier(requestItem.getId(), new RegistryUnitId(Set.of(
                        UnitPartialId.builder().type(RegistryUnitIdType.CIS).value("cis-1").build()
                )), IdentifierType.RECEIVED, null)
        ));

        Item item = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Collections.emptySet());

        var expected = List.of(Map.of("CIS", "cis-1"), Map.of("CIS", "cis-2"), Map.of("CIS", "cis-3"), Map.of("CIS",
                "cis-4"), Map.of("CIS", "cis-5"));
        assertSoftly(s -> {
            s.assertThat(item.getCisHandleMode()).isNotNull();
            s.assertThat(item.getCisHandleMode()).isEqualTo(
                    ru.yandex.market.logistic.gateway.common.model.fulfillment.CisHandleMode.ACCEPT_ONLY_DECLARED);
            s.assertThat(item.getInstances().size()).isEqualTo(expected.size());
            s.assertThat(item.getInstances()).containsExactlyInAnyOrderElementsOf(expected);
        });
    }

    @Test
    void vatRateNotUsed() {
        RequestItem requestItem = getRequestItem();

        Item expected = getItem(List.of(CargoType.R18, CargoType.CIS_REQUIRED));
        Item actual = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Set.of(CargoType.R18.getCode(), CargoType.CIS_REQUIRED.getCode()));

        assertThat(actual).isEqualToIgnoringGivenFields(expected, "cargoTypes");
    }

    @Test
    void ignoreDisabledCargoTypes() {
        Mockito.when(concreteEnvironmentParamService.getDisabledCargoTypes())
                .thenReturn(Set.of(CargoType.CIS_REQUIRED.getCode()));
        RequestItem requestItem = getRequestItem();

        Item expected = getItem(List.of(CargoType.R18));
        Item actual = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Set.of(CargoType.R18.getCode(), CargoType.CIS_REQUIRED.getCode()));

        assertEquals(expected, actual);
    }

    @Test
    void supplyPriceNotSetButOne() {
        RequestItem requestItem = new RequestItem();

        Item item = cut.convertItem(requestItem, getSupplier(), false, Collections.emptyList(),
                Collections.emptySet());

        assertSoftly(s -> s.assertThat(item.getPrice()).isEqualTo(BigDecimal.ONE));
    }

    @Test
    void convertLocation() {
        var address = getAddress();

        Location expected = getLocation();
        Location actual = cut.convertLocation(address);

        assertEquals(expected, actual);
    }

    @Test
    void convertLogisticsPoint() {
        var logisticsPointEntity = getLogisticsPointEntity();

        LogisticPoint expected = getLogisticPoint();
        LogisticPoint actual = cut.convertLogisticsPoint(logisticsPointEntity);

        assertEquals(expected, actual);
    }

    @Test
    void convertDsScInbound() {
        var inbound = getShopRequest(RequestType.ORDERS_SUPPLY);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(getRequestSubTypeEntity(false));
        when(fulfillmentInfoService.getFulfillmentInfo(anyLong())).thenReturn(Optional.empty());

        Inbound expected = getInbound(InboundType.DS_SC);
        Inbound actual = cut.convertInbound(inbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertDamageInbound() {
        var inbound = getShopRequest(RequestType.MOVEMENT_SUPPLY);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(getRequestSubTypeEntity(false));
        inbound.setStockType(StockType.DEFECT);

        Inbound expected = getInbound(InboundType.WH2WHDMG);
        Inbound actual = cut.convertInbound(inbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertExpiredInbound() {
        var inbound = getShopRequest(RequestType.MOVEMENT_SUPPLY);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(getRequestSubTypeEntity(false));
        inbound.setStockType(StockType.EXPIRED);

        Inbound expected = getInbound(InboundType.WH2WHDMG);
        Inbound actual = cut.convertInbound(inbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertUnredeemedInbound() {
        var inbound = getShopRequest(RequestType.CUSTOMER_RETURN);
        inbound.setConsignorId(1L);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(fulfillmentInfoService.getFulfillmentInfo(anyLong())).thenReturn(Optional.of(
                new FulfillmentInfo(1L, "Name", null, FulfillmentServiceType.DELIVERY, "Legal Name", "Legal Address",
                        "Warehouse Address")
        ));
        mockSubType("UNREDEEMED");
        Inbound actual = cut.convertInbound(inbound);
        assertEquals(getInboundWithShipper(InboundType.UNREDEEMED), actual);
    }

    @Test
    void convertUpdatableReturnInbound() {
        var inbound = getShopRequest(RequestType.CUSTOMER_RETURN);
        inbound.setConsignorId(1L);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(fulfillmentInfoService.getFulfillmentInfo(anyLong())).thenReturn(Optional.of(
                new FulfillmentInfo(1L, "Name", null, FulfillmentServiceType.DELIVERY, "Legal Name", "Legal Address",
                        "Warehouse Address")
        ));
        mockSubType("UPDATABLE_CUSTOMER_RETURN");
        Inbound actual = cut.convertInbound(inbound);
        assertEquals(getInboundWithShipper(InboundType.UPDATABLE_CUSTOMER_RETURN), actual);
    }

    @Test
    void convertReturnEnrichmentInbound() {
        var inbound = getShopRequest(RequestType.CUSTOMER_RETURN);
        inbound.setConsignorId(1L);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(fulfillmentInfoService.getFulfillmentInfo(anyLong())).thenReturn(Optional.of(
                new FulfillmentInfo(1L, "Name", null, FulfillmentServiceType.DELIVERY, "Legal Name", "Legal Address",
                        "Warehouse Address")
        ));
        mockSubType("UPDATABLE_CUSTOMER_RETURN");
        Inbound actual = cut.convertInbound(inbound);
        assertEquals(getInboundWithShipper(InboundType.UPDATABLE_CUSTOMER_RETURN), actual);
    }

    private void mockSubType(String typeToSend) {
        RequestSubTypeEntity requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setLgwTypeForSendToService(typeToSend);
        requestSubTypeEntity.setUseParentRequestIdForSendToService(false);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(requestSubTypeEntity);
    }

    @Test
    void convertInvetarizationInbound() {
        var inbound = getShopRequest(RequestType.INVENTORYING_SUPPLY);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(getRequestSubTypeEntity(false));

        Inbound expected = getInbound(InboundType.INVENTARIZATION);
        Inbound actual = cut.convertInbound(inbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertInvetarizationInboundWithoutLogisticsPoint() {
        var inbound = getShopRequest(RequestType.INVENTORYING_SUPPLY);
        inbound.setLogisticsPoint(null);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(getRequestSubTypeEntity(false));
        when(fulfillmentInfoService.getFulfillmentInfo(anyLong())).thenReturn(Optional.empty());

        var inboundBuilder = Inbound.builder(
                ResourceId.builder().setYandexId("123456").build(),
                InboundType.INVENTARIZATION,
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00")
        );
        Inbound expected = inboundBuilder.setComment("Comment").build();
        Inbound actual = cut.convertInbound(inbound);

        assertEquals(expected, actual);
        assertNull(actual.getLogisticPoint());
    }

    @Test
    void convertUnknownInbound() {
        var inbound = getShopRequest(RequestType.CUSTOMER_RETURN_SUPPLY);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(getRequestSubTypeEntity(false));

        Inbound expected = getInbound(InboundType.UNKNOWN);
        Inbound actual = cut.convertInbound(inbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertInboundWithParentRequestId() {
        var inbound = getShopRequest(RequestType.CUSTOMER_RETURN_SUPPLY);
        inbound.setParentRequestId(109090L);

        RequestSubTypeEntity requestSubTypeEntity = getRequestSubTypeEntity(true);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(requestSubTypeEntity);

        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);

        Inbound expected = getInboundWithYandexId(InboundType.UNKNOWN, "109090");
        Inbound actual = cut.convertInbound(inbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertInboundWithParentRequestIdWhenFlagIsDisabled() {
        var inbound = getShopRequest(RequestType.CUSTOMER_RETURN_SUPPLY);
        inbound.setParentRequestId(109090L);
        RequestSubTypeEntity requestSubTypeEntity = getRequestSubTypeEntity(false);

        when(subTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(requestSubTypeEntity);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);

        Inbound expected = getInbound(InboundType.UNKNOWN);
        Inbound actual = cut.convertInbound(inbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertInboundWithParentRequestIdShouldThrowAnException() {
        var inbound = getShopRequest(RequestType.CUSTOMER_RETURN_SUPPLY);
        RequestSubTypeEntity requestSubTypeEntity = getRequestSubTypeEntity(true);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(requestSubTypeEntity);
        when(shopRequestRepository.findForClientInboundConverter(inbound.getId())).thenReturn(inbound);

        assertThrows(IllegalStateException.class, () -> cut.convertInbound(inbound));
    }

    @Test
    void convertDsScOutbound() {
        var outbound = getShopRequest(RequestType.ORDERS_WITHDRAW);

        Outbound expected = getOutbound(OutboundType.DS_SC);
        Outbound actual = outboundCut.convertOutbound(outbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertDamageOutbound() {
        var outbound = getShopRequest(RequestType.MOVEMENT_WITHDRAW);
        outbound.setStockType(StockType.DEFECT);

        Outbound expected = getOutbound(OutboundType.WH2WHDMG);
        Outbound actual = outboundCut.convertOutbound(outbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertExpiredOutbound() {
        var outbound = getShopRequest(RequestType.MOVEMENT_WITHDRAW);
        outbound.setStockType(StockType.EXPIRED);

        Outbound expected = getOutbound(OutboundType.WH2WHEXP);
        Outbound actual = outboundCut.convertOutbound(outbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertReturnOutbound() {
        var outbound = getShopRequest(RequestType.ORDERS_RETURN_WITHDRAW);

        Outbound expected = getOutbound(OutboundType.ORDERS_RETURN);
        Outbound actual = outboundCut.convertOutbound(outbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertDropoffReturnOutbound() {
        var outbound = getShopRequest(RequestType.DROPOFF_RETURN_WITHDRAW);

        Outbound expected = getOutbound(OutboundType.DROPOFF_RETURN);
        Outbound actual = outboundCut.convertOutbound(outbound);

        assertEquals(expected, actual);
    }

    @Test
    void convertFixLostInventoryingOutbound() {
        var outbound = getShopRequest(RequestType.FIX_LOST_INVENTORYING_WITHDRAW);

        Outbound expected = getOutbound(OutboundType.FIX_LOST_INVENTARIZATION);
        Outbound actual = outboundCut.convertOutbound(outbound);

        assertEquals(expected, actual);
    }

    @Test
    void toCommonLgwCourier() {
        assertNull(cut.toCommonLgwCourier(null));
        assertNotNull(cut.toCommonLgwCourier(Collections.singleton(RequestCourier.builder()
                .partnerId(ru.yandex.market.ff.model.dto.courier.ResourceId.builder().build())
                .build())));
    }

    @Test
    void toCommonLgwAddress() {
        assertNull(cut.toCommonLgwAddress(null));
        assertNotNull(cut.toCommonLgwAddress(ru.yandex.market.ff.model.dto.courier.Location.builder().build()));
    }

    @Test
    void toCommonLgwLocation() {
        assertNotNull(cut.toCommonLgwLocation(ru.yandex.market.ff.model.dto.courier.Location.builder().build()));
    }

    @Test
    void toCommonLgwCar() {
        assertNull(cut.toCommonLgwCar(null));
        assertNull(cut.toCommonLgwCar(Car.builder().build()));
        assertNotNull(cut.toCommonLgwCar(Car.builder().number("123").build()));
    }

    @Test
    void toCommonLgwPhone() {
        assertNull(cut.toCommonLgwPhone(null));
        assertNull(cut.toCommonLgwPhone(Phone.builder().build()));
        assertNotNull(cut.toCommonLgwPhone(Phone.builder().phoneNumber("123").build()));
    }

    @Test
    void toCommonLgwPersons() {
        assertNull(cut.toCommonLgwPersons(null));
        assertNotNull(cut.toCommonLgwPersons(List.of()));
        assertNotNull(cut.toCommonLgwPersons(List.of(Person.builder().build())));
    }

    @Test
    void toCommonLgwResourceId() {
        assertNotNull(cut.toCommonLgwResourceId(ru.yandex.market.ff.model.dto.courier.ResourceId.builder().build()));
    }

    private ShopRequest getShopRequest(RequestType type) {
        var shopRequest = new ShopRequest();
        shopRequest.setId(123456L);
        shopRequest.setRequestedDate(LocalDateTime.of(2018, 1, 5, 10, 0, 0));
        shopRequest.setType(type);
        shopRequest.setLogisticsPoint(getLogisticsPointEntity());
        shopRequest.setComment("Comment");

        return shopRequest;
    }

    private Inbound getInbound(InboundType type) {
        return getInboundWithYandexId(type, "123456");
    }

    private Inbound getInboundWithYandexId(InboundType type, String yandexId) {
        var inboundBuilder = Inbound.builder(
                ResourceId.builder().setYandexId(yandexId).build(),
                type,
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00")
        );
        inboundBuilder.setComment("Comment");
        inboundBuilder.setLogisticPoint(getLogisticPoint());
        return inboundBuilder.build();
    }

    private Inbound getInboundWithShipper(InboundType type) {
        var inboundBuilder = Inbound.builder(
                ResourceId.builder().setYandexId("123456").build(),
                type,
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00")
        );
        inboundBuilder.setComment("Comment");
        inboundBuilder.setLogisticPoint(getLogisticPoint());
        inboundBuilder.setShipper(
                Party.builder(LogisticPoint.builder(ResourceId.builder().setYandexId("5125425").build()).build())
                        .setPartnerId(ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                                .setYandexId("1")
                                .build())
                .setLegalEntity(LegalEntity.builder()
                        .setName("Legal Name")
                        .setLegalName("Legal Name")
                        .setAddress(Address.builder("Legal Address").build())
                        .build())
                .build());
        return inboundBuilder.build();
    }

    private Outbound getOutbound(OutboundType type) {
        var outboundBuilder = Outbound.builder(
                ResourceId.builder().setYandexId("123456").build(),
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00")
        );
        outboundBuilder.setOutboundType(type);
        outboundBuilder.setComment("Comment");
        outboundBuilder.setLogisticPoint(getLogisticPoint());
        return outboundBuilder.build();
    }

    private LogisticPoint getLogisticPoint() {
        return LogisticPoint.builder(ResourceId.builder().setYandexId("5125425").setPartnerId("550-1234").build())
                .setLocation(getLocation())
                .build();
    }

    private LogisticsPoint getLogisticsPointEntity() {
        var logisticsPoint = new LogisticsPoint(5125425L);
        logisticsPoint.setAddress(getAddress());
        logisticsPoint.setExternalId("550-1234");

        return logisticsPoint;
    }

    private Location getLocation() {
        return Location.builder("Россия", "Королев", "Московская обл.")
                .setLocationId(123L)
                .setSettlement("Королев")
                .setStreet("Проспект Космонавтов")
                .setHouse("47")
                .setHousing("17")
                .setBuilding("1")
                .setRoom("1")
                .setZipCode("141080")
                .setLat(BigDecimal.valueOf(50.4151))
                .setLng(BigDecimal.valueOf(31.0341))
                .build();
    }

    private AddressDTO getAddress() {
        return AddressDTO.builder()
                .locationId(123)
                .region("Московская обл.")
                .settlement("Королев")
                .street("Проспект Космонавтов")
                .house("47")
                .housing("17")
                .building("1")
                .apartment("1")
                .comment("Первый коктейль бесплатно")
                .postCode("141080")
                .latitude(BigDecimal.valueOf(50.4151))
                .longitude(BigDecimal.valueOf(31.0341))
                .build();
    }

    @NotNull
    private RequestSubTypeEntity getRequestSubTypeEntity(boolean useParentRequestIdForSendToService) {
        var requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setUseParentRequestIdForSendToService(useParentRequestIdForSendToService);
        return requestSubTypeEntity;
    }

    @NotNull
    private Supplier getSupplier() {
        Supplier supplier = new Supplier();
        supplier.setId(SUPPLIER_ID);
        supplier.setName(SUPPLIER_NAME);
        return supplier;
    }

    private RequestItem getRequestItem() {
        final RequestItem requestItem = new RequestItem();
        requestItem.setSku(SKU_ID);
        requestItem.setName(NAME);
        requestItem.setArticle(ARTICLE);
        requestItem.setVatRate(VatRate.VAT_10);
        requestItem.setSupplyPrice(new BigDecimal(100));
        requestItem.setCount(1);
        requestItem.setRealSupplierId(CONTRACTOR_ID);
        requestItem.setRealSupplierName(CONTRACTOR_NAME);
        requestItem.setComment(COMMENT);
        requestItem.setUntaxedPrice(BigDecimal.valueOf(1000L));
        requestItem.setBoxCount(100);
        requestItem.setHasExpirationDate(true);
        requestItem.setPackageNumInSpike(12);
        requestItem.setCategoryId(111L);
        requestItem.setBarcodes(Arrays.asList("bar1", "bar2"));
        requestItem.setVendorCode("VENDOR");
        requestItem.setRequestItemCargoTypes(RequestItemCargoType.asSet(requestItem, 20, 980));
        requestItem.setImeiCount(2);
        requestItem.setImeiMask(".+");
        requestItem.setImeiCount(1);
        requestItem.setImeiMask(".*");
        return requestItem;
    }

    private Item getItem(List<CargoType> cargoTypes) {
        return new Item.ItemBuilder(NAME, 1, new BigDecimal(100), CargoType.UNKNOWN,
                cargoTypes)
                .setUnitId(new UnitId(String.valueOf(SKU_ID), SUPPLIER_ID, ARTICLE))
                .setArticle(ARTICLE)
                .setComment(COMMENT)
                .setBoxCount(100)
                .setHasLifeTime(true)
                .setBoxCapacity(12)
                .setCategoryId(111L)
                .setBarcodes(List.of(new Barcode("bar1", null, null),
                        new Barcode("bar2", null, null)))
                .setVendorCodes(List.of("VENDOR"))
                .setInboundServices(List.of())
                .setContractor(new Contractor(CONTRACTOR_ID, CONTRACTOR_NAME))
                .setUrls(List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 100)))
                .setCheckImei(2)
                .setImeiMask(".+")
                .setCheckImei(1)
                .setImeiMask(".*")
                .setCisHandleMode(ru.yandex.market.logistic.gateway.common.model.fulfillment.CisHandleMode.NOT_DEFINED)
                .setInstances(Collections.emptyList())
                .build();
    }

}
