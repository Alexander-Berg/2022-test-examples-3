package ru.yandex.market.ff.service.lgw;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.CommonTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.enums.FulfillmentServiceStatus;
import ru.yandex.market.ff.enums.FulfillmentServiceType;
import ru.yandex.market.ff.model.TypeSubtype;
import ru.yandex.market.ff.model.converter.LgwClientOutboundConverter;
import ru.yandex.market.ff.model.converter.LgwClientStatusConverter;
import ru.yandex.market.ff.model.dto.AddressDTO;
import ru.yandex.market.ff.model.entity.FulfillmentInfo;
import ru.yandex.market.ff.model.entity.LogisticsPoint;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.implementation.lgw.FulfilmentRequestClient;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestFixLostInventoryingWithdrawService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestWithdrawService;
import ru.yandex.market.ff.service.implementation.lgw.RequestApiTypeValidationService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LgwRequestCreateFixLostInventoryingOutboundTest extends CommonTest {

    public static final LocalDateTime REQUESTED_DATE =
            LocalDateTime.of(2018, 1, 1, 10, 10, 10);
    private static final long REQUEST_ID_1 = 11L;
    private static final long INFOR_WAREHOUSE_ID = 147L;
    private static final String EXTERNAL_REQUEST_ID = "externalRequestId";

    private LgwRequestFixLostInventoryingWithdrawService lgwRequestFixLostInventoryingWithdrawService;
    private FulfillmentInfoService fulfillmentInfoService;

    @BeforeEach
    void init() {
        ConcreteEnvironmentParamService concreteEnvironmentParamService =
                mock(ConcreteEnvironmentParamService.class);
        fulfillmentInfoService = mock(FulfillmentInfoService.class);
        RequestSubTypeService subTypeService = mock(RequestSubTypeService.class);
        ShopRequestFetchingService shopRequestFetchingService = mock(ShopRequestFetchingService.class);
        LgwClientOutboundConverter lgwClientConverter = new LgwClientOutboundConverter(concreteEnvironmentParamService,
            new LgwClientStatusConverter(), subTypeService, shopRequestFetchingService);
        FulfilmentRequestClient fulfilmentRequestClient = mock(FulfilmentRequestClient.class);
        fulfillmentInfoService = mock(FulfillmentInfoService.class);
        ShopRequestModificationService shopRequestModificationService =
                mock(ShopRequestModificationService.class);
        LgwRequestWithdrawService lgwRequestWithdrawService = mock(LgwRequestWithdrawService.class);
        RequestApiTypeValidationService requestApiTypeValidationService = mock(RequestApiTypeValidationService.class);
        lgwRequestFixLostInventoryingWithdrawService = new LgwRequestFixLostInventoryingWithdrawService(
                fulfillmentInfoService, lgwClientConverter, fulfilmentRequestClient,
                shopRequestModificationService, lgwRequestWithdrawService, requestApiTypeValidationService
        );
        Mockito.doReturn(null).when(subTypeService)
                .getEntityByRequestTypeAndSubtype(new TypeSubtype(RequestType.MOVEMENT_WITHDRAW, "DEFAULT"));
    }

    @Test
    void pushRequest() {
        final ShopRequest shopRequest = createShopRequest();
        when(fulfillmentInfoService.getFulfillmentInfoOrThrow(anyLong()))
                .thenReturn(new FulfillmentInfo(REQUEST_ID_1, "", FulfillmentServiceStatus.ON,
                        FulfillmentServiceType.SUPPLIER, "", "", ""));
        lgwRequestFixLostInventoryingWithdrawService.pushRequest(shopRequest);
    }

    private ShopRequest createShopRequest() {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(REQUEST_ID_1);
        shopRequest.setServiceRequestId("1234");
        shopRequest.setLogisticsPoint(createLogisticsPoint());
        shopRequest.setServiceId(INFOR_WAREHOUSE_ID);
        shopRequest.setType(RequestType.FIX_LOST_INVENTORYING_WITHDRAW);
        shopRequest.setStockType(StockType.FIX_LOST_INVENTARIZATION);
        shopRequest.setRequestedDate(REQUESTED_DATE);
        shopRequest.setStatus(RequestStatus.VALIDATED);
        shopRequest.setExternalRequestId(EXTERNAL_REQUEST_ID);
        shopRequest.setSupplier(createFirstPartySupplier());
        return shopRequest;
    }

    private LogisticsPoint createLogisticsPoint() {
        final LogisticsPoint logisticsPoint = new LogisticsPoint(123L);
        logisticsPoint.setExternalId("123");
        logisticsPoint.setAddress(AddressDTO.builder().build());
        return logisticsPoint;
    }

    private Supplier createFirstPartySupplier() {
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.FIRST_PARTY);
        return supplier;
    }
}
