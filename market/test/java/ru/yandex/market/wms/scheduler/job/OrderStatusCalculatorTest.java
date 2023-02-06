package ru.yandex.market.wms.scheduler.job;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.h2.util.LazyFuture;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.order.status.calculate.dao.OrderToProcessDaoV2;
import ru.yandex.market.wms.scheduler.order.status.calculate.model.CalculationConfig;
import ru.yandex.market.wms.scheduler.order.status.calculate.model.OrderToProcess;
import ru.yandex.market.wms.scheduler.order.status.calculate.model.SkuWithBuilding;
import ru.yandex.market.wms.scheduler.order.status.calculate.model.SkuWithQty;
import ru.yandex.market.wms.scheduler.order.status.calculate.service.OrderStatusCalculator;
import ru.yandex.market.wms.scheduler.order.status.common.dao.SchedulerOrderStatusDao;
import ru.yandex.market.wms.scheduler.order.status.common.model.SkuId;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;


public class OrderStatusCalculatorTest extends SchedulerIntegrationTest {


    @MockBean
    @Autowired
    private OrderToProcessDaoV2 orderToProcessDaoV2;

    @Autowired
    private OrderStatusCalculator orderStatusCalculator;

    @MockBean
    @Autowired
    private SchedulerOrderStatusDao schedulerOrderStatusDao;

    @Test
    public void oneGoodItemAndOneCancelledOnStockForOrder() {

        Future<List<OrderToProcess>> orders = new LazyFuture<>() {
            @Override
            protected List<OrderToProcess> run() {
                List<OrderToProcess> ret = new ArrayList<>();
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU00123"), "ORD01", "-1", 1.0,
                        false, false, 1, "0", null, "01"));
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU001"), "ORD02", "-5", 0.0,
                        false, false, 1, "0", null, "01"));
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU00123"), "ORD02", "-1", 1.0,
                        false, false, 1, "0", null, "02"));
                return ret;
            }
        };

        Future<List<SkuWithQty>> skus = new LazyFuture<>() {
            @Override
            protected List<SkuWithQty> run() {
                List<SkuWithQty> ret = new ArrayList<>();
                ret.add(new SkuWithQty(1, SkuId.of("STORER01", "SKU00123"), 0.0, 10.0));
                ret.add(new SkuWithQty(1, SkuId.of("STORER01", "SKU001"), 0.0, 0.0));
                return ret;
            }
        };

        Mockito.when(orderToProcessDaoV2.getAll(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithBuilding(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithoutBuilding(anyInt())).thenReturn(orders);
        Mockito.when(skuWithQtyDao.get(anyInt(), anyBoolean())).thenReturn(skus);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);

        var calculationConfig = Mockito.mock(CalculationConfig.class);
        Mockito.when(calculationConfig.getPeriodForOrdersToPull()).thenReturn(100);
        Mockito.when(calculationConfig.getUseFairItemDistribution()).thenReturn(true);
        var calculationModel = orderStatusCalculator.calculate(calculationConfig);

        assertions.assertThat(calculationModel.getOrdersNotEnoughBalance().size()).isZero();
        assertions.assertThat(calculationModel.getOrdersToEnoughBalance().size()).isEqualTo(2);
    }

    @Test
    public void oneGoodItemOnStockForOrder() {

        Future<List<OrderToProcess>> orders = new LazyFuture<>() {
            @Override
            protected List<OrderToProcess> run() {
                List<OrderToProcess> ret = new ArrayList<>();
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU00123"), "ORD01", "-1", 1.0,
                        false, false, 1, "0", null, "01"));
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU001"), "ORD02", "-1", 1.0,
                        false, false, 1, "0", null, "01"));
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU00123"), "ORD02", "-1", 1.0,
                        false, false, 1, "0", null, "02"));
                return ret;
            }
        };

        Future<List<SkuWithQty>> skus = new LazyFuture<>() {
            @Override
            protected List<SkuWithQty> run() {
                List<SkuWithQty> ret = new ArrayList<>();
                ret.add(new SkuWithQty(1, SkuId.of("STORER01", "SKU00123"), 0.0, 10.0));
                ret.add(new SkuWithQty(1, SkuId.of("STORER01", "SKU001"), 0.0, 0.0));
                return ret;
            }
        };

        Mockito.when(orderToProcessDaoV2.getAll(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithBuilding(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithoutBuilding(anyInt())).thenReturn(orders);
        Mockito.when(skuWithQtyDao.get(anyInt(), anyBoolean())).thenReturn(skus);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);

        var calculationConfig = Mockito.mock(CalculationConfig.class);
        Mockito.when(calculationConfig.getPeriodForOrdersToPull()).thenReturn(100);
        Mockito.when(calculationConfig.getUseFairItemDistribution()).thenReturn(true);
        var calculationModel = orderStatusCalculator.calculate(calculationConfig);

        assertions.assertThat(calculationModel.getOrdersNotEnoughBalance().size()).isOne();
        assertions.assertThat(calculationModel.getOrdersToEnoughBalance().size()).isOne();
    }

    @Test
    public void noGoodOnStockForOrder() {

        Future<List<OrderToProcess>> orders = new LazyFuture<>() {
            @Override
            protected List<OrderToProcess> run() {
                List<OrderToProcess> ret = new ArrayList<>();
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU001"), "ORD02", "-1", 0.0,
                        false, false, 1, "0", null, "01"));
                return ret;
            }
        };

        Future<List<SkuWithQty>> skus = new LazyFuture<>() {
            @Override
            protected List<SkuWithQty> run() {
                List<SkuWithQty> ret = new ArrayList<>();
                ret.add(new SkuWithQty(1, SkuId.of("STORER01", "SKU00123"), 0.0, 1.0));
                ret.add(new SkuWithQty(1, SkuId.of("STORER01", "SKU001"), 0.0, 10.0));
                return ret;
            }
        };

        Mockito.when(orderToProcessDaoV2.getAll(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithBuilding(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithoutBuilding(anyInt())).thenReturn(orders);
        Mockito.when(skuWithQtyDao.get(anyInt(), anyBoolean())).thenReturn(skus);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);

        var calculationConfig = Mockito.mock(CalculationConfig.class);
        Mockito.when(calculationConfig.getPeriodForOrdersToPull()).thenReturn(100);
        Mockito.when(calculationConfig.getUseFairItemDistribution()).thenReturn(true);
        var calculationModel = orderStatusCalculator.calculate(calculationConfig);

        assertions.assertThat(calculationModel.getOrdersNotEnoughBalance().size()).isZero();
        assertions.assertThat(calculationModel.getOrdersToEnoughBalance().size()).isZero();
    }

    @Test
    public void oneGoodOnStockForTwoOrders() {

        Future<List<OrderToProcess>> orders = new LazyFuture<>() {
            @Override
            protected List<OrderToProcess> run() {
                List<OrderToProcess> ret = new ArrayList<>();
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU00123"), "ORD01", "-1", 1.0,
                        false, false, 1, "0", null, "01"));
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU001"), "ORD02", "-1", 0.0,
                        false, false, 1, "0", null, "01"));
                return ret;
            }
        };

        Future<List<SkuWithQty>> skus = new LazyFuture<>() {
            @Override
            protected List<SkuWithQty> run() {
                List<SkuWithQty> ret = new ArrayList<>();
                ret.add(new SkuWithQty(1, SkuId.of("STORER01", "SKU00123"), 0.0, 1.0));
                ret.add(new SkuWithQty(1, SkuId.of("STORER01", "SKU001"), 0.0, 0.0));
                return ret;
            }
        };

        Mockito.when(orderToProcessDaoV2.getAll(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithBuilding(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithoutBuilding(anyInt())).thenReturn(orders);
        Mockito.when(skuWithQtyDao.get(anyInt(), anyBoolean())).thenReturn(skus);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);

        var calculationConfig = Mockito.mock(CalculationConfig.class);
        Mockito.when(calculationConfig.getPeriodForOrdersToPull()).thenReturn(100);
        Mockito.when(calculationConfig.getUseFairItemDistribution()).thenReturn(true);
        var calculationModel = orderStatusCalculator.calculate(calculationConfig);

        assertions.assertThat(calculationModel.getOrdersNotEnoughBalance().size()).isZero();
        assertions.assertThat(calculationModel.getOrdersToEnoughBalance().size()).isOne();
    }

    @Test
    public void noGoodOnStockForTwoOrders() {

        Future<List<OrderToProcess>> orders = new LazyFuture<>() {
            @Override
            protected List<OrderToProcess> run() {
                List<OrderToProcess> ret = new ArrayList<>();
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU001"), "ORD01", "-1", 1.0,
                        false, false, 1, "0", null, "01"));
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU001"), "ORD02", "-1", 0.0,
                        false, false, 1, "0", null, "01"));
                return ret;
            }
        };

        Future<List<SkuWithQty>> skus = new LazyFuture<>() {
            @Override
            protected List<SkuWithQty> run() {
                List<SkuWithQty> ret = new ArrayList<>();
                ret.add(new SkuWithQty(null, SkuId.of("STORER01", "SKU00123"), 0.0, 0.0));
                return ret;
            }
        };

        Mockito.when(orderToProcessDaoV2.getAll(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithBuilding(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithoutBuilding(anyInt())).thenReturn(orders);
        Mockito.when(skuWithQtyDao.get(anyInt(), anyBoolean())).thenReturn(skus);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);

        var calculationConfig = Mockito.mock(CalculationConfig.class);
        Mockito.when(calculationConfig.getPeriodForOrdersToPull()).thenReturn(100);
        Mockito.when(calculationConfig.getUseFairItemDistribution()).thenReturn(true);
        var calculationModel = orderStatusCalculator.calculate(calculationConfig);

        assertions.assertThat(calculationModel.getOrdersNotEnoughBalance().size()).isOne();
        assertions.assertThat(calculationModel.getOrdersToEnoughBalance().size()).isZero();
    }

    @Test
    public void ordersSort() {
        Future<List<OrderToProcess>> orders = new LazyFuture<>() {
            @Override
            protected List<OrderToProcess> run() {
                List<OrderToProcess> ret = new ArrayList<>();
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU00123"), "ORD01", "-3", 1.0, false, false,
                        null, "0", Instant.now().plus(1, ChronoUnit.HOURS), "01"));
                ret.add(new OrderToProcess(SkuId.of("STORER01", "SKU00123"), "ORD02", "-3", 1.0, false, false,
                        null, "0", Instant.now(), "01"));
                return ret;
            }
        };

        Future<List<SkuWithQty>> skus = new LazyFuture<>() {
            @Override
            protected List<SkuWithQty> run() {
                List<SkuWithQty> ret = new ArrayList<>();
                ret.add(new SkuWithQty(null, SkuId.of("STORER01", "SKU00123"), 0.0, 1.0));
                return ret;
            }
        };

        Future<Map<SkuWithBuilding, Double>> skusToReplenish = new LazyFuture<>() {
            @Override
            protected Map<SkuWithBuilding, Double> run() {
                return Map.of();
            }
        };

        Mockito.when(orderToProcessDaoV2.getAll(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithBuilding(anyInt())).thenReturn(orders);
        Mockito.when(orderToProcessDaoV2.getWithoutBuilding(anyInt())).thenReturn(orders);
        Mockito.when(skuWithQtyDao.get(anyInt(), anyBoolean())).thenReturn(skus);
        Mockito.when(schedulerOrderStatusDao.setOrdersStatusesOptimized(anyString(), anySet())).thenReturn(1);

        var calculationConfig = Mockito.mock(CalculationConfig.class);
        Mockito.when(calculationConfig.getPeriodForOrdersToPull()).thenReturn(100);
        Mockito.when(calculationConfig.getUseFairItemDistribution()).thenReturn(true);
        var calculationModel = orderStatusCalculator.calculate(calculationConfig);

        assertions.assertThat(calculationModel.getOrdersToEnoughBalance().size()).isOne();
        assertions.assertThat(calculationModel.getOrdersToEnoughBalance().stream().findFirst().get().getOrderKey())
                .isEqualTo("ORD02");
    }
}
