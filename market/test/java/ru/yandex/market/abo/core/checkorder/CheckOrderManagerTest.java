package ru.yandex.market.abo.core.checkorder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderStatus;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.core.checkorder.model.CheckOrder;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.core.checkorder.scenario.CheckOrderScenarioManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.FAIL;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.IN_PROGRESS;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.NEW;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus.SUCCESS;

/**
 * @author komarovns
 * @date 17.06.19
 */
public class CheckOrderManagerTest {
    @Mock
    CheckOrderDbService checkOrderDbService;
    @Mock
    CheckOrderScenarioManager checkOrderScenarioManager;
    @InjectMocks
    CheckOrderManager checkOrderManager;
    @Captor
    ArgumentCaptor<CheckOrder> checkOrderCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processCheckOrders_notModified() {
        var checkOrder = createCheckOrder(SUCCESS, IN_PROGRESS);
        when(checkOrderDbService.findChecksInProgress()).thenReturn(List.of(checkOrder));
        checkOrderManager.processCheckOrders();

        verify(checkOrderScenarioManager, times(1)).processScenario(any());
        verify(checkOrderDbService, never()).save(any(CheckOrder.class));
    }

    @Test
    void processCheckOrders_success() {
        var checkOrder = createCheckOrder(SUCCESS, IN_PROGRESS);
        when(checkOrderDbService.findChecksInProgress()).thenReturn(List.of(checkOrder));
        doAnswer(invocation -> {
            var scenario = (CheckOrderScenario) invocation.getArguments()[0];
            scenario.setStatus(SUCCESS);
            return true;
        }).when(checkOrderScenarioManager).processScenario(any());
        checkOrderManager.processCheckOrders();

        verify(checkOrderDbService).save(checkOrderCaptor.capture());
        assertEquals(CheckOrderStatus.SUCCESS, checkOrderCaptor.getValue().getStatus());
    }

    @Test
    void processCheckOrders_orderCreated() {
        var checkOrder = createCheckOrder(NEW);
        long orderId = 32452;
        when(checkOrderDbService.findChecksInProgress()).thenReturn(List.of(checkOrder));

        doAnswer(invocation -> {
            var scenario = (CheckOrderScenario) invocation.getArguments()[0];
            scenario.setStatus(IN_PROGRESS);
            scenario.setOrderId(orderId);
            return true;
        }).when(checkOrderScenarioManager).processScenario(any());
        checkOrderManager.processCheckOrders();
        verify(checkOrderDbService).save(checkOrderCaptor.capture());

        CheckOrder saved = checkOrderCaptor.getValue();
        assertEquals(CheckOrderStatus.IN_PROGRESS, saved.getStatus());
        assertEquals(IN_PROGRESS, saved.getSingleScenario().getStatus());
        assertEquals(orderId, saved.getSingleScenario().getOrderId());
    }

    @Test
    void processCheckOrders_fail() {
        var checkOrder = createCheckOrder(SUCCESS, IN_PROGRESS);
        when(checkOrderDbService.findChecksInProgress()).thenReturn(List.of(checkOrder));
        doAnswer(invocation -> {
            var scenario = (CheckOrderScenario) invocation.getArguments()[0];
            scenario.setStatus(FAIL);
            return true;
        }).when(checkOrderScenarioManager).processScenario(any());
        checkOrderManager.processCheckOrders();

        verify(checkOrderDbService).save(checkOrderCaptor.capture());
        assertEquals(CheckOrderStatus.FAIL, checkOrderCaptor.getValue().getStatus());
    }

    private static CheckOrder createCheckOrder(CheckOrderScenarioStatus... scenarioStatuses) {
        var scenarios = Arrays.stream(scenarioStatuses)
                .map(status -> {
                    var scenario = new CheckOrderScenario(CheckOrderScenarioType.LARGE_ORDER, OrderProcessMethod.PI);
                    scenario.setStatus(status);
                    return scenario;
                })
                .collect(Collectors.toList());
        return new CheckOrder(0, false, scenarios);
    }
}
