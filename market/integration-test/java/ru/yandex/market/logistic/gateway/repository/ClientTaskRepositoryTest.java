package ru.yandex.market.logistic.gateway.repository;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.querydsl.core.types.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.model.converter.StringToRequestFlowConverter;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;

import static ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow.DS_CREATE_ORDER;
import static ru.yandex.market.logistic.gateway.model.TaskStatus.NEW;

public class ClientTaskRepositoryTest extends AbstractIntegrationTest {

    private static final LocalDateTime CREATED_UPDATED_DATE_TIME = DateUtil.asLocalDateTime(
        DateUtil.convertToDate("2118-01-01 00:00:00"));

    private static final String EMPTY_MESSAGE = "{}";

    private static final ClassTypeInformation<ClientTask> CLIENT_TASK_TYPE =
        ClassTypeInformation.from(ClientTask.class);

    private static final QuerydslBindings DEFAULT_BINDINGS = new QuerydslBindings();

    private QuerydslPredicateBuilder builder;

    private MultiValueMap<String, String> values;

    @Autowired
    private ClientTaskRepository clientTaskRepository;

    @Before
    public void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToRequestFlowConverter());

        this.builder = new QuerydslPredicateBuilder(
            conversionService,
            SimpleEntityPathResolver.INSTANCE
        );

        this.values = new LinkedMultiValueMap<>();
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_for_support_single.xml")
    public void getSingleClientTask() {
        softAssert.assertThat(
            clientTaskRepository.findAll(PageRequest.of(0, 10)))
            .as("There is only one task in db")
            .hasSize(1)
            .as("Task has correct fields")
            .containsExactly(getClientTask());
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_for_support_single.xml")
    public void findClientTaskById() {
        softAssert.assertThat(
            clientTaskRepository.findTask(1L))
            .as("Task was found by id")
            .isNotNull()
            .as("Task has correct fields")
            .isEqualTo(getClientTask());
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_for_support_multiple.xml")
    public void getClientTasksWithPagination() {
        softAssert.assertThat(
            clientTaskRepository.findAll(PageRequest.of(1, 2)))
            .as("There is only one task on second page")
            .hasSize(1);
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_for_support_multiple.xml")
    public void getClientTasksByRequestFlow() {
        values.add("flow", "ds-create-order");

        Predicate predicate = builder.getPredicate(CLIENT_TASK_TYPE, values, DEFAULT_BINDINGS);

        Page<ClientTask> clientTaskPage = clientTaskRepository.findAll(predicate, PageRequest.of(0, 10));

        softAssert.assertThat(clientTaskPage.getContent())
            .as("There is only one task with this flow")
            .hasSize(1);
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_for_support_entity_id.xml")
    public void getClientTaskByEntityId() {
        Page<ClientTask> clientTaskPage = clientTaskRepository.findByEntityId("123456", PageRequest.of(0, 10));

        softAssert.assertThat(clientTaskPage.getContent())
            .as("Asserting that the client tasks list size equals 1")
            .hasSize(1);
        softAssert.assertThat(clientTaskPage.getContent().get(0).getId())
            .as("Asserting that the client task id is valid")
            .isEqualTo(2);
    }

    private ClientTask getClientTask() {
        return new ClientTask()
            .setId(1L)
            .setRootId(1L)
            .setFlow(DS_CREATE_ORDER)
            .setMessage(EMPTY_MESSAGE)
            .setStatus(NEW)
            .setConsumer(null)
            .setCreated(CREATED_UPDATED_DATE_TIME)
            .setUpdated(CREATED_UPDATED_DATE_TIME);
    }
}
