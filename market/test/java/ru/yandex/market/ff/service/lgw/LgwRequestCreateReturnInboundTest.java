package ru.yandex.market.ff.service.lgw;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.CommonTest;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.dbqueue.producer.CisReturnInboundQueueProducer;
import ru.yandex.market.ff.model.converter.IdentifierConverter;
import ru.yandex.market.ff.model.converter.LgwClientInboundConverter;
import ru.yandex.market.ff.model.converter.LgwClientStatusConverter;
import ru.yandex.market.ff.model.converter.LgwToFfwfConverter;
import ru.yandex.market.ff.model.converter.RegistryUnitDTOConverter;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.entity.Identifier;
import ru.yandex.market.ff.model.entity.LogisticUnit;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.enums.IdentifierType;
import ru.yandex.market.ff.repository.LogisticUnitRepository;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.RequestRealSupplierInfoRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.repository.SupplierRepository;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.service.implementation.lgw.FulfilmentRequestClient;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestCommonClient;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestReturnInboundService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.RequestApiTypeValidationService;
import ru.yandex.market.ff.util.WarehouseDateTimeUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CisHandleMode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Consignor;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Contractor;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnBox;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInboundType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class LgwRequestCreateReturnInboundTest extends CommonTest {

    protected static final long REQUEST_ID = 1;
    protected static final long SERVICE_ID = 145;
    private static final String CONSIGNOR_NAME = "consignorName";
    private static final long CONSIGNOR_ID = 190;
    private static final String CONSIGNOR_REQUEST_ID = "АПП №5";
    private static final LocalDateTime REQUESTED_DATE = LocalDateTime.of(2020, 3, 19, 12, 0, 0);
    private static final String REQUEST_COMMENT = "requestComment";
    private static final long SUPPLIER_ID = 2;
    private static final long SECOND_SUPPLIER_ID = 20;
    private static final long SKU = 3;
    private static final String ARTICLE = "4";
    private static final VatRate VAT_RATE = VatRate.VAT_0;
    private static final long CATEGORY_ID = 10;

    private LgwRequestReturnInboundService service;
    private FulfillmentClient fulfillmentClient;
    private RequestItemRepository requestItemRepository;
    private ShopRequestModificationService shopRequestModificationService;
    private LogisticUnitRepository logisticUnitRepository;
    private SoftAssertions assertions;
    private FulfillmentInfoService fulfillmentInfoService;

    @BeforeEach
    public void init() {
        fulfillmentClient = mock(FulfillmentClient.class);
        requestItemRepository = mock(RequestItemRepository.class);
        SupplierRepository supplierRepository = mock(SupplierRepository.class);
        ShopRequestFetchingService shopRequestFetchingService = mock(ShopRequestFetchingService.class);
        shopRequestModificationService = mock(ShopRequestModificationService.class);
        LgwRequestSupplyService lgwRequestSupplyService = mock(LgwRequestSupplyService.class);
        logisticUnitRepository = mock(LogisticUnitRepository.class);
        CisReturnInboundQueueProducer cisReturnInboundQueueProducer = mock(CisReturnInboundQueueProducer.class);
        ConcreteEnvironmentParamService concreteEnvironmentParamService =
                mock(ConcreteEnvironmentParamService.class);
        RequestRealSupplierInfoRepository requestRealSupplierInfoRepository =
                mock(RequestRealSupplierInfoRepository.class);
        fulfillmentInfoService = mock(FulfillmentInfoService.class);
        LgwClientInboundConverter clientConverter = new LgwClientInboundConverter(concreteEnvironmentParamService,
                new LgwClientStatusConverter(), shopRequestFetchingService, requestRealSupplierInfoRepository,
            mock(RequestSubTypeService.class), mock(ShopRequestRepository.class), fulfillmentInfoService);
        LgwToFfwfConverter lgwToFfwfConverter = new LgwToFfwfConverter();
        RegistryUnitDTOConverter registryUnitDTOConverter = new RegistryUnitDTOConverter();
        IdentifierConverter identifierConverter = new IdentifierConverter(registryUnitDTOConverter);
        LgwRequestCommonClient lgwRequestCommonClient = new FulfilmentRequestClient(fulfillmentClient);
        CalendaringServiceClientWrapperService csClientWrapperService =
                mock(CalendaringServiceClientWrapperService.class);
        RequestSubTypeService requestSubTypeService = mock(RequestSubTypeService.class);

        service = createService(lgwRequestSupplyService, fulfillmentClient, requestItemRepository,
                clientConverter, supplierRepository, shopRequestFetchingService,
                shopRequestModificationService, logisticUnitRepository,
                lgwToFfwfConverter, lgwRequestCommonClient, mock(TimeSlotsService.class),
                identifierConverter, cisReturnInboundQueueProducer, concreteEnvironmentParamService,
                csClientWrapperService, mock(RequestApiTypeValidationService.class), requestSubTypeService);

        Supplier firstSupplier = createSupplier(SUPPLIER_ID);
        Supplier secondSupplier = createSupplier(SECOND_SUPPLIER_ID);

        Mockito.when(supplierRepository.findAllByIdIn(Set.of(SUPPLIER_ID)))
                .thenReturn(Collections.singletonList(firstSupplier));
        Mockito.when(supplierRepository.findAllByIdIn(Set.of(SECOND_SUPPLIER_ID)))
                .thenReturn(Collections.singletonList(secondSupplier));
        Mockito.when(supplierRepository.findAllByIdIn(Set.of(SUPPLIER_ID, SECOND_SUPPLIER_ID)))
                .thenReturn(List.of(firstSupplier, secondSupplier));

        RequestSubTypeEntity requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setLgwTypeForSendToService("VALID_UNREDEEMED");
        Mockito.when(requestSubTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(requestSubTypeEntity);
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private LgwRequestReturnInboundService createService(
            LgwRequestSupplyService lgwRequestSupplyService,
            FulfillmentClient fulfillmentClient,
            RequestItemRepository requestItemRepository,
            LgwClientInboundConverter clientInboundConverter,
            SupplierRepository supplierRepository,
            ShopRequestFetchingService shopRequestFetchingService,
            ShopRequestModificationService shopRequestModificationService,
            LogisticUnitRepository logisticUnitRepository,
            LgwToFfwfConverter lgwToFfwfConverter,
            LgwRequestCommonClient lgwRequestCommonClient,
            TimeSlotsService timeSlotsService,
            IdentifierConverter identifierConverter,
            CisReturnInboundQueueProducer cisReturnInboundQueueProducer,
            ConcreteEnvironmentParamService concreteEnvironmentParamService,
            CalendaringServiceClientWrapperService csClientWrapperService,
            RequestApiTypeValidationService requestApiTypeValidationService,
            RequestSubTypeService requestSubTypeService) {
        return new LgwRequestReturnInboundService(
                lgwRequestSupplyService,
                fulfillmentClient,
                new LgwClientStatusConverter(),
                requestItemRepository,
                clientInboundConverter,
                supplierRepository,
                shopRequestFetchingService,
                shopRequestModificationService,
                logisticUnitRepository,
                lgwToFfwfConverter,
                lgwRequestCommonClient,
                timeSlotsService,
                identifierConverter,
                cisReturnInboundQueueProducer,
                concreteEnvironmentParamService,
                csClientWrapperService,
                requestApiTypeValidationService,
                requestSubTypeService
        );
    }

    @Test
    void assertCreateBoxWithoutOrderIdCorrect()
            throws GatewayApiException {
        String boxId = "boxId";
        LocalDateTime maxReceiptDate = LocalDateTime.of(2020, 5, 20, 15, 0, 0);
        LogisticUnit logisticUnit =
                createLogisticUnit(boxId, null, new String[]{"barcode1", "barcode2"}, maxReceiptDate, true, null);
        String itemName = "itemName";
        int count = 10;
        RequestItem requestItem = createRequestItem(itemName, count, logisticUnit, 145, SUPPLIER_ID);
        Mockito.when(requestItemRepository.findAllByRequestIdJoinFetchMarketVendorCodesOrderById(REQUEST_ID))
                .thenReturn(Collections.singleton(requestItem));
        Mockito.when(logisticUnitRepository.findAllByRequestId(REQUEST_ID))
                .thenReturn(Collections.singletonList(logisticUnit));

        ShopRequest request = createShopRequest();
        service.pushRequest(request);

        ArgumentCaptor<ReturnInbound> returnInboundCaptor = ArgumentCaptor.forClass(ReturnInbound.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(fulfillmentClient).createReturnInbound(returnInboundCaptor.capture(), partnerCaptor.capture());
        ReturnBox expectedReturnBox = createReturnBox(boxId, null, Arrays.asList("barcode1", "barcode2"),
                maxReceiptDate, true, null);
        assertReturnInboundIsExpected(returnInboundCaptor.getValue(), ReturnInboundType.VALID_UNREDEEMED,
                Collections.singletonList(expectedReturnBox), Collections.emptyList());
        Mockito.verify(shopRequestModificationService).updateStatus(request, RequestStatus.SENT_TO_SERVICE);
    }

    @Test
    void assertCreateOrderItemInTwoBoxesCorrect()
            throws GatewayApiException {
        String boxId = "boxId";
        LocalDateTime maxReceiptDate = LocalDateTime.of(2020, 5, 20, 15, 0, 0);
        String orderId = "orderId";
        String secondBoxId = "secondBoxId";
        LogisticUnit logisticUnit =
                createLogisticUnit(boxId, orderId, new String[]{"barcode1", "barcode2"}, maxReceiptDate, true, 1);
        LogisticUnit secondLogisticUnit = createLogisticUnit(secondBoxId, orderId,
                new String[]{"barcode1", "barcode2"}, maxReceiptDate, true, 2);

        String itemName = "itemName";
        int count = 10;
        RequestItem requestItem = createRequestItem(itemName, count, logisticUnit, 145, SECOND_SUPPLIER_ID);
        Mockito.when(requestItemRepository.findAllByRequestIdJoinFetchMarketVendorCodesOrderById(REQUEST_ID))
                .thenReturn(Collections.singleton(requestItem));
        Mockito.when(logisticUnitRepository.findAllByRequestId(REQUEST_ID))
                .thenReturn(Arrays.asList(logisticUnit, secondLogisticUnit));

        ShopRequest request = createShopRequest();
        service.pushRequest(request);

        ArgumentCaptor<ReturnInbound> returnInboundCaptor = ArgumentCaptor.forClass(ReturnInbound.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(fulfillmentClient).createReturnInbound(returnInboundCaptor.capture(), partnerCaptor.capture());
        ReturnBox expectedReturnBox = createReturnBox(boxId, orderId, Arrays.asList("barcode1", "barcode2"),
                maxReceiptDate, true, 1);
        ReturnBox secondExpectedReturnBox = createReturnBox(secondBoxId, orderId, Arrays.asList("barcode1", "barcode2"),
                maxReceiptDate, true, 2);
        ReturnItem expectedReturnItem = createReturnItem(orderId, itemName, count,
                Arrays.asList(boxId, secondBoxId), 145, SECOND_SUPPLIER_ID);
        assertReturnInboundIsExpected(returnInboundCaptor.getValue(), ReturnInboundType.VALID_UNREDEEMED,
                Arrays.asList(expectedReturnBox, secondExpectedReturnBox),
                Collections.singletonList(expectedReturnItem));
        Mockito.verify(shopRequestModificationService).updateStatus(request, RequestStatus.SENT_TO_SERVICE);
    }

    @Test
    void assertCreateTwoOrderItemsInSameBoxCorrect()
            throws GatewayApiException {
        String boxId = "boxId";
        LocalDateTime maxReceiptDate = LocalDateTime.of(2020, 5, 20, 15, 0, 0);
        String orderId = "orderId";
        LogisticUnit logisticUnit =
                createLogisticUnit(boxId, orderId, new String[]{"barcode1", "barcode2"}, maxReceiptDate, true, 2);

        String itemName = "itemName";
        int count = 10;
        RequestItem requestItem = createRequestItem(itemName, count, logisticUnit, 145, SUPPLIER_ID);
        RequestItem secondRequestItem = createRequestItem(itemName, count, logisticUnit, 145, SUPPLIER_ID);
        Mockito.when(requestItemRepository.findAllByRequestIdJoinFetchMarketVendorCodesOrderById(REQUEST_ID))
                .thenReturn(Set.of(requestItem, secondRequestItem));
        Mockito.when(logisticUnitRepository.findAllByRequestId(REQUEST_ID))
                .thenReturn(Collections.singletonList(logisticUnit));

        ShopRequest request = createShopRequest();
        service.pushRequest(request);

        ArgumentCaptor<ReturnInbound> returnInboundCaptor = ArgumentCaptor.forClass(ReturnInbound.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(fulfillmentClient).createReturnInbound(returnInboundCaptor.capture(), partnerCaptor.capture());
        ReturnBox expectedReturnBox = createReturnBox(boxId, orderId, Arrays.asList("barcode1", "barcode2"),
                maxReceiptDate, true, 2);
        ReturnItem expectedReturnItem = createReturnItem(orderId, itemName, count,
                Collections.singletonList(boxId), 145, SUPPLIER_ID);
        assertReturnInboundIsExpected(returnInboundCaptor.getValue(), ReturnInboundType.VALID_UNREDEEMED,
                Collections.singletonList(expectedReturnBox), Arrays.asList(expectedReturnItem, expectedReturnItem));
        Mockito.verify(shopRequestModificationService).updateStatus(request, RequestStatus.SENT_TO_SERVICE);
    }

    @Test
    void assertCreateTwoOrderItemsNotInBoxesCorrect()
            throws GatewayApiException {
        String boxId = "boxId";
        LocalDateTime maxReceiptDate = LocalDateTime.of(2020, 5, 20, 15, 0, 0);
        String orderId = "orderId";
        LogisticUnit boxLogisticUnit =
                createLogisticUnit(boxId, orderId, new String[]{"barcode1", "barcode2"}, maxReceiptDate, true, 2);

        LogisticUnit orderLogisticUnit =
                createLogisticUnit(null, orderId, new String[]{}, maxReceiptDate, true, 2);

        String itemName = "itemName";
        int count = 10;
        RequestItem requestItem = createRequestItem(itemName, count, orderLogisticUnit, 145, SUPPLIER_ID);
        RequestItem secondRequestItem = createRequestItem(itemName, count, orderLogisticUnit, 145, SUPPLIER_ID);
        Mockito.when(requestItemRepository.findAllByRequestIdJoinFetchMarketVendorCodesOrderById(REQUEST_ID))
                .thenReturn(Set.of(requestItem, secondRequestItem));
        Mockito.when(logisticUnitRepository.findAllByRequestId(REQUEST_ID))
                .thenReturn(Collections.singletonList(boxLogisticUnit));

        ShopRequest request = createShopRequest();
        service.pushRequest(request);

        ArgumentCaptor<ReturnInbound> returnInboundCaptor = ArgumentCaptor.forClass(ReturnInbound.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(fulfillmentClient).createReturnInbound(returnInboundCaptor.capture(), partnerCaptor.capture());
        ReturnBox expectedReturnBox = createReturnBox(boxId, orderId, Arrays.asList("barcode1", "barcode2"),
                maxReceiptDate, true, 2);
        ReturnItem expectedReturnItem = createReturnItem(orderId, itemName, count,
                Collections.emptyList(), 145, SUPPLIER_ID);
        assertReturnInboundIsExpected(returnInboundCaptor.getValue(), ReturnInboundType.VALID_UNREDEEMED,
                Collections.singletonList(expectedReturnBox), Arrays.asList(expectedReturnItem, expectedReturnItem));
        Mockito.verify(shopRequestModificationService).updateStatus(request, RequestStatus.SENT_TO_SERVICE);
    }


    @Test
    void assertCreateDifferentParamsForItemsInSameBoxCorrect() throws GatewayApiException {
        String boxId = "boxId";
        LocalDateTime maxReceiptDate = LocalDateTime.of(2020, 5, 20, 15, 0, 0);
        String orderId = "orderId";
        LocalDateTime secondMaxReceiptDate = LocalDateTime.of(2020, 6, 20, 15, 0, 0);
        LogisticUnit logisticUnit =
                createLogisticUnit(boxId, orderId, new String[]{"barcode1", "barcode2"}, maxReceiptDate, true, 1);
        LogisticUnit secondLogisticUnit =
                createLogisticUnit(boxId, orderId, null, secondMaxReceiptDate, false, 2);
        LogisticUnit thirdLogisticUnit =
                createLogisticUnit(boxId, orderId, new String[]{"barcode1", "barcode3"}, null, true, 3);

        String itemName = "itemName";
        int count = 10;
        RequestItem requestItem = createRequestItem(itemName, count, logisticUnit, 145, SUPPLIER_ID);
        RequestItem secondRequestItem =
                createRequestItem(itemName, count, secondLogisticUnit, 171, SECOND_SUPPLIER_ID);
        RequestItem thirdRequestItem = createRequestItemWithCis(thirdLogisticUnit, itemName, count);

        Mockito.when(requestItemRepository.findAllByRequestIdJoinFetchMarketVendorCodesOrderById(REQUEST_ID))
                .thenReturn(Set.of(requestItem, secondRequestItem, thirdRequestItem));
        Mockito.when(logisticUnitRepository.findAllByRequestId(REQUEST_ID))
                .thenReturn(Arrays.asList(logisticUnit, secondLogisticUnit, thirdLogisticUnit));

        ShopRequest request = createShopRequest();
        service.pushRequest(request);

        ArgumentCaptor<ReturnInbound> returnInboundCaptor = ArgumentCaptor.forClass(ReturnInbound.class);
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        Mockito.verify(fulfillmentClient).createReturnInbound(returnInboundCaptor.capture(), partnerCaptor.capture());
        ReturnBox expectedReturnBox = createReturnBox(boxId, orderId, Arrays.asList("barcode1", "barcode2", "barcode3"),
                maxReceiptDate, false, 3);
        ReturnItem expectedReturnItem = createReturnItem(orderId, itemName, count,
                Collections.singletonList(boxId), 145, SUPPLIER_ID);
        ReturnItem expectedSecondReturnItem = createReturnItem(orderId, itemName, count,
                Collections.singletonList(boxId), 171, SECOND_SUPPLIER_ID);
        List<Map<String, String>> identifiers = List.of(Map.of("CIS", "111СIS01"), Map.of("CIS", "111СIS02"));
        ReturnItem expectedThrirdReturnItem = createReturnItem(orderId, itemName, count,
                Collections.singletonList(boxId), 172, SUPPLIER_ID, identifiers);
        assertReturnInboundIsExpected(returnInboundCaptor.getValue(), ReturnInboundType.VALID_UNREDEEMED,
                Collections.singletonList(expectedReturnBox),
                Arrays.asList(expectedReturnItem, expectedSecondReturnItem, expectedThrirdReturnItem));
        Mockito.verify(shopRequestModificationService).updateStatus(request, RequestStatus.SENT_TO_SERVICE);
    }

    @NotNull
    private RequestItem createRequestItemWithCis(LogisticUnit thirdLogisticUnit, String itemName, int count) {
        RequestItem requestItem = createRequestItem(itemName, count, thirdLogisticUnit, 172, SUPPLIER_ID);
        long itemId = 111L;
        requestItem.setId(itemId);
        Identifier identifier = Identifier.builder()
                .id(1L)
                .itemId(itemId)
                .identifiers(RegistryUnitId.of(
                        RegistryUnitIdType.CIS, itemId + "СIS01",
                        RegistryUnitIdType.CIS, itemId + "СIS02"
                ))
                .type(IdentifierType.DECLARED)
                .build();
        requestItem.setRequestItemIdentifiers(Set.of(identifier));
        return requestItem;
    }

    @Nonnull
    private Supplier createSupplier(long supplierId) {
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setName("supplier" + supplierId);
        return supplier;
    }

    @Nonnull
    private ShopRequest createShopRequest() {
        ShopRequest request = new ShopRequest();
        request.setId(REQUEST_ID);
        request.setServiceId(SERVICE_ID);
        request.setStatus(RequestStatus.VALIDATED);
        request.setConsignor(CONSIGNOR_NAME);
        request.setConsignorId(CONSIGNOR_ID);
        request.setConsignorRequestId(CONSIGNOR_REQUEST_ID);
        request.setRequestedDate(REQUESTED_DATE);
        request.setComment(REQUEST_COMMENT);
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.FIRST_PARTY);
        request.setSupplier(supplier);
        return request;
    }

    @Nonnull
    private RequestItem createRequestItem(@Nonnull String name, int count, @Nullable LogisticUnit logisticUnit,
                                          long sourceFulfillmentId, long supplierId) {
        RequestItem requestItem = new RequestItem();
        requestItem.setName(name);
        requestItem.setCount(count);
        requestItem.setSku(SKU);
        requestItem.setArticle(ARTICLE);
        requestItem.setSupplierId(supplierId);
        requestItem.setVatRate(VAT_RATE);
        requestItem.setLogisticUnit(logisticUnit);
        requestItem.setCategoryId(CATEGORY_ID);
        requestItem.setSourceFulfillmentId(sourceFulfillmentId);
        return requestItem;
    }

    @Nonnull
    private LogisticUnit createLogisticUnit(@Nullable String boxId, @Nullable String orderId,
                                            @Nullable String[] barcodes, @Nullable LocalDateTime maxReceiptDate,
                                            boolean shouldBeAccepted, @Nullable Integer boxesInOrder) {
        LogisticUnit logisticUnit = new LogisticUnit();
        logisticUnit.setBoxId(boxId);
        logisticUnit.setOrderId(orderId);
        logisticUnit.setBarcodes(barcodes);
        logisticUnit.setMaxReceiptDate(maxReceiptDate);
        logisticUnit.setShouldBeAccepted(shouldBeAccepted);
        logisticUnit.setBoxesInOrder(boxesInOrder);
        return logisticUnit;
    }

    @Nonnull
    private ReturnBox createReturnBox(@Nonnull String boxId, @Nullable String orderId,
                                      @Nonnull List<String> barcodesString, @Nullable LocalDateTime maxReceiptDate,
                                      boolean shouldBeReceipt, @Nullable Integer boxesInOrder) {
        List<Barcode> barcodes = barcodesString.stream()
                .map(this::createBarcode)
                .collect(Collectors.toList());
        return new ReturnBox.ReturnBoxBuilder(boxId, shouldBeReceipt)
                .setOrderId(orderId)
                .setBarcodes(barcodes)
                .setMaxReceiptDate(Optional.ofNullable(maxReceiptDate).map(DateTime::fromLocalDateTime).orElse(null))
                .setBoxesInOrder(boxesInOrder)
                .build();
    }

    @Nonnull
    private Barcode createBarcode(String barcodeString) {
        return new Barcode(barcodeString, null, null);
    }

    @Nonnull
    private ReturnItem createReturnItem(@Nonnull String orderId, @Nonnull String name, int count,
                                        @Nullable List<String> boxIds, long sourceFulfillmentId, long supplierId) {
        return createReturnItem(orderId, name, count, boxIds, sourceFulfillmentId, supplierId, Collections.EMPTY_LIST);
    }

    @Nonnull
    private ReturnItem createReturnItem(@Nonnull String orderId, @Nonnull String name, int count,
                                        @Nullable List<String> boxIds, long sourceFulfillmentId, long supplierId,
                                        @Nonnull List<Map<String, String>> instances) {
        return new ReturnItem.ReturnItemBuilder(orderId, createItem(name, count, supplierId, instances))
                .setBoxIds(boxIds)
                .setSourceFulfillmentId(sourceFulfillmentId)
                .build();
    }

    @Nonnull
    private Item createItem(@Nonnull String name, int count, long supplierID, List<Map<String, String>> instances) {
        return new Item.ItemBuilder(name, count, BigDecimal.ONE, CargoType.UNKNOWN, Collections.emptyList())
                .setUnitId(new UnitId(String.valueOf(SKU), supplierID, ARTICLE))
                .setArticle(ARTICLE)
                .setHasLifeTime(false)
                .setInboundServices(Collections.emptyList())
                .setCategoryId(CATEGORY_ID)
                .setUrls(List.of("https://pokupki.market.yandex.ru/product/3"))
                .setContractor(new Contractor(String.valueOf(supplierID), "supplier" + supplierID))
                .setCisHandleMode(CisHandleMode.NOT_DEFINED)
                .setInstances(instances)
                .build();
    }

    private void assertReturnInboundIsExpected(@Nonnull ReturnInbound returnInbound,
                                               @Nonnull ReturnInboundType expectedReturnInboundType,
                                               @Nonnull List<ReturnBox> expectedReturnBoxes,
                                               @Nonnull List<ReturnItem> expectedReturnItems) {
        OffsetDateTime offsetRequestedDate = WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE, SERVICE_ID);
        DateTimeInterval expectedInterval = new DateTimeInterval(offsetRequestedDate, offsetRequestedDate);
        assertions.assertThat(returnInbound.getInboundId().getYandexId()).isEqualTo(String.valueOf(REQUEST_ID));
        assertions.assertThat(returnInbound.getConsignor()).isEqualTo(new Consignor(CONSIGNOR_ID, CONSIGNOR_NAME));
        assertions.assertThat(returnInbound.getConsignorInboundId()).isEqualTo(CONSIGNOR_REQUEST_ID);
        assertions.assertThat(returnInbound.getComment()).isEqualTo(REQUEST_COMMENT);
        assertions.assertThat(returnInbound.getDateTimeInterval()).isEqualTo(expectedInterval);
        assertions.assertThat(returnInbound.getInboundType()).isEqualTo(expectedReturnInboundType);
        assertions.assertThat(returnInbound.getReturnBoxes()).containsExactlyInAnyOrderElementsOf(expectedReturnBoxes);
        assertEqualsIgnoringArrayOrder(returnInbound.getReturnItems(), expectedReturnItems);
    }

    private void assertEqualsIgnoringArrayOrder(@Nonnull List<ReturnItem> first, @Nonnull List<ReturnItem> second) {
        List<ReturnItemWithSetOfBoxes> firstItemsWithSetOfBoxes = first.stream()
                .map(ReturnItemWithSetOfBoxes::new)
                .collect(Collectors.toList());
        List<ReturnItemWithSetOfBoxes> secondItemsWithSetOfBoxes = second.stream()
                .map(ReturnItemWithSetOfBoxes::new)
                .collect(Collectors.toList());
        assertions.assertThat(firstItemsWithSetOfBoxes).containsExactlyInAnyOrderElementsOf(secondItemsWithSetOfBoxes);
    }

    private static class ReturnItemWithSetOfBoxes {
        private final ReturnItem returnItem;
        private final Set<String> boxIds;

        private ReturnItemWithSetOfBoxes(ReturnItem returnItem) {
            this.returnItem = new ReturnItem.ReturnItemBuilder(returnItem.getOrderId(), returnItem.getItem()).build();
            this.boxIds = new HashSet<>(returnItem.getBoxIds());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ReturnItemWithSetOfBoxes that = (ReturnItemWithSetOfBoxes) o;
            return Objects.equals(returnItem, that.returnItem) &&
                    Objects.equals(boxIds, that.boxIds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(returnItem, boxIds);
        }

        @Override
        public String toString() {
            return "ReturnItemWithSetOfBoxes{" +
                    "returnItem=" + returnItem +
                    ", boxIds=" + boxIds +
                    '}';
        }
    }
}
