package ru.yandex.market.checkout.checkouter.controllers;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.AbstractQueuedCallTestBase;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.CashParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;
import ru.yandex.market.queuedcalls.QueuedCallType;
import ru.yandex.market.queuedcalls.impl.QueuedCallLocalTask;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;

public class QueuedCallsControllerTest extends AbstractQueuedCallTestBase {

    @Test
    public void testAddQueuedCall() throws Exception {
        Instant someTime0 = freezeTime();

        mockMvc.perform(
                        post("/queuedcalls/add")
                                .param("type", ORDER_CREATE_CASH_PAYMENT.name())
                                .param("objectId", "1111"))
                .andDo(log())
                .andExpect(status().is2xxSuccessful());

        checkCallInQueue(ORDER_CREATE_CASH_PAYMENT, 1111L, equalTo(someTime0));
    }

    @Test
    public void testCancelQueuedCall() throws Exception {
        Instant someTime1 = freezeTime();
        mockMvc.perform(
                        post("/queuedcalls/add")
                                .param("type", ORDER_CREATE_CASH_PAYMENT.name())
                                .param("objectId", "1111"))
                .andDo(log())
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                        post("/queuedcalls/cancel")
                                .param("type", ORDER_CREATE_CASH_PAYMENT.name())
                                .param("objectId", "1111")
                                .param("comment", "УДОЛИ МЕНЯ!!!!!!111один"))
                .andDo(log())
                .andExpect(status().is2xxSuccessful());

        checkCallCompleted(ORDER_CREATE_CASH_PAYMENT, 1111L,
                equalTo(someTime1), 0, equalTo("УДОЛИ МЕНЯ!!!!!!111один"));
    }

    @Test
    public void testGlobalSuspendExecution() throws Exception {
        mockMvc.perform(post("/queuedcalls/suspend-processing"))
                .andDo(log())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.globallySuspended", equalTo(true)));

        Instant someTime0 = freezeTime();

        mockMvc.perform(
                        post("/queuedcalls/add")
                                .param("type", ORDER_CREATE_CASH_PAYMENT.name())
                                .param("objectId", "1111"))
                .andDo(log())
                .andExpect(status().is2xxSuccessful());

        //несмотря на успешную имплементацию, обработка не должна проходить
        executeQueuedCalls(ORDER_CREATE_CASH_PAYMENT, id -> {
            assertEquals(1111L, id.longValue());
            return null;
        });
        checkCallInQueue(ORDER_CREATE_CASH_PAYMENT, 1111L, equalTo(someTime0));

        Instant someTime1 = freezeTime();

        mockMvc.perform(post("/queuedcalls/resume-processing"))
                .andDo(log())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.globallySuspended", equalTo(false)));

        //теперь обработка должна пройти
        executeQueuedCalls(ORDER_CREATE_CASH_PAYMENT, id -> {
            assertEquals(1111L, id.longValue());
            return null;
        });
        checkCallCompletedAfterExecution(ORDER_CREATE_CASH_PAYMENT, 1111L, equalTo(someTime1));
    }

    @Test
    public void testTypeSuspendExecution() throws Exception {
        mockMvc.perform(post("/queuedcalls/stop-processing/" + ORDER_CREATE_CASH_PAYMENT.name()))
                .andDo(log())
                .andExpect(status().is2xxSuccessful());

        Instant someTime0 = freezeTime();
        createQueuedCall(ORDER_CREATE_CASH_PAYMENT, 123L);
        createQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, 234L);

        Instant someTime1 = freezeTime();

        //несмотря на успешную имплементацию, обработка не должна проходить
        executeQueuedCalls(ORDER_CREATE_CASH_PAYMENT, id -> {
            assertEquals(123L, id.longValue());
            return null;
        });
        checkCallInQueue(ORDER_CREATE_CASH_PAYMENT, 123L, equalTo(someTime0));

        //а этот должен обработаться
        executeQueuedCalls(ORDER_CREATE_SUBSIDY_PAYMENT, id -> {
            assertEquals(234L, id.longValue());
            return null;
        });
        checkCallCompletedAfterExecution(ORDER_CREATE_SUBSIDY_PAYMENT, 234L, equalTo(someTime1));


        // включаем, выполняем и проверяем что обработается
        mockMvc.perform(post("/queuedcalls/start-processing/" + ORDER_CREATE_CASH_PAYMENT.name()))
                .andDo(log())
                .andExpect(status().is2xxSuccessful());
        executeQueuedCalls(ORDER_CREATE_CASH_PAYMENT, id -> {
            assertEquals(123L, id.longValue());
            return null;
        });
        checkCallCompletedAfterExecution(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT, 123L, equalTo(someTime1));
    }

    @Test
    public void testSynchronousExecution() throws Exception {
        freezeTime();

        // сделать кешовый заказ
        Parameters parameters = CashParametersProvider.createCashParameters(false);
        Order order = orderCreateHelper.createOrder(parameters);
        ShopMetaData shopMetaData = ShopSettingsHelper.createCustomNewPrepayMeta(FulfilmentProvider.FF_SHOP_ID
                .intValue());
        shopService.updateMeta(FulfilmentProvider.FF_SHOP_ID, shopMetaData);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, order.getId()));

        Instant someTime1 = freezeTime();
        trustMockConfigurer.mockWholeTrust();

        long orderId = order.getId();
        mockMvc.perform(
                        post("/queuedcalls/execute")
                                .param("type", ORDER_CREATE_CASH_PAYMENT.name())
                                .param("objectId", String.valueOf(orderId)))
                .andDo(log())
                .andExpect(status().is2xxSuccessful());

        checkCallCompletedAfterExecution(ORDER_CREATE_CASH_PAYMENT, orderId, equalTo(someTime1));
    }

    @Test
    public void testExecutionOfAbsentQueuedCall() throws Exception {
        mockMvc.perform(
                        post("/queuedcalls/execute")
                                .param("type", ORDER_CREATE_CASH_PAYMENT.name())
                                .param("objectId", "1111"))
                .andDo(log())
                .andExpect(status().is4xxClientError());
    }


    @Test
    public void testFailedExecutionQueuedCall() throws Exception {
        //пятисотим патамушта нет обработчика
        mockMvc.perform(
                        post("/queuedcalls/execute")
                                .param("type", "-1")
                                .param("objectId", "1111"))
                .andDo(log())
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void requestIdTest() {
        List<String> firstParts = new LinkedList<>();
        List<String> lastParts = new LinkedList<>();
        QueuedCallProcessor processor = new QueuedCallProcessorStub() {
            @Override
            public ExecutionResult process(QueuedCallExecution execution) {
                String requestId = MDC.get("requestId");

                // Проверяем, что трассировака не null
                assertThat(requestId, notNullValue());
                int lastSlashIndex = requestId.lastIndexOf('/');
                firstParts.add(requestId.substring(0, lastSlashIndex));
                lastParts.add(requestId.substring(lastSlashIndex + 1));
                return null;
            }
        };
        QueuedCallType type = processor.getSupportedType();
        QueuedCallLocalTask localTask = new QueuedCallLocalTask(processor, queuedCallService, () -> false, "test.ya" +
                ".ru");
        transactionTemplate.execute(t -> {
            queuedCallService.addQueuedCalls(type, List.of(1L, 2L, 3L));
            return null;
        });
        localTask.run();

        // Проверяем, что процессор вызывался 3 раза
        assertThat(firstParts, hasSize(3));
        assertThat(lastParts, hasSize(3));
        // Проверяем, что основная часть трассировки общая для всех вызовов
        assertThat(Set.copyOf(firstParts), hasSize(1));
        // Проверяем, что первая суффикс трассировки различается для всех вызовов
        assertThat(Set.copyOf(lastParts), hasSize(3));
    }

    @Test
    public void shouldReturnQueuedCallsTypes() throws Exception {
        mockMvc.perform(get("/queuedcalls/types"))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(CheckouterQCType.values().length)))
                .andExpect(jsonPath("$[?(@.name=='ORDER_CREATE_CASH_PAYMENT')]").value(hasSize(1)))
                .andExpect(jsonPath("$[?(@.name=='ORDER_CREATE_CASH_PAYMENT')].description").value(
                        everyItem(is(ORDER_CREATE_CASH_PAYMENT.getDescription()))));
    }

    @Test
    public void shouldReturnIsPaymentTask() throws Exception {
        mockMvc.perform(get("/queuedcalls/types"))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(CheckouterQCType.values().length)))
                .andExpect(jsonPath("$[?(@.name=='ORDER_CREATE_CASH_PAYMENT')].isPaymentTask").value(
                        everyItem(is(true))))
                .andExpect(jsonPath("$[?(@.name=='POSTPONED_PUSH_API')].isPaymentTask").value(
                        everyItem(is(false))));
    }
}
