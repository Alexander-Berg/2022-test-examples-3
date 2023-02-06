package ru.yandex.market.abo.core.checkorder.scenario.runner;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType;
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorDetail;
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorParam;
import ru.yandex.market.abo.core.checkorder.CheckOrderCreationException;
import ru.yandex.market.abo.core.checkorder.CheckOrderOffersProvider;
import ru.yandex.market.abo.core.checkorder.CheckOrderService;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.core.checkorder.scenario.runner.blue.RejectedByPartnerScenarioRunner;
import ru.yandex.market.abo.core.checkorder.scenario.runner.blue.WarehouseRegionService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeatureListItem;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerIndexState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 11/11/2019.
 */
class RejectedByPiScenarioRunnerTest {
    private static final Long SHOP_ID = 3123L;
    private static final Long ORDER_ID = 423545L;

    RejectedByPartnerScenarioRunner runner;

    @Mock
    CheckOrderService checkOrderService;
    @Mock
    CheckOrderOffersProvider checkOrderOffersProvider;
    @Mock
    CheckOrderScenario scenario;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    WarehouseRegionService warehouseRegionService;
    @Mock
    ShopFeatureListItem shopFeature;
    @Mock
    Order order;
    @Mock
    Order previousOrderState;
    @Mock
    OrderHistoryEvent orderHistoryEvent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        runner = new RejectedByPartnerScenarioRunner(checkOrderService, mbiApiService, warehouseRegionService,
                null, null, checkOrderOffersProvider, null);
        when(scenario.getType()).thenReturn(CheckOrderScenarioType.REJECTED_BY_PARTNER);
        when(scenario.getStatus()).thenReturn(CheckOrderScenarioStatus.IN_PROGRESS);
        when(scenario.getShopId()).thenReturn(SHOP_ID);

        when(mbiApiService.getShopWithFeature(SHOP_ID, FeatureType.DROPSHIP)).thenReturn(shopFeature);
        when(mbiApiService.getIndexState(SHOP_ID)).thenReturn(new PartnerIndexState(SHOP_ID, false, true));

        when(shopFeature.isCpaPartnerInterface()).thenReturn(true);

        when(checkOrderService.getOrderHistoryEvents(any(), eq(ORDER_ID), any()))
                .thenReturn(List.of(orderHistoryEvent, orderHistoryEvent));
        when(orderHistoryEvent.getOrderAfter()).thenReturn(previousOrderState, order);

        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getShopId()).thenReturn(SHOP_ID);
        when(order.getCreationDate()).thenReturn(new Date());
        when(warehouseRegionService.chooseRegionForCheckOrder(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void initOrder() throws CheckOrderCreationException {
        when(checkOrderService.createOrder(any(), any())).thenReturn(order);
        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, runner.initOrder(scenario));
        verify(scenario).setOrderId(ORDER_ID);
        verify(scenario).setOrderCreationTime(any());
    }

    @Test
    void initFailed() throws CheckOrderCreationException {
        when(checkOrderService.createOrder(any(), any())).thenReturn(null);
        assertThrows(CheckOrderCreationException.class, () -> runner.initOrder(scenario));
    }

    @Test
    void checkProgress() {
        when(order.getStatus()).thenReturn(OrderStatus.PENDING);
        when(order.getCancellationRequest()).thenReturn(null);
        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, runner.checkProgress(scenario, order));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cancelled(boolean cancelledTooEarly) {
        when(order.getStatus()).thenReturn(OrderStatus.CANCELLED);
        when(previousOrderState.getStatus())
                .thenReturn(cancelledTooEarly ? OrderStatus.PLACING : OrderStatus.PENDING);

        if (cancelledTooEarly) {
            assertEquals(CheckOrderScenarioStatus.FAIL, runner.checkProgress(scenario, order));

            verify(scenario).setErrorType(CheckOrderScenarioErrorType.ORDER_CANCELLED_INCORRECTLY);
            verify(scenario).withErrorDetails(List.of(
                    new ScenarioErrorDetail(ScenarioErrorParam.EXPECTED_ORDER_SUBSTATUS, OrderStatus.PENDING),
                    new ScenarioErrorDetail(ScenarioErrorParam.ACTUAL_ORDER_SUBSTATUS, OrderStatus.PLACING)
            ));
        } else {
            assertEquals(CheckOrderScenarioStatus.SUCCESS, runner.checkProgress(scenario, order));
        }
    }

    @Test
    void timeout() {
        assertEquals(CheckOrderScenarioStatus.FAIL, runner.handleTimeout(scenario, order));
        verify(scenario).setErrorType(CheckOrderScenarioErrorType.ORDER_NOT_CANCELLED);
    }
}
