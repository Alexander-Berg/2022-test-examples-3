package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.lifting.service.LiftingService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.lifting.LiftingWeightOutOfRangeException;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;

public class DynamicTotalPriceInOrderWithLiftingTest extends AbstractWebTestBase {

    @Autowired
    private LiftingService liftingService;

    @Value("${market.checkouter.lifting.minWeightForUnload}")
    private Long minWeightForUnload;

    private Parameters parameters;

    @BeforeEach
    public void init() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        enableLiftOptions(parameters);
        checkouterProperties.setNewLargeSizeCalculation(true);
        checkouterProperties.setNewLargeSizeWithVolumeCalculation(true);
        parameters.getReportParameters().setLargeSize(true);
    }

    @Test
    public void borderTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().iterator().next().setWeight(100 * 1000L); // 100 кг - пограничное
        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.valueOf(33).multiply(BigDecimal.ONE), liftingPrice);
        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.ONE), manualPrice);
    }

    @Test
    public void borderPlusOneTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().iterator().next().setWeight(101 * 1000L); // 101 кг - пограничное
        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.valueOf(33).multiply(BigDecimal.valueOf(2)), liftingPrice);
        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.valueOf(2)), manualPrice);
    }


    @Test
    public void borderMinusOneTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().iterator().next().setWeight(99 * 1000L); // 99 кг - пограничное
        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.ONE), liftingPrice);
        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.ONE), manualPrice);
    }

    @Test
    public void borderMaxWeightTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().iterator().next().setWeight(Integer.MAX_VALUE * 1000L); // Integer.MAX_VALUE т - пограничное
        assertThrows(LiftingWeightOutOfRangeException.class,
                () -> liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order));
        assertThrows(LiftingWeightOutOfRangeException.class,
                () -> liftingService.calculateLiftingPrice(LiftType.MANUAL, order));
    }

    @Test
    public void borderMaxAvailableWeightTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().iterator().next().setWeight(9_999 * 1000L + 999); // 9 т 999 кг 999 г - пограничное
        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);


        assertEquals(BigDecimal.valueOf(40).multiply(BigDecimal.valueOf(100)), liftingPrice);

        assertEquals(BigDecimal.valueOf(80).multiply(BigDecimal.valueOf(100)), manualPrice);
    }

    @Test
    public void nominalWeightMoreRealWeightTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        OrderItem orderItem = order.getItems().iterator().next();
        orderItem.setWeight(499L * 1000); // 499 кг (до следующего предела)
        // w*h*d * 200 / 10^6 - это в кг и * 10^3, чтобы получить в граммах
        long width = 25L;
        long height = 100L;
        long depth = 1000L;

        orderItem.setWidth(width);
        orderItem.setHeight(height);
        orderItem.setDepth(depth);

        // const = 2 * 10^2*10^3 / 10^6 = 2 / 10 = 0.2
        // const * 500 * 1000 = 25 * 10^5
        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.valueOf(160).multiply(BigDecimal.valueOf(5)), liftingPrice);
        assertEquals(BigDecimal.valueOf(160).multiply(BigDecimal.valueOf(5)), manualPrice);
    }

    @Test
    public void zeroWeightTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        OrderItem orderItem = order.getItems().iterator().next();
        orderItem.setWeight(0L); // 0 кг

        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.ONE.multiply(BigDecimal.ONE), liftingPrice);
        assertEquals(BigDecimal.ONE.multiply(BigDecimal.ONE), manualPrice);
    }


    @Test
    public void countGoodWeightTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().iterator().next().setWeight(10 * 1000L); // 10 кг - пограничное
        order.getItems().iterator().next().setCount(10); // 10 штук
        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.valueOf(33).multiply(BigDecimal.ONE), liftingPrice);
        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.ONE), manualPrice);
    }

    @Test
    public void freeUnloadPriceTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().iterator().next().setWeight(minWeightForUnload * 1000L - 1); // minWeightForUnload кг - 1 грамм

        BigDecimal unloadPrice = liftingService.calculateUnloadPrice(order);

        assertNull(unloadPrice);
    }

    @Test
    public void notFreeUnloadPriceTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        order.getItems().iterator().next().setWeight(minWeightForUnload * 1000L); // minWeightForUnload кг

        BigDecimal unloadPrice = liftingService.calculateUnloadPrice(order);

        assertEquals(BigDecimal.valueOf(150), unloadPrice);
    }

    @Test
    public void testIgnorableRangeTest() {
        Order order = orderCreateHelper.createOrder(parameters);
        OrderItem orderItem = order.getItems().iterator().next();
        orderItem.setWeight(99L * 1000); // 99 кг (до следующего предела)
        // w*h*d * 200 / 10^6 - это в кг и * 10^3, чтобы получить в граммах
        long width = 9L;
        long height = 100L;
        long depth = 1000L;

        // сравниваем 30 кг и 99 кг
        orderItem.setWidth(width);
        orderItem.setHeight(height);
        orderItem.setDepth(depth);

        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.ONE), liftingPrice);
        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.ONE), manualPrice);
    }

    @Test
    public void testBorderWeight() {
        Order order = orderCreateHelper.createOrder(parameters);
        OrderItem orderItem = order.getItems().iterator().next();
        orderItem.setWeight(25L * 1000); // 25 кг (не КГТ)
        // w*h*d / 1_000_000 должно быть от 0.5 до 1 м^3
        long width = 9L;
        long height = 100L;
        long depth = 1000L;

        // эквивалентно 0.9 м^3 = 30 кг, а реального 25 кг
        orderItem.setWidth(width);
        orderItem.setHeight(height);
        orderItem.setDepth(depth);

        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.ONE), liftingPrice);
        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.ONE), manualPrice);
    }

    @Test
    public void testLowWeightWithIgnoreVolume() {
        Order order = orderCreateHelper.createOrder(parameters);
        OrderItem orderItem = order.getItems().iterator().next();
        orderItem.setWeight(150L * 1000); // 150 кг (от 100 до 200 нет объема)
        // w*h*d / 1_000_000 должно быть от 0.5 до 1 м^3
        long width = 9L;
        long height = 100L;
        long depth = 1000L;

        //  0.9 м^3 это промежуток 30 - 100 (а это трансформируется в нижнюю границу веса в 30)
        orderItem.setWidth(width);
        orderItem.setHeight(height);
        orderItem.setDepth(depth);

        BigDecimal liftingPrice = liftingService.calculateLiftingPrice(LiftType.ELEVATOR, order);
        BigDecimal manualPrice = liftingService.calculateLiftingPrice(LiftType.MANUAL, order);

        assertEquals(BigDecimal.valueOf(33).multiply(BigDecimal.valueOf(2)), liftingPrice);
        assertEquals(BigDecimal.valueOf(150).multiply(BigDecimal.valueOf(2)), manualPrice);
    }

    private void enableLiftOptions(Parameters parameters) {
        checkouterProperties.setEnableLiftOptions(true);
        parameters.setExperiments(MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE);
    }

}
