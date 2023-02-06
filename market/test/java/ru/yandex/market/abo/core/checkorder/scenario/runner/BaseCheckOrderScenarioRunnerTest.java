package ru.yandex.market.abo.core.checkorder.scenario.runner;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorDetail;
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorParam;
import ru.yandex.market.abo.core.business.BusinessService;
import ru.yandex.market.abo.clch.model.DeliveryRegion;
import ru.yandex.market.abo.core.checkorder.CheckOrderCreationException;
import ru.yandex.market.abo.core.checkorder.CheckOrderOffersProvider;
import ru.yandex.market.abo.core.checkorder.CheckOrderService;
import ru.yandex.market.abo.core.checkorder.CreateOrderParam;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.core.checkorder.scenario.runner.blue.WarehouseRegionService;
import ru.yandex.market.abo.core.offer.report.IndexType;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.region.ShopDeliveryRegionService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 26/10/2019.
 */
class BaseCheckOrderScenarioRunnerTest {
    private static final long ORDER_ID = 112231;
    private static final long SHOP_ID = 32423344L;

    private BaseCheckOrderScenarioRunner baseRunner;
    @Mock
    CheckOrderService checkOrderService;
    @Mock
    CheckOrderOffersProvider checkOrderOffersProvider;
    @Mock
    BusinessService businessService;
    @Mock
    CheckOrderScenario scenario;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    WarehouseRegionService warehouseRegionService;
    @Mock
    ShopDeliveryRegionService shopDeliveryRegionService;
    @Mock
    Order order;

    @Mock
    Order cancelledOrder;
    @Mock
    Order processingOrder;
    @Mock
    Order pendingOrder;

    @Mock
    OrderHistoryEvent cancelledEvent;
    @Mock
    OrderHistoryEvent processingEvent;
    @Mock
    OrderHistoryEvent pendingEvent;


    @BeforeEach
    void setUp() throws CheckOrderCreationException {
        MockitoAnnotations.openMocks(this);
        baseRunner = spy(runnerStub(Color.BLUE));
        doCallRealMethod().when(baseRunner).checkProgress(any());
        doCallRealMethod().when(baseRunner).basicOrderParam(anyLong(), any());

        when(scenario.getOrderId()).thenReturn(ORDER_ID);
        when(scenario.getShopId()).thenReturn(SHOP_ID);

        when(checkOrderService.getOrder(any(), eq(ORDER_ID), any(), any())).thenReturn(order);
        when(checkOrderService.createOrder(any(), anyList())).thenReturn(order);

        mockHistoryEvent(cancelledEvent, 3L, cancelledOrder, OrderStatus.CANCELLED);
        mockHistoryEvent(processingEvent, 2L, processingOrder, OrderStatus.PROCESSING);
        mockHistoryEvent(pendingEvent, 1L, pendingOrder, OrderStatus.PENDING);

        when(order.getStatus()).thenReturn(OrderStatus.PROCESSING);
        when(order.getCreationDate()).thenReturn(new Date());
        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getShopId()).thenReturn(SHOP_ID);

        when(baseRunner.reachedTimeout(any())).thenReturn(false);
        when(mbiApiService.resolveIndexType(SHOP_ID)).thenReturn(IndexType.SANDBOX);
        when(warehouseRegionService.chooseRegionForCheckOrder(anyLong())).thenReturn(Optional.empty());
    }

    private void mockHistoryEvent(OrderHistoryEvent event, Long id, Order order, OrderStatus orderStatus) {
        when(event.getId()).thenReturn(id);
        when(event.getOrderAfter()).thenReturn(order);
        when(order.getStatus()).thenReturn(orderStatus);
        when(order.getId()).thenReturn(ORDER_ID);
    }

    @Test
    void testBaseCheckSuccess() {
        baseRunner.checkProgress(scenario);
        verify(checkOrderService, never()).payFakeOrder(eq(order), any());
        verify(baseRunner, never()).handleTimeout(scenario, order);
        verify(baseRunner).checkProgress(scenario, order);
    }

    @ParameterizedTest
    @CsvSource({"IN_PROGRESS", "SUCCESS"})
    void testExpired(CheckOrderScenarioStatus current) {
        when(baseRunner.reachedTimeout(any())).thenReturn(true);
        when(baseRunner.checkProgress(scenario, order)).thenReturn(current);
        CheckOrderScenarioStatus timeoutStatus = CheckOrderScenarioStatus.FAIL;
        when(baseRunner.handleTimeout(scenario, order)).thenReturn(timeoutStatus);

        CheckOrderScenarioStatus newStatus = baseRunner.checkProgress(scenario);
        if (current.isTerminal()) {
            verify(baseRunner, never()).handleTimeout(scenario, order);
            assertEquals(current, newStatus);
        } else {
            verify(baseRunner).handleTimeout(scenario, order);
            assertEquals(timeoutStatus, newStatus);
        }
    }

    @Test
    void testUnpaidOrder() {
        when(order.getStatus()).thenReturn(OrderStatus.UNPAID);
        CheckOrderScenarioStatus result = baseRunner.checkProgress(scenario);
        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, result);

        verify(checkOrderService).payFakeOrder(eq(order), eq(IndexType.SANDBOX));
        verify(baseRunner, never()).checkProgress(scenario, order);
        verify(baseRunner, never()).handleTimeout(scenario, order);
    }

    @Test
    void initOrder() throws CheckOrderCreationException {
        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, baseRunner.initOrder(scenario));
        verify(scenario).setOrderId(ORDER_ID);
        verify(scenario).setOrderCreationTime(any());
        verify(scenario).addRegion(anyLong());
    }

    @Test
    void initWhiteOrder() throws CheckOrderCreationException {
        BaseCheckOrderScenarioRunner whiteBaseRunner = spy(runnerStub(Color.WHITE));
        doCallRealMethod().when(whiteBaseRunner).checkProgress(any());
        doCallRealMethod().when(whiteBaseRunner).basicOrderParam(anyLong(), any());

        when(shopDeliveryRegionService.getDeliveryRegionsFromTarifficator(eq(SHOP_ID)))
                .thenReturn(List.of(
                        new DeliveryRegion(2, "Санкт-Петербург", true, true),
                        new DeliveryRegion(62, "Красноярск", false, true)
                ));

        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, whiteBaseRunner.initOrder(scenario));
        verify(scenario).setOrderId(ORDER_ID);
        verify(scenario).setOrderCreationTime(any());
        verify(scenario).addRegion((long) 2);
    }

    @Test
    void initWhiteOrderWithNoCourierDelivery() throws CheckOrderCreationException {
        BaseCheckOrderScenarioRunner whiteBaseRunner = spy(runnerStub(Color.WHITE));
        doCallRealMethod().when(whiteBaseRunner).checkProgress(any());
        doCallRealMethod().when(whiteBaseRunner).basicOrderParam(anyLong(), any());

        when(shopDeliveryRegionService.getDeliveryRegionsFromTarifficator(eq(SHOP_ID)))
                .thenReturn(List.of(
                        new DeliveryRegion(2, "Санкт-Петербург", false, false),
                        new DeliveryRegion(62, "Краснярск", false, false),
                        new DeliveryRegion(216, "Зеленоград", true, false)
                ));

        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, whiteBaseRunner.initOrder(scenario));
        verify(scenario).setOrderId(ORDER_ID);
        verify(scenario).setOrderCreationTime(any());
        verify(scenario).addRegion((long) 216);
    }

    @Test
    void initWhiteOrderWithNoDelivery() throws CheckOrderCreationException {
        BaseCheckOrderScenarioRunner whiteBaseRunner = spy(runnerStub(Color.WHITE));
        doCallRealMethod().when(whiteBaseRunner).checkProgress(any());
        doCallRealMethod().when(whiteBaseRunner).basicOrderParam(anyLong(), any());

        when(shopDeliveryRegionService.getDeliveryRegionsFromTarifficator(eq(SHOP_ID)))
                .thenReturn(Collections.emptyList());

        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, whiteBaseRunner.initOrder(scenario));
        verify(scenario).setOrderId(ORDER_ID);
        verify(scenario).setOrderCreationTime(any());
        verify(scenario).addRegion((long) Regions.MOSCOW);
    }

    @Test
    void initWhiteOrderWithoutOwnDeliveryRegion() throws CheckOrderCreationException {
        BaseCheckOrderScenarioRunner whiteBaseRunner = spy(runnerStub(Color.WHITE));
        doCallRealMethod().when(whiteBaseRunner).checkProgress(any());
        doCallRealMethod().when(whiteBaseRunner).basicOrderParam(anyLong(), any());

        when(shopDeliveryRegionService.getDeliveryRegionsFromTarifficator(eq(SHOP_ID)))
                .thenReturn(List.of(
                        new DeliveryRegion(2, "Санкт-Петербург", true, false),
                        new DeliveryRegion(62, "Красноярск", false, true)
                ));

        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, whiteBaseRunner.initOrder(scenario));
        verify(scenario).setOrderId(ORDER_ID);
        verify(scenario).setOrderCreationTime(any());
        verify(scenario).addRegion((long) 62);
    }

    @Test
    void initWhiteOrderWithDefaultRegion() throws CheckOrderCreationException {
        BaseCheckOrderScenarioRunner whiteBaseRunner = spy(runnerStub(Color.WHITE));
        doCallRealMethod().when(whiteBaseRunner).checkProgress(any());
        doCallRealMethod().when(whiteBaseRunner).basicOrderParam(anyLong(), any());

        when(shopDeliveryRegionService.getDeliveryRegionsFromTarifficator(eq(SHOP_ID)))
                .thenReturn(List.of(
                        new DeliveryRegion(2, "Санкт-Петербург", false, false),
                        new DeliveryRegion(62, "Красноярск", false, false)
                ));

        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, whiteBaseRunner.initOrder(scenario));
        verify(scenario).setOrderId(ORDER_ID);
        verify(scenario).setOrderCreationTime(any());
        verify(scenario).addRegion((long) 213);
    }

    @Test
    void noOrderCreated() throws CheckOrderCreationException {
        when(checkOrderService.createOrder(any(), anyList())).thenReturn(null);
        assertThrows(CheckOrderCreationException.class, () -> baseRunner.initOrder(scenario));
    }

    @ParameterizedTest
    @CsvSource({"PROCESSING, SUCCESS", "PENDING, FAIL"})
    void waitForOrderCancellation(OrderStatus cancelAfterStatus, CheckOrderScenarioStatus scenarioStatus) {
        when(checkOrderService.getOrderHistoryEvents(any(), eq(ORDER_ID), any()))
                .thenReturn(List.of(cancelledEvent, processingEvent, pendingEvent));
        when(order.getStatus()).thenReturn(OrderStatus.CANCELLED);

        assertEquals(scenarioStatus, baseRunner.waitForOrderCancellation(order, cancelAfterStatus, scenario));
        if (scenarioStatus == CheckOrderScenarioStatus.FAIL) {
            verify(scenario).setErrorType(CheckOrderScenarioErrorType.ORDER_CANCELLED_INCORRECTLY);
            verify(scenario).withErrorDetails(List.of(
                    new ScenarioErrorDetail(ScenarioErrorParam.EXPECTED_ORDER_SUBSTATUS, OrderStatus.PENDING),
                    new ScenarioErrorDetail(ScenarioErrorParam.ACTUAL_ORDER_SUBSTATUS, OrderStatus.PROCESSING)
            ));
        }
    }

    @Test
    void waitForOrderCancellation_still_waiting() {
        when(order.getStatus()).thenReturn(OrderStatus.PROCESSING);
        when(checkOrderService.getOrderHistoryEvents(any(), eq(ORDER_ID), any()))
                .thenReturn(List.of(processingEvent, pendingEvent));

        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS,
                baseRunner.waitForOrderCancellation(order, OrderStatus.PROCESSING, scenario));
    }

    @Test
    void waitForOrderCancellation_too_late_to_cancel() {
        when(order.getStatus()).thenReturn(OrderStatus.DELIVERY);
        when(checkOrderService.getOrderHistoryEvents(any(), eq(ORDER_ID), any()))
                .thenReturn(List.of(processingEvent, pendingEvent));

        assertEquals(CheckOrderScenarioStatus.FAIL,
                baseRunner.waitForOrderCancellation(order, OrderStatus.PROCESSING, scenario));
        verify(scenario).setErrorType(CheckOrderScenarioErrorType.ORDER_NOT_CANCELLED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void retryOnUnknownException(boolean exceededRetryCnt) {
        when(scenario.getFailedAttemptsCnt()).thenReturn(exceededRetryCnt ? Integer.MAX_VALUE : 0);
        baseRunner.handleCheckOrderException(scenario, new IllegalStateException("foo"));
        if (exceededRetryCnt) {
            verify(scenario).setStatus(CheckOrderScenarioStatus.INTERNAL_ERROR);
        } else {
            verify(scenario, never()).setStatus(any());
        }
        verify(scenario).addFailedAttemptTrace(any());
    }

    private BaseCheckOrderScenarioRunner runnerStub(Color color) {
        return new BaseCheckOrderScenarioRunner(checkOrderService, mbiApiService,
                warehouseRegionService, shopDeliveryRegionService, checkOrderOffersProvider, businessService) {
            @Nonnull
            @Override
            protected CheckOrderScenarioStatus checkProgress(CheckOrderScenario scenario, Order order) {
                return CheckOrderScenarioStatus.IN_PROGRESS;
            }

            @Override
            protected boolean reachedTimeout(Order order) {
                return false;
            }

            @Override
            @Nonnull
            protected CreateOrderParam orderParam(long shopId) {
                return basicOrderParam(SHOP_ID, color);
            }

            @Override
            @Nonnull
            protected CheckOrderScenarioStatus handleTimeout(CheckOrderScenario scenario, Order order) {
                return CheckOrderScenarioStatus.FAIL;
            }

            @Override
            @Nonnull
            public CheckOrderScenarioType getType() {
                return CheckOrderScenarioType.SUCCESSFUL_ORDER;
            }
        };
    }

    @Test
    void handleOrderCreationException() {
        var scenarioToProcess = new CheckOrderScenario(CheckOrderScenarioType.SUCCESSFUL_ORDER, OrderProcessMethod.API);
        var subcodes = Set.of(OrderFailure.SubCode.DELIVERY_OPTIONS, OrderFailure.SubCode.PAYMENT);
        runnerStub(Color.BLUE).handleOrderCreationException(scenarioToProcess, new CheckOrderCreationException(
                CheckOrderScenarioErrorType.CHECKOUT_PROBLEMS,
                List.of(new ScenarioErrorDetail(ScenarioErrorParam.API_ERROR_CODES, "value")),
                subcodes
        ));
        assertEquals(subcodes, scenarioToProcess.getPayload().getFailureSubcodes());
    }
}
