package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.config.properties.PersonalProperties;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.personal.PersonalClient;
import ru.yandex.market.personal.enums.PersonalDataType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CREATE_ORDER_FF;
import static ru.yandex.market.logistic.gateway.utils.CommonDtoFactory.createPersonalResponse;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateOrderIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    private final static String UNIQ = "6ea161f870ba6574d3bd9bdd19e1e9d8";

    @MockBean
    private ClientTaskRepository repository;

    @Autowired
    private CreateOrderExecutor createOrderExecutor;

    @Autowired
    private PersonalProperties personalProperties;

    @Autowired
    private PersonalClient personalClient;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(CREATE_ORDER_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        personalProperties.setPassPersonalFieldsToPartners(false);
        personalProperties.setConvertPersonalToOpenForExternalPartners(false);
        personalProperties.setConvertLocationTo(true);
        personalProperties.setConvertRecipient(true);
        mockServer.verify();
    }

    @Test
    public void testExecuteTaskCreateOrderExecutorSuccess() {
        executeAndAssertTaskCreateOrderExecutorSuccess("fulfillment_create_order");
    }

    @Test
    public void testExecuteTaskCreateOrderExtendedExecutorSuccess() {
        personalProperties.setPassPersonalFieldsToPartners(true);
        executeAndAssertTaskCreateOrderExecutorSuccess("fulfillment_create_order_extended");
        verifyNoInteractions(personalClient);
    }

    @Test
    public void passPersonalFieldsToInternalPartner() {
        personalProperties.setPassPersonalFieldsToPartners(true);
        ClientTask task = getClientTask("fulfillment_create_order_internal_partner_task_message.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost.market.yandex.net/query-gateway",
            "fixtures/request/fulfillment/create_order/fulfillment_create_order_with_personal_fields.xml",
            "fixtures/response/fulfillment/create_order/fulfillment_create_order.xml"
        );

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));
        assertJsonBodyMatches(
            "fixtures/executors/fulfillment_create_order/fulfillment_create_order_internal_partner_task_response.json",
            message.getMessageBody()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void replaceOpenDataToReceivedFromPersonal() {
        personalProperties.setConvertPersonalToOpenForExternalPartners(true);
        ArgumentCaptor<List<Pair<String, PersonalDataType>>> captor = ArgumentCaptor.forClass(List.class);
        ClientTask task = getClientTask("fulfillment_create_order_extended_task_message.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);
        when(personalClient.multiTypesRetrieve(any())).thenReturn(createPersonalResponse());

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/create_order/fulfillment_create_order_with_open_data_from_personal.xml",
            "fixtures/response/fulfillment/create_order/fulfillment_create_order.xml"
        );

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        verify(personalClient).multiTypesRetrieve(captor.capture());

        softAssert.assertThat(captor.getValue()).containsExactlyInAnyOrder(
            createPersonalResponse().stream()
                .map(ri -> Pair.of(ri.getPersonalId(), ri.getDataType()))
                .toArray(Pair[]::new)
        );

        assertJsonBodyMatches(
            "fixtures/executors/fulfillment_create_order/fulfillment_create_order_task_response.json",
            message.getMessageBody()
        );
    }

    @Test
    public void replaceOnlyRecipientFromPersonal() {
        personalProperties.setConvertPersonalToOpenForExternalPartners(true);
        personalProperties.setConvertLocationTo(false);
        ClientTask task = getClientTask("fulfillment_create_order_extended_task_message.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);
        when(personalClient.multiTypesRetrieve(any())).thenReturn(createPersonalResponse());

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/create_order/fulfillment_create_order_with_recipient_from_personal.xml",
            "fixtures/response/fulfillment/create_order/fulfillment_create_order.xml"
        );

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        assertJsonBodyMatches(
            "fixtures/executors/fulfillment_create_order/fulfillment_create_order_task_response.json",
            message.getMessageBody()
        );
    }

    @Test(expected = ValidationException.class)
    public void testExecuteTaskCreateOrderExecutorValidationFailed() {
        ClientTask task = getClientTask("fulfillment_create_order_task_message_validation_failed.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        createOrderExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));
    }

    @Test
    public void executeWithItemInstancesDuplicateCis() {
        ClientTask task = getClientTask("fulfillment_create_order_task_message_with_item_instances_duplicate_cis.json");

        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        softAssert.assertThatThrownBy(() -> createOrderExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicate key CIS (attempted merging values qwerty0987 and qwerty0987)");
    }

    @Test
    public void executeSuccessWithItemInstancesDuplicateOther() {
        ClientTask task = getClientTask(
            "fulfillment_create_order_task_message_with_item_instances_duplicate_other.json"
        );

        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        softAssert.assertThatThrownBy(() -> createOrderExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Duplicate key UNRELATED (attempted merging values dmVyeV91bnJlbGF0ZWQ= and dmVyeV91bnJlbGF0ZWQ=)"
            );
    }

    private void executeAndAssertTaskCreateOrderExecutorSuccess(String fileName) {
        ClientTask task = getClientTask(fileName + "_task_message.json");
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/create_order/" + fileName + ".xml",
            "fixtures/response/fulfillment/create_order/fulfillment_create_order.xml");

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));
        assertJsonBodyMatches("fixtures/executors/fulfillment_create_order/fulfillment_create_order_task_response.json",
            message.getMessageBody());
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_CREATE_ORDER);
        task.setMessage(getFileContent("fixtures/executors/fulfillment_create_order/" + filename));
        return task;
    }


}
