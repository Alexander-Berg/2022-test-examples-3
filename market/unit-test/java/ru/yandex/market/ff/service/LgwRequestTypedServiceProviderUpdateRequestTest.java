package ru.yandex.market.ff.service;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.ExternalOperationType;
import ru.yandex.market.ff.enums.FulfillmentServiceType;
import ru.yandex.market.ff.enums.PartnerApiType;
import ru.yandex.market.ff.model.bo.LGWRequestType;
import ru.yandex.market.ff.model.entity.FulfillmentInfo;
import ru.yandex.market.ff.model.entity.LogisticsPoint;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.enums.ApiType;
import ru.yandex.market.ff.repository.NewMovementFlowRequestsRepository;
import ru.yandex.market.ff.service.implementation.LgwRequestServiceImpl;
import ru.yandex.market.ff.service.implementation.LgwRequestTypedServiceProvider;
import ru.yandex.market.ff.service.implementation.lgw.FfApiRequestOrdersSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.FfApiRequestOrdersWithdrawService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestCrossdockService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestCustomerReturnSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestExpendableMaterialsSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestInboundSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestUtilizationWithdrawService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestWithdrawService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LgwRequestTypedServiceProviderUpdateRequestTest {

    private static final long REQUEST_ID = 123;

    private LgwRequestTypedServiceProvider serviceProvider;
    private LgwRequestService lgwRequestService;
    private NewMovementFlowRequestsRepository newMovementFlowRequestsRepository;

    private static Stream<Arguments> supportedTypes() {
        return Stream.of(
                Arguments.of(
                        LGWRequestType.asNotXdocForFfApi(RequestType.EXPENDABLE_MATERIALS),
                        LgwRequestExpendableMaterialsSupplyService.class
                ),
                Arguments.of(
                        LGWRequestType.asNotXdocForFfApi(RequestType.CUSTOMER_RETURN_SUPPLY),
                        LgwRequestCustomerReturnSupplyService.class
                ),
                Arguments.of(
                        LGWRequestType.asNotXdocForFfApi(RequestType.CROSSDOCK),
                        LgwRequestCrossdockService.class
                ),
                Arguments.of(
                        LGWRequestType.asNotXdocForFfApi(RequestType.WITHDRAW),
                        LgwRequestWithdrawService.class
                ),
                Arguments.of(
                        LGWRequestType.asNotXdocForFfApi(RequestType.UTILIZATION_WITHDRAW),
                        LgwRequestUtilizationWithdrawService.class
                )
        );
    }

    private static Stream<Arguments> supplyAndWithdrawTypes() {
        return Stream.of(
                Arguments.of(
                        LGWRequestType.asNotXdocForFfApi(RequestType.MOVEMENT_SUPPLY),
                        FfApiRequestOrdersSupplyService.class
                ),
                Arguments.of(
                        LGWRequestType.asNotXdocForFfApi(RequestType.MOVEMENT_WITHDRAW),
                        FfApiRequestOrdersWithdrawService.class
                )
        );
    }

    @ParameterizedTest
    @MethodSource("supportedTypes")
    void processPayloadCallsRightService(LGWRequestType lgwRequestType, Class<LgwRequestTypedService> serviceClass) {
        LgwRequestTypedService service = getServiceSetUpByProvider(serviceClass);

        ShopRequest request = createShopRequest(lgwRequestType);

        lgwRequestService.updateRequest(request);

        verify(serviceProvider).provide(lgwRequestType);
        verify(service).updateRequest(request);
    }

    @ParameterizedTest
    @MethodSource("supplyAndWithdrawTypes")
    void processPushCallsRightServiceWithApiType(LGWRequestType lgwRequestType,
                                                 Class<LgwRequestTypedService> serviceClass) {
        var service = getServiceSetUpByMovementFlowRequests(serviceClass);

        when(newMovementFlowRequestsRepository.exists(anyLong())).thenReturn(true);
        var shopRequest = createShopRequestWithApiType(lgwRequestType);

        lgwRequestService.pushRequest(shopRequest, false);

        verify(service).pushRequest(shopRequest);
    }

    @ParameterizedTest
    @MethodSource("supplyAndWithdrawTypes")
    void processPushCallsRightServiceNoApiType(LGWRequestType lgwRequestType,
                                               Class<LgwRequestTypedService> serviceClass) {
        var service = getServiceSetUpByMovementFlowRequests(serviceClass);

        when(newMovementFlowRequestsRepository.exists(anyLong())).thenReturn(true);
        var shopRequest = createShopRequest(lgwRequestType);

        lgwRequestService.pushRequest(shopRequest, false);

        verify(service).pushRequest(shopRequest);
    }

    private LgwRequestTypedService getServiceSetUpByProvider(Class<LgwRequestTypedService> serviceClass) {
        LgwRequestTypedService service = getLgwRequestTypedServicesMocks(serviceClass);
        SupplyPutInboundService supplyPutInboundService = mock(SupplyPutInboundService.class);
        when(supplyPutInboundService.isRequestToMigrateToPutInbound(any())).thenReturn(true);
        serviceProvider = Mockito.spy(new LgwRequestTypedServiceProvider(List.of(service)));
        lgwRequestService = new LgwRequestServiceImpl(
            serviceProvider, null, null,
            null, null, supplyPutInboundService, null,
                service instanceof LgwRequestInboundSupplyService ? (LgwRequestInboundSupplyService) service : null);
        return service;
    }

    private LgwRequestTypedService getServiceSetUpByMovementFlowRequests(Class<LgwRequestTypedService> serviceClass) {
        LgwRequestTypedService service = getLgwRequestTypedServicesMocksForMovementFlow(serviceClass);
        newMovementFlowRequestsRepository = mock(NewMovementFlowRequestsRepository.class);
        lgwRequestService = new LgwRequestServiceImpl(
            null, null, newMovementFlowRequestsRepository,
            service instanceof FfApiRequestOrdersSupplyService ? (FfApiRequestOrdersSupplyService) service : null,
            service instanceof FfApiRequestOrdersWithdrawService ? (FfApiRequestOrdersWithdrawService) service : null,
                null,
                service instanceof LgwRequestSupplyService ? (LgwRequestSupplyService) service : null,
                service instanceof LgwRequestInboundSupplyService ? (LgwRequestInboundSupplyService) service : null);
        return service;
    }

    @Nonnull
    private LgwRequestTypedService getLgwRequestTypedServicesMocks(
            Class<LgwRequestTypedService> serviceClass
    ) {
        LgwRequestTypedService mock = mock(serviceClass);
        when(mock.getTypes()).thenCallRealMethod();
        return mock;
    }

    @Nonnull
    private LgwRequestTypedService getLgwRequestTypedServicesMocksForMovementFlow(
            Class<LgwRequestTypedService> serviceClass
    ) {
        return mock(serviceClass);
    }

    @Nonnull
    private ShopRequest createShopRequest(LGWRequestType lgwRequestType) {
        ShopRequest request = new ShopRequest();
        request.setId(REQUEST_ID);
        request.setType(lgwRequestType.getRequestType());
        request.setLogisticsPoint(createLogisticsPoint(restoreFFServiceTypeFromLGWRequestType(lgwRequestType)));
        if (lgwRequestType.isXDocExecutor()) {
            request.setExternalOperationType(ExternalOperationType.XDOC);
        }
        return request;
    }

    @NotNull
    private FulfillmentServiceType restoreFFServiceTypeFromLGWRequestType(LGWRequestType lgwRequestType) {
        if (lgwRequestType.getPartnerApiType() == PartnerApiType.DS_API) {
            return FulfillmentServiceType.DELIVERY;
        }
        return FulfillmentServiceType.FULFILLMENT;
    }

    @NotNull
    private LogisticsPoint createLogisticsPoint(FulfillmentServiceType serviceType) {
        LogisticsPoint logisticsPoint = new LogisticsPoint();
        logisticsPoint.setPartnerInfo(createFulfillmentInfo(serviceType));
        return logisticsPoint;
    }

    @NotNull
    private FulfillmentInfo createFulfillmentInfo(FulfillmentServiceType serviceType) {
        FulfillmentInfo partnerInfo = new FulfillmentInfo();
        partnerInfo.setType(serviceType);
        return partnerInfo;
    }

    private ShopRequest createShopRequestWithApiType(LGWRequestType lgwRequestType) {
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(REQUEST_ID);
        shopRequest.setType(lgwRequestType.getRequestType());
        shopRequest.setApiType(
                lgwRequestType.getPartnerApiType() == PartnerApiType.DS_API ? ApiType.DELIVERY : ApiType.FULFILLMENT);
        return shopRequest;
    }

}
