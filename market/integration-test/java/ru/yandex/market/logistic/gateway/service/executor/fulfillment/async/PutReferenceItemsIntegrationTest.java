package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.PUT_REFERENCE_ITEMS_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PutReferenceItemsIntegrationTest extends AbstractIntegrationTest {

    private final static long TASK_ID = 100L;

    private final static String UNIQ = "6ea161f870ba6574d3bd9bdd19e1e9d8";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private PutReferenceItemsExecutor putReferenceItemsExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(PUT_REFERENCE_ITEMS_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessTaskUpdateOrderExecutor() throws Exception {
        ClientTask task = getClientTask("fixtures/executors/put_reference_items/put_reference_items_task_message.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/put_reference_items/put_reference_items.xml",
            "fixtures/response/fulfillment/put_reference_items/put_reference_items.xml");

        TaskMessage message = putReferenceItemsExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        assertJsonBodyMatches("fixtures/executors/put_reference_items/put_reference_items_task_response.json", message.getMessageBody());
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_REFERENCE_ITEMS);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
