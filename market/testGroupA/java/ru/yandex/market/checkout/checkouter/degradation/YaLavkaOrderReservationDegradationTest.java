package ru.yandex.market.checkout.checkouter.degradation;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryServiceServiceImpl;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order.YaLavkaReservationQCProcessor;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.yalavka.YaLavkaDeliveryServiceConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

@EnableAspectJAutoProxy
public class YaLavkaOrderReservationDegradationTest extends AbstractWebTestBase {

    @Autowired
    private YaLavkaReservationQCProcessor processor;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private YaLavkaDeliveryServiceConfigurer serviceConfigurer;
    @Autowired
    private YaLavkaDeliveryServiceServiceImpl service;

    @BeforeEach
    public void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    @Test
    void onNot5xxErrorTest() {
        checkouterProperties.setEnableLavkaOrderReservationDegradationStrategy(true);
        checkouterProperties.setAntifraudSubstatusEnabled(true);

        serviceConfigurer.configureOrderReservationRequest(HttpStatus.NOT_FOUND);

        Parameters parameters = defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        assertThrows(ErrorCodeException.class, () -> service.reserveOrder(order));
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.CREATE_YA_LAVKA_ORDER, order.getId()));
    }

    @Test
    void on5xxErrorTest() {
        checkouterProperties.setEnableLavkaOrderReservationDegradationStrategy(true);
        checkouterProperties.setAntifraudSubstatusEnabled(true);

        serviceConfigurer.configureOrderReservationRequest(HttpStatus.INTERNAL_SERVER_ERROR);

        Parameters parameters = defaultBlueOrderParameters();
        parameters.setExperiments(Experiments.of(
                Experiments.CHECKOUT_LAVKA_DEGRADATION_EXP,
                Experiments.CHECKOUT_LAVKA_DEGRADATION_EXP_VALUE
        ));

        Order order = parameters.getOrder();

        // Указываем, что нужно проверка антифрода
        order.setProperty("asyncOutletAntifraud", "true");

        order = orderCreateHelper.createOrder(parameters);

        service.reserveOrder(order);

        // Проверяем, что отработала стратегия деградации и создался QC
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.CREATE_YA_LAVKA_ORDER, order.getId()));

        payHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        // Проверяем, что после оплаты заказ улетел в нужный статус+подстатус
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_LAVKA_RESERVATION, order.getSubstatus());

        //  Процессим заказ в лавке и проверяем, что заказ теперь в статусе проверки фрода
        processor.process(new QueuedCallProcessor.QueuedCallExecution(order.getId(), null,
                0, Instant.now(), order.getId()));

        order = orderService.getOrder(order.getId());

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(OrderSubstatus.ANTIFRAUD, order.getSubstatus());
    }

    @Test
    void experimentOnErrorTest() {
        checkouterProperties.setEnableLavkaOrderReservationDegradationStrategy(true);
        checkouterProperties.setAntifraudSubstatusEnabled(true);
        checkouterProperties.setApplyLavkaDegradationStraightOnExperiment(true);

        Parameters parameters = defaultBlueOrderParameters();
        parameters.setExperiments(Experiments.of(
                Experiments.CHECKOUT_LAVKA_DEGRADATION_EXP,
                Experiments.CHECKOUT_LAVKA_DEGRADATION_EXP_VALUE
        ));
        Order order = parameters.getOrder();

        // Указываем, что нужно проверка антифрода
        order.setProperty("asyncOutletAntifraud", "true");

        order = orderCreateHelper.createOrder(parameters);

        service.reserveOrder(order);

        serviceConfigurer.assertNoInteractions();

        // Проверяем, что отработала стратегия деградации и создался QC
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.CREATE_YA_LAVKA_ORDER, order.getId()));

        payHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        // Проверяем, что после оплаты заказ улетел в нужный статус+подстатус
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(OrderSubstatus.AWAIT_LAVKA_RESERVATION, order.getSubstatus());

        //  Процессим заказ в лавке и проверяем, что заказ теперь в статусе проверки фрода
        processor.process(new QueuedCallProcessor.QueuedCallExecution(order.getId(), null,
                0, Instant.now(), order.getId()));

        order = orderService.getOrder(order.getId());

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(OrderSubstatus.ANTIFRAUD, order.getSubstatus());
    }

    @Test
    void doNotDegradeIfPropertyFalseTest() {
        checkouterProperties.setEnableLavkaOrderReservationDegradationStrategy(true);
        checkouterProperties.setAntifraudSubstatusEnabled(true);
        checkouterProperties.setApplyLavkaDegradationStraightOnExperiment(false);

        serviceConfigurer.configureOrderReservationRequest(HttpStatus.OK);

        Parameters parameters = defaultBlueOrderParameters();
        parameters.setExperiments(Experiments.of(
                Experiments.CHECKOUT_LAVKA_DEGRADATION_EXP,
                Experiments.CHECKOUT_LAVKA_DEGRADATION_EXP_VALUE
        ));

        Order order = orderCreateHelper.createOrder(parameters);

        service.reserveOrder(order);

        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.CREATE_YA_LAVKA_ORDER, order.getId()));
    }

    @Test
    void doNotDegradeIfNoExperimentsTest() {
        checkouterProperties.setEnableLavkaOrderReservationDegradationStrategy(true);
        checkouterProperties.setAntifraudSubstatusEnabled(true);

        serviceConfigurer.configureOrderReservationRequest(HttpStatus.SERVICE_UNAVAILABLE);

        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters());

        assertThrows(ErrorCodeException.class, () -> service.reserveOrder(order));
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.CREATE_YA_LAVKA_ORDER, order.getId()));
    }

}
