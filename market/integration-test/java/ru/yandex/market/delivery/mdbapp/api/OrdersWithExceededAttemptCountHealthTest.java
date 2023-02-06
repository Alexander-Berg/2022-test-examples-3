package ru.yandex.market.delivery.mdbapp.api;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.curator.Curator;
import ru.yandex.market.delivery.mdbapp.components.health.components.OrdersWithExceededAttemptCountChecker;
import ru.yandex.market.delivery.mdbapp.controller.DsApiReplyController;
import ru.yandex.market.delivery.mdbapp.scheduled.HealthScheduler;
import ru.yandex.market.delivery.mdbapp.scheduled.checkouter.order.OrderHistoryEventScheduler;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockBean({
    DsApiReplyController.class,
    Curator.class,
    CheckouterAPI.class,
    MbiApiClient.class,
    OrderHistoryEventScheduler.class,
    HealthScheduler.class,
    LMSClient.class,
    PechkinHttpClient.class,
    ScIntClient.class,
})
@DirtiesContext
public class OrdersWithExceededAttemptCountHealthTest extends MockContextualTest {

    @Autowired
    protected OrdersWithExceededAttemptCountChecker checker;

    @Autowired
    protected MockMvc mvc;

    @Test
    @Sql(value = "/data/health/orders_with_exceeded_attempt_count/with_exceeded_counters.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/health/orders_with_exceeded_attempt_count/clean.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void withExceededCounters() throws Exception {

        checker.check();

        mvc.perform(get("/health/ordersWithExceededAttemptCount").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andExpect(content().string("2;Found 2 orders with exceeded attempt count"));
    }

    @Test
    @Sql(value = "/data/health/orders_with_exceeded_attempt_count/without_exceeded_counters.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/health/orders_with_exceeded_attempt_count/clean.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void withNoOneExceededCounter() throws Exception {

        checker.check();

        mvc.perform(get("/health/ordersWithExceededAttemptCount").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));
    }

    @Test
    public void onEmptyDb() throws Exception {
        checker.check();

        mvc.perform(get("/health/ordersWithExceededAttemptCount").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));
    }

}
