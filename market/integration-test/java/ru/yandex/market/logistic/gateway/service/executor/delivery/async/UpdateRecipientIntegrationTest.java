package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.config.properties.PersonalProperties;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class UpdateRecipientIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_TASK_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private UpdateRecipientExecutor updateRecipientExecutor;

    @Autowired
    private PersonalProperties personalProperties;

    @After
    public void tearDown() {
        personalProperties.setPassPersonalFieldsToPartners(false);
    }

    @Test
    public void executeSuccess() throws Exception {
        personalProperties.setPassPersonalFieldsToPartners(true);
        MockRestServiceServer mockServer = createMockServerByRequest(PartnerMethod.UPDATE_RECIPIENT_DS);

        ClientTask task = getClientTask("fixtures/executors/update_order/update_recipient_task_message_lom.json");
        when(repository.findTask(eq(TEST_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/update_recipient/delivery_update_recipient.xml",
            "fixtures/response/delivery/update_recipient/delivery_update_recipient.xml"
        );

        updateRecipientExecutor.execute(new ExecutorTaskWrapper(TEST_TASK_ID, 0));

        mockServer.verify();
    }

    @Test
    public void executeSuccessDoNotPassPrivateFieldsIfPropertyDisabled() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(PartnerMethod.UPDATE_RECIPIENT_DS);

        ClientTask task = getClientTask(
            "fixtures/executors/update_order/update_recipient_task_message_internal_partner.json"
        );
        when(repository.findTask(eq(TEST_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost.market.yandex.net/query-gateway",
            "fixtures/request/delivery/update_recipient/delivery_update_recipient_internal_without_personal.xml",
            "fixtures/response/delivery/update_recipient/delivery_update_recipient.xml"
        );

        updateRecipientExecutor.execute(new ExecutorTaskWrapper(TEST_TASK_ID, 0));

        mockServer.verify();
    }

    @Test
    public void executeSuccessPrivateFields() throws Exception {
        personalProperties.setPassPersonalFieldsToPartners(true);
        MockRestServiceServer mockServer = createMockServerByRequest(PartnerMethod.UPDATE_RECIPIENT_DS);

        ClientTask task = getClientTask(
            "fixtures/executors/update_order/update_recipient_task_message_internal_partner.json"
        );
        when(repository.findTask(eq(TEST_TASK_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost.market.yandex.net/query-gateway",
            "fixtures/request/delivery/update_recipient/delivery_update_recipient_internal.xml",
            "fixtures/response/delivery/update_recipient/delivery_update_recipient.xml"
        );

        updateRecipientExecutor.execute(new ExecutorTaskWrapper(TEST_TASK_ID, 0));

        mockServer.verify();
    }

    private ClientTask getClientTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(TEST_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_UPDATE_RECIPIENT);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
