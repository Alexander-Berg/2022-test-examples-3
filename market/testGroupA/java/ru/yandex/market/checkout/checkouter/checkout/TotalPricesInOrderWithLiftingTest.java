package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.utils.DeliveryOptionsUtils;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;

public class TotalPricesInOrderWithLiftingTest extends AbstractWebTestBase {

    public static final BigDecimal DELIVERY_PRICE = BigDecimal.valueOf(10);
    /**
     * Это дефолтное значение из
     * OrderItemProvider.applyDefaults
     */
    public static final BigDecimal ITEM_PRICE = BigDecimal.valueOf(250);

    @Autowired
    private OrderCreateHelper orderCreateHelper;
    private Parameters parameters;

    @BeforeEach
    public void init() {
        checkouterProperties.setEnableLiftOptions(true);
        OrderItem orderItem = OrderItemProvider.getOrderItem();
        parameters =
                WhiteParametersProvider.shopDeliveryOrder(OrderProvider.orderBuilder()
                        .item(orderItem)
                        .build()
                );
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setPrice(DELIVERY_PRICE);
        parameters.getOrder().getDelivery().setBuyerPrice(DELIVERY_PRICE);
        parameters.getOrder().getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setLargeSize(true);
    }

    @Test
    public void testCargoLiftingTotalPrice() throws Exception {
        parameters.getOrder().getDelivery().setLiftType(LiftType.CARGO_ELEVATOR);
        BigDecimal liftPrice = DeliveryOptionsUtils.ELEVATOR_LIFT_PRICE;
        // в тотале учитывается и доставка, и подъем
        BigDecimal total = ITEM_PRICE.add(DELIVERY_PRICE).add(liftPrice);

        testTotals(liftPrice, total);
    }

    @ParameterizedTest(name = "Проверка тоталов при ручном подъеме на {0} этаж.")
    @CsvSource({"3", "1", "25", "103"})
    public void testManualLiftingTotalPrice(int floor) throws Exception {
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        parameters.getOrder().getDelivery().setBuyerAddress(
                AddressProvider.getAddress(address -> address.setFloor(String.valueOf(floor))
                ));
        BigDecimal liftPrice = DeliveryOptionsUtils.MANUAL_LIFT_PRICE.multiply(BigDecimal.valueOf(floor));
        // в тотале учитывается и доставка, и подъем
        BigDecimal total = ITEM_PRICE.add(DELIVERY_PRICE).add(liftPrice);

        testTotals(liftPrice, total);
    }

    private void testTotals(BigDecimal liftPrice, BigDecimal total) {
        Order order = orderCreateHelper.createOrder(parameters);
        // стоимость доставки такая же
        assertEquals(liftPrice.compareTo(order.getDelivery().getLiftPrice()), 0,
                liftPrice + " vs " + order.getDelivery().getLiftPrice());
        assertEquals(DELIVERY_PRICE.compareTo(order.getDelivery().getBuyerPrice()), 0,
                DELIVERY_PRICE + " vs " + order.getDelivery().getBuyerPrice());
        assertEquals(ITEM_PRICE.compareTo(order.getBuyerItemsTotal()), 0,
                ITEM_PRICE + " vs " + order.getBuyerItemsTotal());
        assertEquals(total.compareTo(order.getBuyerTotal()), 0,
                total + " vs " + order.getBuyerTotal());
    }

    private void enableLiftOptions(Parameters parameters) {
        checkouterProperties.setEnableLiftOptions(true);
        parameters.addExperiment(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE);
    }
}
