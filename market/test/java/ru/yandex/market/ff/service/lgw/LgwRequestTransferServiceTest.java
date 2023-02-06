package ru.yandex.market.ff.service.lgw;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.exception.http.RequestNotFoundException;
import ru.yandex.market.ff.model.converter.BaseLgwClientConverter;
import ru.yandex.market.ff.model.converter.IdentifierConverter;
import ru.yandex.market.ff.model.converter.LgwClientStatusConverter;
import ru.yandex.market.ff.model.converter.LgwToFfwfConverter;
import ru.yandex.market.ff.model.converter.RegistryUnitDTOConverter;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.SupplierRepository;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestTransferService;
import ru.yandex.market.ff.service.implementation.lgw.RequestApiTypeValidationService;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Transfer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LgwRequestTransferServiceTest {

    private static final long REQUEST_ID_1 = 11L;
    private static final long REQUEST_ID_2 = 11L;
    private static final long INBOUND_ID_1 = 1110L;
    private static final long INBOUND_ID_2 = 1111L;
    private static final long SERVICE_ID = 100L;
    private static final String SERVICE_REQUEST_ID = "ServiceRequestId";

    private LgwRequestTransferService lgwRequestTransferService;
    private FulfillmentClient fulfillmentClient;
    private ShopRequestFetchingService shopRequestFetchingService;

    @BeforeEach
    void init() {
        fulfillmentClient = mock(FulfillmentClient.class);
        RequestItemRepository requestItemRepository = mock(RequestItemRepository.class);
        SupplierRepository supplierRepository = mock(SupplierRepository.class);
        shopRequestFetchingService = mock(ShopRequestFetchingService.class);
        ShopRequestModificationService shopRequestModificationService =
                mock(ShopRequestModificationService.class);
        LgwToFfwfConverter lgwToFfwfConverter = mock(LgwToFfwfConverter.class);
        CalendaringServiceClientWrapperService csClientWrapperService =
                mock(CalendaringServiceClientWrapperService.class);

        when(shopRequestFetchingService.getRequestOrThrow(INBOUND_ID_1))
                .thenReturn(createShopRequest(REQUEST_ID_2, INBOUND_ID_2, null, null));

        ConcreteEnvironmentParamService concreteEnvironmentParamService =
                mock(ConcreteEnvironmentParamService.class);
        LgwClientStatusConverter statusConverter = new LgwClientStatusConverter();
        BaseLgwClientConverter baseLgwClientConverter = new BaseLgwClientConverter(concreteEnvironmentParamService);
        IdentifierConverter identifierConverter = new IdentifierConverter(new RegistryUnitDTOConverter());

        lgwRequestTransferService = new LgwRequestTransferService(
                fulfillmentClient,
                requestItemRepository,
                statusConverter,
                baseLgwClientConverter,
                supplierRepository,
                shopRequestFetchingService,
                shopRequestModificationService,
                lgwToFfwfConverter,
                mock(TimeSlotsService.class),
                identifierConverter,
                concreteEnvironmentParamService,
                csClientWrapperService,
                mock(RequestApiTypeValidationService.class)
        );
    }

    @Test
    void shouldSuccessCreateTransferBetweenExpiredAndDefect() throws Exception {
        ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, INBOUND_ID_1, StockType.EXPIRED, StockType.DEFECT);
        Transfer targetTransfer = createTargetTransfer(
                createResourceId(Long.toString(REQUEST_ID_1), SERVICE_REQUEST_ID),
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.EXPIRED,
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.DEFECT);

        lgwRequestTransferService.pushRequest(shopRequest);

        verify(fulfillmentClient).createTransfer(targetTransfer, new Partner(SERVICE_ID));
    }

    @Test
    void shouldSuccessCreateTransferBetweenCisQuarantineAndFit() throws Exception {
        ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, INBOUND_ID_1, StockType.CIS_QUARANTINE,
                StockType.FIT);
        Transfer targetTransfer = createTargetTransfer(
                createResourceId(Long.toString(REQUEST_ID_1), SERVICE_REQUEST_ID),
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.CIS_QUARANTINE,
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.FIT);

        lgwRequestTransferService.pushRequest(shopRequest);

        verify(fulfillmentClient).createTransfer(targetTransfer, new Partner(SERVICE_ID));
    }

    @Test
    void shouldSuccessCreateUtilizationTransfer() throws Exception {
        when(shopRequestFetchingService.getRequestOrThrow(anyLong()))
                .thenThrow(new RequestNotFoundException(INBOUND_ID_1));

        ShopRequest shopRequest = createShopRequest(REQUEST_ID_1, null, StockType.EXPIRED,
                StockType.PLAN_UTILIZATION);
        Transfer targetTransfer = createTargetTransfer(
                null,
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.EXPIRED,
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.PLAN_UTILIZATION);

        lgwRequestTransferService.pushRequest(shopRequest);

        verify(fulfillmentClient).createTransfer(targetTransfer, new Partner(SERVICE_ID));
    }

    private static ShopRequest createShopRequest(long requestId, Long inboundId, StockType from, StockType to) {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(requestId);
        shopRequest.setServiceId(SERVICE_ID);
        shopRequest.setType(RequestType.TRANSFER);
        shopRequest.setStockType(from);
        shopRequest.setStockTypeTo(to);
        shopRequest.setRequestedDate(LocalDateTime.of(2018, 1, 1, 10, 10, 10));
        shopRequest.setStatus(RequestStatus.VALIDATED);
        shopRequest.setInboundId(inboundId);
        shopRequest.setServiceRequestId(SERVICE_REQUEST_ID);
        return shopRequest;
    }

    private static Transfer createTargetTransfer(
            ResourceId inboundId,
            ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType from,
            ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType to) {
        return new Transfer(
                createResourceId(Long.toString(REQUEST_ID_1), null),
                inboundId,
                from,
                to,
                Collections.emptyList()
        );
    }

    private static ResourceId createResourceId(String yandexId, String partnerId) {
        return ResourceId.builder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .setFulfillmentId(partnerId)
                .build();
    }
}
