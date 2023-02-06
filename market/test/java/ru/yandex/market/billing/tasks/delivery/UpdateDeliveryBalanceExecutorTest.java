package ru.yandex.market.billing.tasks.delivery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.IntakeTariffDao;
import ru.yandex.market.core.delivery.service.billing.DeliveryBalanceOrderService;
import ru.yandex.market.core.delivery.service.billing.DeliveryBillingService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.shipment.dao.ShipmentDao;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static ru.yandex.market.billing.tasks.delivery.UpdateDeliveryBalanceExecutor.TO_BILL_DELIVERY;
import static ru.yandex.market.billing.tasks.delivery.UpdateShipmentsIntakeCostExecutor.DATE_TO_ENV_NAME;
import static ru.yandex.market.billing.tasks.delivery.UpdateShipmentsIntakeCostExecutor.DAYS_AGO_ENV_NAME;

@DbUnitDataSet(before = "../../db/CommonDeliveryData.csv")
class UpdateDeliveryBalanceExecutorTest extends FunctionalTest {

    UpdateShipmentsIntakeCostExecutor updateShipmentsIntakeCostExecutor;
    UpdateDeliveryBalanceExecutor updateDeliveryBalanceExecutor;

    @Autowired
    private IntakeTariffDao intakeTariffDao;

    @Autowired
    private ShipmentDao shipmentDao;

    @Autowired
    private DeliveryBalanceOrderService deliveryBalanceOrderService;

    @Autowired
    private DeliveryBillingService deliveryBillingService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void init() {
        environmentService.setValue(DAYS_AGO_ENV_NAME, "1");
        environmentService.setValue(DATE_TO_ENV_NAME, "2017-10-13");
        environmentService.setValue(TO_BILL_DELIVERY, "1");
        updateShipmentsIntakeCostExecutor =
                new UpdateShipmentsIntakeCostExecutor(intakeTariffDao, shipmentDao, environmentService);

        updateDeliveryBalanceExecutor = new UpdateDeliveryBalanceExecutor(
                deliveryBalanceOrderService, deliveryBillingService, environmentService, orderService, shipmentDao);
    }

    /**
     * Тестируем биллинг Маркет.Доставки.
     * Первый шаг - проставляем стоимости заборов в отгрузки (MARKET_BILLING.SHIPMENTS)
     * на основании MARKET_BILLING.INTAKE_TARIFFS
     * 1. Не попадает по фильтру дат
     * 2. Берём 150 - тариф для конкретного магазина с высоким приоритетом
     * 3. 130 - первый диапазон для СД 107
     * 4. Не попадает по статусу
     * 5. 1500 - второй диапазон для СД 107
     * 6. Не попадает по диапазонам для СД 106
     * ---
     * Второй шаг - расчёт баланса (MARKET_BILLING.DELIVERY_ORDER_BALANCE)
     */
    @Test
    @DbUnitDataSet(
            before = "UpdateDeliveryBalanceExecutorTest.before.csv",
            after = "UpdateDeliveryBalanceExecutorTest.after.csv"
    )
    void test() {
        updateShipmentsIntakeCostExecutor.doJob(null);
        updateDeliveryBalanceExecutor.doJobLockedBeforeChaining(null);
    }
}
