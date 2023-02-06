package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

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
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.PUT_INBOUND_REGISTRY_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PutInboundRegistryIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;

    private static final String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private PutInboundRegistryExecutor putInboundRegistryExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(PUT_INBOUND_REGISTRY_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getClientTask("fixtures/executors/put_inbound_registry/task_message_ds.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/put-inbound-registry",
            "fixtures/request/common/put_inbound_registry/request_ds.xml",
            "fixtures/response/common/put_inbound_registry/response.xml"
        );

        TaskMessage message = putInboundRegistryExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
        softAssert.assertThat(
            new JsonMatcher(getFileContent(
                "fixtures/executors/put_inbound_registry/task_response_success.json")
            ).matches(message.getMessageBody())
        )
            .as("Method response does not matches with expected JSON")
            .isTrue();
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.DS_PUT_INBOUND_REGISTRY)
            .setMessage(getFileContent(filename));
    }

}
