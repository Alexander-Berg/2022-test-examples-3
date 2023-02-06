package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties.TariffsAndLiftExperimentToggle;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;

public class GetOrderWithLiftOptionsTest extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    @Test
    public void testGetOrderWithDeliveryOptions() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = orderGetHelper.getOrder(newOrder.getOrders().get(0).getId(), ClientInfo.SYSTEM);

        assertEquals(BigDecimal.valueOf(150L), order.getDelivery().getLiftPrice());
        assertEquals(LiftType.ELEVATOR, order.getDelivery().getLiftType());
    }

    @Test
    public void testGetOrderWithoutDeliveryOptions() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(false);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = orderGetHelper.getOrder(newOrder.getOrders().get(0).getId(), ClientInfo.SYSTEM);

        assertNull(order.getDelivery().getLiftPrice());
        assertNull(order.getDelivery().getLiftType());
    }

    @Test
    public void testGetOrderWithoutDeliveryOptionsWhenExperimentAreForced() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        checkouterProperties.setEnableLiftOptions(true);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.TARIFFS_AND_LIFT_EXPERIMENT_TOGGLE,
                TariffsAndLiftExperimentToggle.FORCE);
        parameters.getReportParameters().setLargeSize(false);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = orderGetHelper.getOrder(newOrder.getOrders().get(0).getId(), ClientInfo.SYSTEM);

        assertNull(order.getDelivery().getLiftPrice());
        assertNull(order.getDelivery().getLiftType());
    }

    @Test
    public void testUnloadPrice() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        // ручной
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        // 120 кг - 2 раза по 100 -> 150 ; 2 = 300
        parameters.getReportParameters().getOffers().get(0).setWeight(BigDecimal.valueOf(120));
        // 24 этаж
        ((AddressImpl) parameters.getOrder().getDelivery().getBuyerAddress()).setFloor("24");
        // Выбираем разгрузку
        parameters.getOrder().getDelivery().setUnloadEnabled(true);
        checkouterProperties.setNewLargeSizeCalculation(true);
        // чтобы сделать заказа КГТ
        parameters.getReportParameters().getActualDelivery().getResults().get(0).setLargeSize(true);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        Delivery delivery = multiCart.getCarts().get(0).getDeliveryOptions().get(0);

        assertEquals(BigDecimal.valueOf(300), delivery.getLiftPrice());
        assertEquals(LiftType.MANUAL, delivery.getLiftType());

        multiCart.getCarts().get(0).setDelivery(delivery);
        delivery.setLiftType(parameters.getOrder().getDelivery().getLiftType());
        delivery.setUnloadEnabled(true);

        MultiOrder newOrder = orderCreateHelper.checkout(multiCart, parameters);

        Order order = orderGetHelper.getOrder(newOrder.getOrders().get(0).getId(), ClientInfo.SYSTEM);

        assertEquals(BigDecimal.valueOf(300), order.getDelivery().getLiftPrice());
        assertEquals(LiftType.MANUAL, order.getDelivery().getLiftType());
    }

    @Test
    public void testGetOrderWithoutDeliveryOptionsWhenExperimentAreDisabled() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.TARIFFS_AND_LIFT_EXPERIMENT_TOGGLE,
                TariffsAndLiftExperimentToggle.OFF);
        parameters.getReportParameters().setLargeSize(true);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = orderGetHelper.getOrder(newOrder.getOrders().get(0).getId(), ClientInfo.SYSTEM);

        assertNull(order.getDelivery().getLiftPrice());
        assertNull(order.getDelivery().getLiftType());
    }

    private MultiOrder checkoutOrder(Parameters parameters) throws Exception {
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        multiCart.getCarts().get(0).setDelivery(
                multiCart.getCarts().get(0).getDeliveryOptions().get(0)
        );
        multiCart.getCarts().get(0).getDelivery().setLiftType(parameters.getOrder().getDelivery().getLiftType());

        return orderCreateHelper.checkout(multiCart, parameters);
    }

    private void enableLiftOptions(Parameters parameters) {
        checkouterProperties.setEnableLiftOptions(true);
        parameters.setExperiments(MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE);
    }
}
