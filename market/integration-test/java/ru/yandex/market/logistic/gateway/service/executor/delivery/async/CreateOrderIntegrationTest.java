package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.IOException;
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

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.config.properties.PersonalProperties;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistics.personal.model.Address;
import ru.yandex.market.personal.PersonalClient;
import ru.yandex.market.personal.enums.PersonalDataType;
import ru.yandex.market.personal.model.PersonalResponseItem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CREATE_ORDER_DS;
import static ru.yandex.market.logistic.gateway.utils.CommonDtoFactory.createPersonalResponse;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class CreateOrderIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

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
    public void setup() throws IOException {
        mockServer = createMockServerByRequest(CREATE_ORDER_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
        personalProperties.setPassPersonalFieldsToPartners(false);
        personalProperties.setConvertPersonalToOpenForExternalPartners(false);
        personalProperties.setConvertLocationTo(true);
        personalProperties.setConvertRecipient(true);
    }

    @Test
    public void executeSuccessWithParcelId() throws Exception {
        ClientTask task = getClientTaskWithParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_with_parcel_id.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_with_parcel_id.xml");

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent("fixtures/executors/create_order/create_order_task_with_parcel_response.json"));
    }

    @Test
    public void executeSuccessWithItemSupplier() throws Exception {
        ClientTask task = getClientTaskWithItemSupplier();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_with_item_supplier.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml");

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent("fixtures/executors/create_order/create_order_task_without_parcel_response.json"));
    }

    @Test
    public void executeSuccessWithoutParcelId() throws Exception {
        ClientTask task = getClientTaskWithoutParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_without_parcel_id.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml");

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent("fixtures/executors/create_order/create_order_task_without_parcel_response.json"));
    }

    @Test
    public void executeSuccessWithRestrictedData() throws Exception {
        ClientTask task = getClientTaskWithRestrictedData();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_with_restricted_data.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml");

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent("fixtures/executors/create_order/create_order_task_without_parcel_response.json"));
    }

    @Test
    public void executeSuccessWithItemInstances() throws Exception {
        ClientTask task = getClientTaskWithItemInstances();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_with_item_instances.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml");

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent("fixtures/executors/create_order/create_order_task_without_parcel_response.json"));
    }

    @Test
    public void executeSuccessWithItemInstancesDuplicateCis() throws Exception {
        ClientTask task = getClientTaskWithItemInstancesDuplicateCis();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        softAssert.assertThatThrownBy(() -> createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicate key CIS (attempted merging values qwerty0987 and qwerty0987)");
    }

    @Test
    public void executeSuccessWithItemInstancesDuplicateOther() throws Exception {
        ClientTask task = getClientTaskWithItemInstancesDuplicateOther();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        softAssert.assertThatThrownBy(() -> createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Duplicate key UNRELATED (attempted merging values dmVyeV91bnJlbGF0ZWQ= and dmVyeV91bnJlbGF0ZWQ=)"
            );
    }

    @Test
    public void executeSuccessWithPersonalDataWithoutPassing() throws Exception {
        ClientTask task = getClientTaskWithPersonalData();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost.market.yandex.net/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_with_personal_data_without_passing.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml"
        );

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent(
                "fixtures/executors/create_order/create_order_task_message_with_personal_data_response.json"
            ));
    }

    @Test
    public void executeSuccessWithPersonalDataWithPassing() throws Exception {
        personalProperties.setPassPersonalFieldsToPartners(true);
        ClientTask task = getClientTaskWithPersonalData();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost.market.yandex.net/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_with_personal_data.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml"
        );

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent(
                "fixtures/executors/create_order/create_order_task_message_with_personal_data_response.json"
            ));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void replaceOpenDataToReceivedFromPersonal() throws Exception {
        personalProperties.setConvertPersonalToOpenForExternalPartners(true);
        ArgumentCaptor<List<Pair<String, PersonalDataType>>> captor = ArgumentCaptor.forClass(List.class);
        ClientTask task = getClientTaskWithRestrictedData();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);
        when(personalClient.multiTypesRetrieve(any())).thenReturn(createPersonalResponse());

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_with_replaced_open_data.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml"
        );

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        verify(personalClient).multiTypesRetrieve(captor.capture());

        softAssert.assertThat(captor.getValue()).containsExactlyInAnyOrder(
            createPersonalResponse().stream()
                .map(ri -> Pair.of(ri.getPersonalId(), ri.getDataType()))
                .toArray(Pair[]::new)
        );

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent("fixtures/executors/create_order/create_order_task_without_parcel_response.json"));
    }

    @Test
    public void replaceOnlyLocationFromPersonal() throws Exception {
        personalProperties.setConvertPersonalToOpenForExternalPartners(true);
        personalProperties.setConvertRecipient(false);
        ClientTask task = getClientTaskWithRestrictedData();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);
        when(personalClient.multiTypesRetrieve(any())).thenReturn(createPersonalResponse());

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_with_replaced_location_to.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml"
        );

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent("fixtures/executors/create_order/create_order_task_without_parcel_response.json"));
    }

    @Test
    public void replaceRegionForCdekInOpenData() throws Exception {
        personalProperties.setConvertPersonalToOpenForExternalPartners(true);
        ClientTask task = getClientTaskForCdek();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);
        PersonalResponseItem personalLocationResponse = new PersonalResponseItem(
            "location_to_address_id",
            PersonalDataType.ADDRESS,
            createAddressWithReplacedRegion()
        );
        when(personalClient.multiTypesRetrieve(any())).thenReturn(List.of(personalLocationResponse));

        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/create_order/delivery_create_order_cdek_with_replaced_open_data.xml",
            "fixtures/response/delivery/create_order/delivery_create_order_without_parcel_id.xml"
        );

        TaskMessage message = createOrderExecutor.execute(new ExecutorTaskWrapper(100L, 0));

        verify(personalClient).multiTypesRetrieve(any());
        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent("fixtures/executors/create_order/create_order_task_cdek_response.json"));
    }

    private ClientTask getClientTaskWithParcelId() throws IOException {
        return getClientTask("fixtures/executors/create_order/create_order_task_message_with_parcel_id.json");
    }

    private ClientTask getClientTaskWithItemSupplier() throws IOException {
        return getClientTask("fixtures/executors/create_order/create_order_task_message_with_item_supplier.json");
    }

    private ClientTask getClientTaskWithoutParcelId() throws IOException {
        return getClientTask("fixtures/executors/create_order/create_order_task_message_without_parcel_id.json");
    }

    private ClientTask getClientTaskWithRestrictedData() throws IOException {
        return getClientTask("fixtures/executors/create_order/create_order_task_message_with_restricted_data.json");
    }

    private ClientTask getClientTaskForCdek() throws IOException {
        return getClientTask("fixtures/executors/create_order/create_order_task_message_for_cdek.json");
    }

    private ClientTask getClientTaskWithPersonalData() throws IOException {
        return getClientTask(
            "fixtures/executors/create_order/create_order_task_message_with_personal_data.json");
    }

    private ClientTask getClientTaskWithItemInstances() throws IOException {
        return getClientTask("fixtures/executors/create_order/create_order_task_message_with_item_instances.json");
    }

    private ClientTask getClientTaskWithItemInstancesDuplicateCis() throws IOException {
        return getClientTask(
            "fixtures/executors/create_order/create_order_task_message_with_item_instances_duplicate_cis.json"
        );
    }

    private ClientTask getClientTaskWithItemInstancesDuplicateOther() throws IOException {
        return getClientTask(
            "fixtures/executors/create_order/create_order_task_message_with_item_instances_duplicate_other.json"
        );
    }

    private ClientTask getClientTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_CREATE_ORDER);
        task.setMessage(getFileContent(filename));
        return task;
    }

    private Address createAddressWithReplacedRegion() {
        return Address.builder()
            .country("country_from_personal")
            .federalDistrict("federalDistrict_from_personal")
            .region("region_from_personal")
            .locality("locality_from_personal")
            .subRegion("subRegion_from_personal")
            .settlement("settlement_from_personal")
            .district("district_from_personal")
            .street("street_from_personal")
            .house("house_from_personal")
            .building("building_from_personal")
            .housing("housing_from_personal")
            .room("room_from_personal")
            .zipCode("zipCode_from_personal")
            .porch("porch_from_personal")
            .floor(12)
            .metro("metro_from_personal")
            .geoId(120013)
            .intercom("intercom_from_personal")
            .comment("comment_from_personal")
            .build();
    }
}
