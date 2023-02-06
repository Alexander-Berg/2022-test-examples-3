package ru.yandex.market.checkout.checkouter.service;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.antifraud.entity.AntifraudActionResult;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.antifraud.AntispamAntifraudMockConfigurer;
import ru.yandex.market.queuedcalls.impl.QueuedCallDao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_ASYNC_PAYMENTS_EXPERIMENT;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class TrustAsyncPayServiceTest extends AbstractArchiveWebTestBase {

    private static final String CARD_ID = "card-123f";
    private static final String LOGIN_ID = "login_id_123";

    @Autowired
    private QueuedCallDao queuedCallDao;

    @Autowired
    private AntispamAntifraudMockConfigurer antispamAntifraudMockConfigurer;

    private static Parameters getParameters() {
        var parameters = defaultBlueOrderParameters();
        parameters.setAsyncPaymentCardId(CARD_ID);
        parameters.setLoginId(LOGIN_ID);
        return parameters;
    }

    @BeforeEach
    public void setUp() {
        checkouterProperties.setEnableAsyncPayments(false);
        checkouterProperties.setAsyncPaymentUsers(Set.of());
        checkouterFeatureWriter.writeValue(ENABLE_ASYNC_PAYMENTS_EXPERIMENT, false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void asyncPaymentsEnabled_shouldCreateAsyncPaymentQC(boolean allowWithout3ds) {
        checkouterProperties.setEnableAsyncPayments(true);
        antispamAntifraudMockConfigurer.mockAntifraud(allowWithout3ds
                ? AntifraudActionResult.ALLOW
                : AntifraudActionResult.DENY);

        Order order = orderCreateHelper.createOrder(getParameters());

        assertOrder(order, allowWithout3ds);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void asyncPaymentsEnabledByExperiment_shouldCreateAsyncPaymentQC(boolean allowWithout3ds) {
        checkouterFeatureWriter.writeValue(ENABLE_ASYNC_PAYMENTS_EXPERIMENT, allowWithout3ds);
        var parameters = getParameters();
        parameters.addExperiment(Experiments.ASYNC_PAYMENT, null);
        antispamAntifraudMockConfigurer.mockAntifraud(allowWithout3ds
                ? AntifraudActionResult.ALLOW
                : AntifraudActionResult.DENY);

        Order order = orderCreateHelper.createOrder(parameters);

        assertOrder(order, allowWithout3ds);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void asyncPaymentsEnabledForUser_shouldCreateAsyncPaymentQC(boolean allowWithout3ds) {
        var parameters = getParameters();
        checkouterProperties.setAsyncPaymentUsers(Set.of(parameters.getBuyer().getUid()));
        antispamAntifraudMockConfigurer.mockAntifraud(allowWithout3ds
                ? AntifraudActionResult.ALLOW
                : AntifraudActionResult.DENY);

        Order order = orderCreateHelper.createOrder(parameters);

        assertOrder(order, allowWithout3ds);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void asyncPaymentsDisabled_shouldNotCreateAsyncPaymentQC(boolean allowWithout3ds) {
        Order order = orderCreateHelper.createOrder(getParameters());
        antispamAntifraudMockConfigurer.mockAntifraud(allowWithout3ds
                ? AntifraudActionResult.ALLOW
                : AntifraudActionResult.DENY);

        assertOrder(order, false);
    }

    private void assertOrder(Order order, boolean expectedAsync) {
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        var expectedStatus = expectedAsync ? OrderSubstatus.AWAIT_PAYMENT : OrderSubstatus.WAITING_USER_INPUT;
        assertEquals(expectedStatus, order.getSubstatus());
        var existedCalls = queuedCallDao.existsNotCompletedCallsForObjects(
                Set.of(CheckouterQCType.ASYNC_PAYMENT_PROCESSING),
                Set.of(order.getId()));
        assertThat(existedCalls, hasSize(expectedAsync ? 1 : 0));
    }
}
