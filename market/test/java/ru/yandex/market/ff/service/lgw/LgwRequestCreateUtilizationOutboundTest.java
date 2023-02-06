package ru.yandex.market.ff.service.lgw;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.CommonTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.model.converter.LgwClientOutboundConverter;
import ru.yandex.market.ff.model.converter.LgwClientStatusConverter;
import ru.yandex.market.ff.model.converter.LgwToFfwfConverter;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.SupplierRepository;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.service.implementation.lgw.FulfilmentRequestClient;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestCommonClient;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestUtilizationWithdrawService;
import ru.yandex.market.ff.service.implementation.lgw.RequestApiTypeValidationService;
import ru.yandex.market.ff.util.WarehouseDateTimeUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Courier;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Outbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LgwRequestCreateUtilizationOutboundTest extends CommonTest {

    private static final long REQUEST_ID_1 = 11L;
    private static final long INFOR_WAREHOUSE_ID = 147L;
    public static final LocalDateTime REQUESTED_DATE =
            LocalDateTime.of(2018, 1, 1, 10, 10, 10);
    private static final Courier COURIER =
            new Courier.CourierBuilder(List.of(new Person.PersonBuilder("Утилизация").build())).build();
    private static final LegalEntity OWNER = new LegalEntity.LegalEntityBuilder("Утилизация").build();

    private LgwRequestUtilizationWithdrawService lgwRequestUtilizationWithdrawService;
    private FulfillmentClient fulfillmentClient;

    @BeforeEach
    void init() {
        fulfillmentClient = Mockito.mock(FulfillmentClient.class);
        RequestItemRepository requestItemRepository = Mockito.mock(RequestItemRepository.class);
        SupplierRepository supplierRepository = Mockito.mock(SupplierRepository.class);
        ShopRequestFetchingService shopRequestFetchingService = Mockito.mock(ShopRequestFetchingService.class);
        ShopRequestModificationService shopRequestModificationService =
                Mockito.mock(ShopRequestModificationService.class);
        ConcreteEnvironmentParamService concreteEnvironmentParamService =
                Mockito.mock(ConcreteEnvironmentParamService.class);
        RequestSubTypeService subTypeService = mock(RequestSubTypeService.class);
        LgwClientOutboundConverter outboundConverter = new LgwClientOutboundConverter(concreteEnvironmentParamService,
            new LgwClientStatusConverter(), subTypeService, shopRequestFetchingService);
        LgwToFfwfConverter lgwToFfwfConverter = new LgwToFfwfConverter();
        MbiApiClient mbiApiClient = mock(MbiApiClient.class);
        LgwRequestCommonClient lgwRequestCommonClient = mock(FulfilmentRequestClient.class);
        FulfilmentRequestClient fulfilmentRequestClient = mock(FulfilmentRequestClient.class);
        CalendaringServiceClientWrapperService csClientWrapperService =
                mock(CalendaringServiceClientWrapperService.class);
        RequestSubTypeService requestSubTypeService = mock(RequestSubTypeService.class);

        RequestSubTypeEntity requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setUseRegistryMethodsToSendToService(false);
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(requestSubTypeEntity);

        lgwRequestUtilizationWithdrawService = new LgwRequestUtilizationWithdrawService(
                fulfillmentClient,
                requestItemRepository,
                outboundConverter,
                new LgwClientStatusConverter(),
                supplierRepository,
                shopRequestFetchingService,
                shopRequestModificationService,
                mbiApiClient,
                lgwToFfwfConverter,
                lgwRequestCommonClient,
                fulfilmentRequestClient,
                mock(TimeSlotsService.class),
                concreteEnvironmentParamService,
                csClientWrapperService,
                mock(RequestApiTypeValidationService.class),
                requestSubTypeService
        );
    }

    @Test
    void createUtilizationOutbound() throws Exception {
        ShopRequest shopRequest = createShopRequest(REQUEST_ID_1);
        Outbound expectedOutbound = createExpectedOutbound();

        lgwRequestUtilizationWithdrawService.pushRequest(shopRequest);

        verify(fulfillmentClient).createOutbound(expectedOutbound, new Partner(INFOR_WAREHOUSE_ID));
    }

    private ShopRequest createShopRequest(long requestId) {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(requestId);
        shopRequest.setServiceId(INFOR_WAREHOUSE_ID);
        shopRequest.setType(RequestType.UTILIZATION_WITHDRAW);
        shopRequest.setStockType(StockType.PLAN_UTILIZATION);
        shopRequest.setRequestedDate(REQUESTED_DATE);
        shopRequest.setStatus(RequestStatus.VALIDATED);
        shopRequest.setSupplier(createSupplier());
        return shopRequest;
    }

    private Supplier createSupplier() {
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.THIRD_PARTY);
        return supplier;
    }

    private Outbound createExpectedOutbound() {
        return new Outbound.OutboundBuilder(
                ResourceId.builder().setYandexId(String.valueOf(REQUEST_ID_1)).build(),
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.PLAN_UTILIZATION,
                Collections.emptyList(),
                COURIER,
                OWNER,
                new DateTimeInterval(WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE, INFOR_WAREHOUSE_ID),
                        WarehouseDateTimeUtils.toOffsetDateTime(REQUESTED_DATE, INFOR_WAREHOUSE_ID))
        ).build();
    }
}
