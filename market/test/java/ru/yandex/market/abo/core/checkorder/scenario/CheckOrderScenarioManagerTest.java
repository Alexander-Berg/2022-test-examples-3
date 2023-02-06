package ru.yandex.market.abo.core.checkorder.scenario;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.core.checkorder.CheckOrderCreationException;
import ru.yandex.market.abo.core.checkorder.model.CheckOrder;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.core.checkorder.model.ScenarioPayload;
import ru.yandex.market.abo.core.checkorder.scenario.runner.CheckOrderScenarioRunner;
import ru.yandex.market.checkout.checkouter.order.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.IN_PROGRESS;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.NEW;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.SUCCESS;

/**
 * @author komarovns
 * @date 14.06.19
 */
class CheckOrderScenarioManagerTest {
    @InjectMocks
    CheckOrderScenarioManager checkOrderScenarioManager;
    @Mock
    CheckOrderScenarioRunnerResolver checkOrderScenarioRunnerResolver;
    @Mock
    CheckOrderScenarioRunner runner;
    @Mock
    CheckOrderScenario scenario;
    @Mock
    Order order;
    CheckOrderScenarioStatus statusAfter;

    @BeforeEach
    void setUp() throws CheckOrderCreationException {
        MockitoAnnotations.openMocks(this);
        when(checkOrderScenarioRunnerResolver.getRunner(any())).thenReturn(runner);
        when(runner.readyToInitOrder(scenario)).thenReturn(true);
        when(runner.initOrder(scenario)).thenReturn(IN_PROGRESS);
        when(order.getCreationDate()).thenReturn(new Date());
        when(scenario.getCheckOrder()).thenReturn(new CheckOrder(0, false, Collections.emptyList()));
        doAnswer(inv -> statusAfter = (CheckOrderScenarioStatus) inv.getArguments()[0]).when(scenario).setStatus(any());
    }

    @Test
    void processScenario_creationException() throws CheckOrderCreationException {
        when(scenario.getStatus()).thenReturn(NEW, statusAfter);
        CheckOrderCreationException ex = new CheckOrderCreationException(CheckOrderScenarioErrorType.INTERNAL_ERROR);
        doThrow(ex).when(runner).initOrder(scenario);
        assertTrue(checkOrderScenarioManager.processScenario(scenario));
        verify(runner).initOrder(scenario);
        verify(runner).handleOrderCreationException(scenario, ex);
    }

    @Test
    void processScenario_unknownException() throws CheckOrderCreationException {
        when(scenario.getStatus()).thenReturn(NEW);
        RuntimeException exception = new RuntimeException("fooo");
        doThrow(exception).when(runner).initOrder(scenario);
        checkOrderScenarioManager.processScenario(scenario);
        verify(runner).initOrder(scenario);
        verify(runner).handleCheckOrderException(scenario, exception);
    }

    @Test
    void processScenario_initOrder() throws CheckOrderCreationException {
        when(scenario.getStatus()).thenReturn(NEW, statusAfter);
        assertTrue(checkOrderScenarioManager.processScenario(scenario));
        verify(runner).initOrder(scenario);
        verify(scenario).setStatus(IN_PROGRESS);
    }

    @Test
    void processScenario_resolveStatus() {
        when(scenario.getStatus()).thenReturn(IN_PROGRESS, statusAfter);
        when(runner.checkProgress(scenario)).thenReturn(SUCCESS);
        assertTrue(checkOrderScenarioManager.processScenario(scenario));
        verify(scenario).setStatus(SUCCESS);
    }

    @ParameterizedTest
    @EnumSource(value = CheckOrderScenarioStatus.class, names = {"NEW", "IN_PROGRESS"},
            mode = EnumSource.Mode.EXCLUDE)
    void processScenario_illegalStatus(CheckOrderScenarioStatus status) {
        when(scenario.getStatus()).thenReturn(status);
        checkOrderScenarioManager.processScenario(scenario);
        verify(runner).handleCheckOrderException(eq(scenario), any(Exception.class));
    }

    @Test
    void failInProgress() {
        when(scenario.getStatus()).thenReturn(IN_PROGRESS);
        when(runner.checkProgress(scenario)).thenThrow(new UnsupportedOperationException("ups"));
        checkOrderScenarioManager.processScenario(scenario);
        verify(runner).handleCheckOrderException(eq(scenario), any(Exception.class));
    }

    @Test
    void notReadyToInitOrder() throws CheckOrderCreationException {
        when(scenario.getStatus()).thenReturn(NEW);
        when(runner.readyToInitOrder(scenario)).thenReturn(false);
        assertFalse(checkOrderScenarioManager.processScenario(scenario));
        verify(runner, never()).initOrder(scenario);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void payloadChanged(boolean payloadChanged) throws CheckOrderCreationException {
        CheckOrderScenario scenario = new CheckOrderScenario(CheckOrderScenarioType.OFFLINE_ORDER, OrderProcessMethod.PI);
        scenario.setPayload(new ScenarioPayload().setTraceId("foobar"));
        when(runner.readyToInitOrder(scenario)).then(inv -> {
            if (payloadChanged) {
                scenario.addOfferDetectionTimestamp(4234L);
            }
            return false;
        });

        assertEquals(payloadChanged, checkOrderScenarioManager.processScenario(scenario));
    }

    @ParameterizedTest
    @EnumSource(value = CheckOrderScenarioStatus.class, names = {"NEW", "IN_PROGRESS"})
    void setTraceId(CheckOrderScenarioStatus status) {
        when(scenario.getStatus()).thenReturn(status);
        checkOrderScenarioManager.processScenario(scenario);
        verify(scenario).addTrace(any());
    }
}
