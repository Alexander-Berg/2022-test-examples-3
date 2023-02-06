package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.DeliveryRoute;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;

public class CheckoutLiftPriceTest extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;

    @Test
    public void testLiftPriceCalculationIsDisabled() throws Exception {
        disableLiftOptions();

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.ELEVATOR);
        parameters.getOrder().getDelivery().setLiftPrice(BigDecimal.valueOf(50L));

        MultiOrder newOrder = checkoutOrder(parameters);
        Order order = newOrder.getCarts().get(0);
        assertNull(order.getDelivery().getLiftType());
        assertNull(order.getDelivery().getLiftPrice());
    }

    @Test
    public void testElevatorLiftType() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertEquals(BigDecimal.valueOf(150L), order.getDelivery().getLiftPrice());
        assertEquals(LiftType.ELEVATOR, order.getDelivery().getLiftType());
    }

    @Test
    public void testDsbsFreeLiftType() throws Exception {
        ShopMetaData newMeta =
                ShopMetaDataBuilder.createTestDefault().withFreeLiftingEnabled(true).build();

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addShopMetaData(parameters.getShopId(), newMeta);
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertEquals(BigDecimal.valueOf(0L), order.getDelivery().getLiftPrice());
        assertEquals(LiftType.FREE, order.getDelivery().getLiftType());
    }

    @Test
    public void testCargoElevatorLiftType() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.CARGO_ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertEquals(BigDecimal.valueOf(150L), order.getDelivery().getLiftPrice());
        assertEquals(LiftType.CARGO_ELEVATOR, order.getDelivery().getLiftType());
    }

    @Test
    public void testManualLiftTypeWithoutFloor() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        Address testAddress = AddressProvider.getAddress(address -> address.setFloor(null));
        parameters.getOrder().getDelivery().setBuyerAddress(testAddress);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertNull(order.getDelivery().getLiftPrice());
        assertEquals(LiftType.NOT_NEEDED, order.getDelivery().getLiftType());
    }

    @Test
    public void testManualTypeWithIncorrectFloor() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        Address testAddress = AddressProvider.getAddress(address -> address.setFloor("floor"));
        parameters.getOrder().getDelivery().setBuyerAddress(testAddress);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertNull(order.getDelivery().getLiftPrice());
        assertEquals(LiftType.NOT_NEEDED, order.getDelivery().getLiftType());
    }

    @Test
    public void testManualTypeWithValidPositiveFloor() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        Address testAddress = AddressProvider.getAddress(address -> address.setFloor("2"));
        parameters.getOrder().getDelivery().setBuyerAddress(testAddress);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertEquals(BigDecimal.valueOf(300L), order.getDelivery().getLiftPrice());
        assertEquals(LiftType.MANUAL, order.getDelivery().getLiftType());
    }

    @Test
    public void testManualTypeWithValidNegativeFloor() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        Address testAddress = AddressProvider.getAddress(address -> address.setFloor("-2"));
        parameters.getOrder().getDelivery().setBuyerAddress(testAddress);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertEquals(BigDecimal.valueOf(300L), order.getDelivery().getLiftPrice());
        assertEquals(LiftType.MANUAL, order.getDelivery().getLiftType());
    }

    @Test
    public void testManualTypeWithNonIntegerFloor() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        Address testAddress = AddressProvider.getAddress(address -> address.setFloor("2.5"));
        parameters.getOrder().getDelivery().setBuyerAddress(testAddress);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertNull(order.getDelivery().getLiftPrice());
        assertEquals(LiftType.NOT_NEEDED, order.getDelivery().getLiftType());
    }

    @Test
    public void testLiftTypeNotNeeded() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.NOT_NEEDED);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertNull(order.getDelivery().getLiftPrice());
        assertEquals(LiftType.NOT_NEEDED, order.getDelivery().getLiftType());
    }

    @Test
    public void testLiftTypeNull() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(null);

        MultiOrder newOrder = checkoutOrder(parameters);

        Order order = newOrder.getCarts().get(0);
        assertNull(order.getDelivery().getLiftPrice());
        assertEquals(LiftType.NOT_NEEDED, order.getDelivery().getLiftType());
    }

    @Test
    public void testLiftTypeUnknown() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.UNKNOWN);

        MultiOrder newOrder = checkoutOrder(parameters);
        Order order = newOrder.getCarts().get(0);
        assertNull(order.getDelivery().getLiftPrice());
        assertEquals(LiftType.NOT_NEEDED, order.getDelivery().getLiftType());
    }

    @Test
    public void testLiftTypeForBlueParameters() throws Exception {
        checkouterProperties.setNewLargeSizeCalculation(true);
        // Добиваемся похода через комбинатор и необходимость использования actual_delivery, только delivery_route
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist<>(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        enableLiftOptions(parameters);

        ActualDelivery actualDelivery = parameters.getReportParameters().getActualDelivery();
        // указываем КГТ в параметрах
        actualDelivery.getResults().get(0).setLargeSize(true);
        DeliveryRoute deliveryRoute = DeliveryRouteProvider.fromActualDelivery(actualDelivery, DeliveryType.DELIVERY);

        parameters.getReportParameters().setDeliveryRoute(deliveryRoute);
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);

        MultiOrder orders = checkoutOrder(parameters);

        // проверяем, что поход через delivery_route в доставке для Y_MARKET проставляем FREE и цену 0
        Delivery delivery = orders.getOrders().get(0).getDelivery();
        assertEquals(DeliveryPartnerType.YANDEX_MARKET, delivery.getDeliveryPartnerType());
        assertEquals(LiftType.FREE, delivery.getLiftType());
        assertEquals(0, BigDecimal.ZERO.compareTo(delivery.getLiftPrice()));
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

    private void disableLiftOptions() {
        checkouterProperties.setEnableLiftOptions(false);
    }
}
