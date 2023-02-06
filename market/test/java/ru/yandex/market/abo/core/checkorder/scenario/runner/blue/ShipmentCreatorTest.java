package ru.yandex.market.abo.core.checkorder.scenario.runner.blue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderStatus;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.core.checkorder.CheckOrderCreationException;
import ru.yandex.market.abo.core.checkorder.CheckOrderDbService;
import ru.yandex.market.abo.core.checkorder.model.CheckOrder;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.core.prepay.PrepayRequestManager;
import ru.yandex.market.abo.core.supplier.Supplier;
import ru.yandex.market.abo.core.supplier.SupplierService;
import ru.yandex.market.abo.cpa.lms.model.LmsPartnerInfo;
import ru.yandex.market.abo.cpa.lms.model.ShipmentType;
import ru.yandex.market.abo.cpa.lms.stat.SupplierLogistics;
import ru.yandex.market.abo.cpa.lms.stat.SupplierLogisticsService;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.logistics.lom.client.LomClientImpl;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.management.client.LmsHttpClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 27/04/2020.
 */
class ShipmentCreatorTest {
    private static final Long SHOP_ID = 234234234235L;
    private static final Long SHIPMENT_ID = 423525L;
    private static final Long PREPAY_REQ_ID = 123123L;
    private static final Long WAREHOUSE_ID = 523L;
    private static final Long PARTNER_ID = 23145L;
    private static final Long DELIVERY_SERVICE_ID = 568678L;
    private static final Long MARKET_ID_TO = 5345323333L;
    private static final Long MARKET_ID_FROM = 34832322L;
    @InjectMocks
    ShipmentCreator shipmentCreator;
    @Mock
    SupplierLogisticsService dbLmsStore;
    @Mock
    SupplierService supplierService;
    @Mock
    LomClientImpl lomClient;
    @Mock
    LmsHttpClient lmsClient;
    @Mock
    PrepayRequestManager prepayRequestManager;
    @Mock
    CheckOrderDbService checkOrderDbService;

    @Mock
    Order order;
    LmsPartnerInfo lmsData = new LmsPartnerInfo(
            PARTNER_ID,
            DELIVERY_SERVICE_ID,
            0L,
            ShipmentType.WITHDRAW,
            SHOP_ID
    );
    @Mock
    Delivery delivery;
    @Mock
    Parcel parcel;
    @Mock
    PrepayRequestDTO prepayRequest;
    @Mock
    LogisticsPointResponse logisticsPoint;
    @Mock
    Supplier supplier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(order.getShopId()).thenReturn(SHOP_ID);
        when(order.getDelivery()).thenReturn(delivery);
        when(delivery.getParcels()).thenReturn(List.of(parcel));

        when(dbLmsStore.findAny(SHOP_ID)).thenReturn(Optional.of(lmsData));

        doReturn(PageResult.empty(new Pageable(0, 1, null))).when(lomClient).searchShipments(any(), any());
        when(lmsClient.getLogisticsPoints(any())).thenReturn(List.of(logisticsPoint));
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID))
                .thenReturn(Optional.of(PartnerResponse.newBuilder().marketId(MARKET_ID_TO).build()));

        when(logisticsPoint.getId()).thenReturn(WAREHOUSE_ID);

        when(prepayRequestManager.getPrepayRequest(PREPAY_REQ_ID, SHOP_ID)).thenReturn(prepayRequest);
        when(prepayRequest.getMarketId()).thenReturn(MARKET_ID_FROM);

        when(supplier.getId()).thenReturn(SHOP_ID);
        when(supplier.getRequestId()).thenReturn(PREPAY_REQ_ID);
        when(supplierService.load(SHOP_ID)).thenReturn(Optional.of(supplier));
    }

    @Test
    void create() throws CheckOrderCreationException {
        shipmentCreator.createFrom(order);
        verify(lomClient, never()).cancelShipmentApplication(anyLong(), anyLong());
        verify(lomClient).createShipmentApplication(any());
    }

    @Test
    void noDbLms() {
        when(dbLmsStore.findAny(SHOP_ID)).thenReturn(Optional.empty());
        assertThrows(CheckOrderCreationException.class, () -> shipmentCreator.createFrom(order));
    }

    @Test
    void noPrepayRequest() {
        when(prepayRequestManager.getPrepayRequest(anyLong(), eq(SHOP_ID))).thenReturn(null);
        assertThrows(CheckOrderCreationException.class, () -> shipmentCreator.createFrom(order));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shipmentAlreadyExists(boolean shipmentFromPreviousOfflineOrder) throws CheckOrderCreationException {
        doReturn(List.of(previousOfflineOrder(shipmentFromPreviousOfflineOrder ? SHIPMENT_ID : SHIPMENT_ID + 1)))
                .when(checkOrderDbService).findOfflineChecks(SHOP_ID);
        doReturn(new PageResult<ShipmentSearchDto>()
                .setData(List.of(ShipmentSearchDto.builder().applicationId(SHIPMENT_ID).build()))
                .setPageNumber(1)
                .setTotalPages(1)
                .setSize(1)
                .setTotalElements(1)
        ).when(lomClient).searchShipments(any(), any());

        shipmentCreator.createFrom(order);
        if (shipmentFromPreviousOfflineOrder) {
            verify(lomClient).cancelShipmentApplication(SHIPMENT_ID, MARKET_ID_FROM);
        }
        verify(lomClient).createShipmentApplication(any());
    }

    private CheckOrder previousOfflineOrder(long shipmentId) {
        CheckOrderScenario scenario = new CheckOrderScenario(CheckOrderScenarioType.OFFLINE_ORDER, OrderProcessMethod.API);
        scenario.setStatus(CheckOrderScenarioStatus.SUCCESS);
        scenario.addShipment(shipmentId);
        CheckOrder checkOrder = new CheckOrder(SHOP_ID, false, List.of(scenario));
        checkOrder.setStatus(CheckOrderStatus.SUCCESS);
        return checkOrder;
    }

    @Test
    void noLogisticPoint() {
        when(lmsClient.getLogisticsPoints(any())).thenReturn(List.of());
        assertThrows(CheckOrderCreationException.class, () -> shipmentCreator.createFrom(order));
    }
}
