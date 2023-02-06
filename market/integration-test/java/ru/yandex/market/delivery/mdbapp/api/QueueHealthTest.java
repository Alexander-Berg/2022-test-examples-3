package ru.yandex.market.delivery.mdbapp.api;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.curator.Curator;
import ru.yandex.market.delivery.mdbapp.components.health.components.QueueChecker;
import ru.yandex.market.delivery.mdbapp.components.queue.QueueMonitoring;
import ru.yandex.market.delivery.mdbapp.configuration.queue.CancelParcelQueue;
import ru.yandex.market.delivery.mdbapp.configuration.queue.MailSenderQueue;
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

@RunWith(Parameterized.class)
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
@Sql(scripts = "/data/health/queue/clean-tasks.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/data/health/queue/insert-tasks.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/data/health/queue/clean-tasks.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class QueueHealthTest extends MockContextualTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected QueueChecker queueChecker;

    @Autowired
    protected MailSenderQueue mailQueue;

    @Autowired
    protected CancelParcelQueue cancelParcelQueue;

    @Autowired
    protected QueueMonitoring queueMonitoring;

    @Parameterized.Parameter
    public long mailMaxAttempts;

    @Parameterized.Parameter(1)
    public long mailMaxFail;

    @Parameterized.Parameter(2)
    public long parcelMaxAttempts;

    @Parameterized.Parameter(3)
    public long parcelMaxFail;

    @Parameterized.Parameter(4)
    public String monitoringMessage;

    @Test
    public void allQueuesMonitoringTests() throws Exception {
        mailQueue.setMaxAttempts(mailMaxAttempts);
        mailQueue.setMaxFailedTasks(mailMaxFail);
        cancelParcelQueue.setMaxAttempts(parcelMaxAttempts);
        cancelParcelQueue.setMaxFailedTasks(parcelMaxFail);

        ReflectionTestUtils.invokeMethod(queueMonitoring, "buildJdbcQuery");

        queueChecker.check();

        mvc.perform(get("/health/queue").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andExpect(content().string(monitoringMessage));
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {10, 10, 10, 10, "0;OK"},
            {3, 1, 10, 10, "1;Queue [mail.queue] contains 2 tasks, failed more than 1 times"},
            {1, 1, 10, 10, "1;Queue [mail.queue] contains 8 tasks, failed more than 1 times"},
            {1, 1, 1, 1, "1;Queue [mail.queue] contains 8 tasks, failed more than 1 times, " +
                "Queue [parcel.cancel] contains 7 tasks, failed more than 1 times"}
        });
    }
}
