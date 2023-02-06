package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderControllerTestHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDeliveryFiltersTestBase extends AbstractWebTestBase {

    protected Order orderWithShipment2;
    protected Order orderWithShipment1;
    protected Order orderWithoutShipment;

    @Autowired
    private OrderControllerTestHelper orderControllerTestHelper;

    @BeforeAll
    public void init() {
        setupFeatureDefaults();
        orderWithShipment1 = orderControllerTestHelper.createOrderWithPartnerDelivery(OrderProvider.SHOP_ID);
        orderWithShipment2 = orderControllerTestHelper.createOrderWithPartnerDelivery(OrderProvider.SHOP_ID);
        orderWithoutShipment = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
    }

    @AfterEach
    @Override
    public void tearDownBase() {
        setupFeatureDefaults();
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }
}
