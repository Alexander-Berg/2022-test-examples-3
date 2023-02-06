package ru.yandex.market.ff.service.lgw;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.CommonTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.model.TypeSubtype;
import ru.yandex.market.ff.model.converter.LgwClientInboundConverter;
import ru.yandex.market.ff.model.converter.LgwClientStatusConverter;
import ru.yandex.market.ff.model.converter.LgwToFfwfConverter;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.RequestRealSupplierInfoRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.repository.SupplierRepository;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.RequestApiTypeValidationService;
import ru.yandex.market.ff.util.WarehouseDateTimeUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LgwRequestCreateInboundTest extends CommonTest {

    public static final LocalDateTime REQUESTED_DATE =
            LocalDateTime.of(2018, 1, 1, 10, 10, 10);
    private static final long REQUEST_ID_1 = 11L;
    private static final long INFOR_WAREHOUSE_ID = 147L;
    private static final String EXTERNAL_REQUEST_ID = "externalRequestId";
    private LgwRequestSupplyService lgwRequestSupplyService;
    private FulfillmentClient fulfillmentClient;
    private ConcreteEnvironmentParamService concreteEnvironmentParamService =
            Mockito.mock(ConcreteEnvironmentParamService.class);
    private CalendaringServiceClientWrapperService csClientWrapperService =
            mock(CalendaringServiceClientWrapperService.class);
    private FulfillmentInfoService fulfillmentInfoService;

    @BeforeEach
    void init() {
        fulfillmentClient = Mockito.mock(FulfillmentClient.class);
        RequestItemRepository requestItemRepository = Mockito.mock(RequestItemRepository.class);
        SupplierRepository supplierRepository = Mockito.mock(SupplierRepository.class);
        ShopRequestFetchingService shopRequestFetchingService = Mockito.mock(ShopRequestFetchingService.class);
        ShopRequestModificationService shopRequestModificationService =
                Mockito.mock(ShopRequestModificationService.class);
        EnvironmentParamService environmentParamService = Mockito.mock(EnvironmentParamService
                .class);

        RequestRealSupplierInfoRepository requestRealSupplierInfoRepository =
                mock(RequestRealSupplierInfoRepository.class);
        fulfillmentInfoService = Mockito.mock(FulfillmentInfoService.class);

        RequestSubTypeService requestSubTypeService =  mock(RequestSubTypeService.class);
        LgwClientInboundConverter clientConverter = new LgwClientInboundConverter(concreteEnvironmentParamService,
            new LgwClientStatusConverter(), shopRequestFetchingService, requestRealSupplierInfoRepository,
                requestSubTypeService, mock(ShopRequestRepository.class), fulfillmentInfoService);
        LgwToFfwfConverter lgwToFfwfConverter = new LgwToFfwfConverter();

        lgwRequestSupplyService = new LgwRequestSupplyService(
                fulfillmentClient,
                environmentParamService,
                requestItemRepository,
                clientConverter,
                new LgwClientStatusConverter(),
                supplierRepository,
                shopRequestFetchingService,
                shopRequestModificationService,
                lgwToFfwfConverter,
                null,
                concreteEnvironmentParamService,
                mock(TimeSlotsService.class),
                csClientWrapperService,
                mock(RequestApiTypeValidationService.class),
                requestSubTypeService
        );
        RequestSubTypeEntity requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setLgwTypeForSendToService("DEFAULT");
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(new TypeSubtype(RequestType.SUPPLY, any())))
                .thenReturn(requestSubTypeEntity);
    }

    @Test
    void passExternalRequestIdAsPartnerIdWhileCreate1PInboundToInforWarehouse() throws Exception {
        final ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, RequestType.SUPPLY, EXTERNAL_REQUEST_ID);
        final Inbound expectedInbound = createExpectedInbound(InboundType.DEFAULT, null);

        lgwRequestSupplyService.pushRequest(shopRequest);

        verify(fulfillmentClient).createInbound(expectedInbound, new Partner(INFOR_WAREHOUSE_ID));
    }

    @Test
    void passWH2WHInboundTypeForMovementSupply() throws Exception {
        final ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, RequestType.MOVEMENT_SUPPLY, null);
        final Inbound expectedInbound = createExpectedInbound(InboundType.WH2WH, null);

        lgwRequestSupplyService.pushRequest(shopRequest);

        verify(fulfillmentClient).createInbound(expectedInbound, new Partner(INFOR_WAREHOUSE_ID));
    }

    private ShopRequest createShopRequest(long requestId, RequestType requestType, String externalRequestId) {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(requestId);
        shopRequest.setServiceId(INFOR_WAREHOUSE_ID);
        shopRequest.setType(requestType);
        shopRequest.setStockType(StockType.EXPIRED);
        shopRequest.setStockTypeTo(StockType.DEFECT);
        shopRequest.setRequestedDate(REQUESTED_DATE);
        shopRequest.setStatus(RequestStatus.VALIDATED);
        shopRequest.setExternalRequestId(externalRequestId);
        shopRequest.setSupplier(createFirstPartySupplier());
        return shopRequest;
    }

    private Supplier createFirstPartySupplier() {
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.FIRST_PARTY);
        return supplier;
    }

    private Inbound createExpectedInbound(InboundType inboundType, String externalRequestId) {
        return new Inbound.InboundBuilder(createResourceId(String.valueOf(REQUEST_ID_1), externalRequestId),
                inboundType,
                Collections.emptyList(),
                new DateTimeInterval(WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE, INFOR_WAREHOUSE_ID),
                        WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE, INFOR_WAREHOUSE_ID)))
                .build();
    }

    private ResourceId createResourceId(String yandexId, String partnerId) {
        return ResourceId.builder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .setFulfillmentId(partnerId)
                .build();
    }
}
