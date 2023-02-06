package ru.yandex.market.crm.operatorwindow.toloka;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.crm.operatorwindow.integration.Brands;
import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.TicketVersion;
import ru.yandex.market.jmf.module.toloka.AssessmentPool;
import ru.yandex.market.jmf.module.toloka.AssessmentRule;
import ru.yandex.market.jmf.module.toloka.Service;
import ru.yandex.market.jmf.module.toloka.Ticket;
import ru.yandex.market.jmf.module.toloka.TolokaExchanger;
import ru.yandex.market.jmf.module.toloka.model.TolokaAssignmentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSESSMENT_BROKEN_RESULTS_PATH;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSESSMENT_RESULTS_PATH;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSIGNMENTS_EMPTY;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.CREATED_POOL_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.EMPTY_TASKS_LIST;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.NO_ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_OPERATION_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_PENDING_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_SUCCESS_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_OPEN_OPERATION_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_OPEN_PENDING_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_OPEN_SUCCESS_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.REFERENCE_POOL_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.TASKS_CREATE_SUCCESS_RESPONSE;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.TOLOKA_TASKS;
import static ru.yandex.market.jmf.utils.serialize.SerializationConfiguration.JMF_OBJECT_MAPPER;

/**
 * Кейсы проверки вычитки ответов ассессоров Толоки/Янга. <b>Важно!</b> Имитируются ответы API клиента,
 * проверка UI не производится
 * <p/>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1332<br/>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1333<br/>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1334<br/>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1335<br/>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1336<br/>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TolokaResultsImportTest extends TolokaAbstractTest {

    @Inject
    @Named(JMF_OBJECT_MAPPER)
    private ObjectMapper objectMapper;
    @Inject
    private TolokaExchanger tolokaExchanger;

    private enum Scenario {
        EXCHANGE_DISABLED, // обмен с толокой выключен
        POSITIVE, // получили ответы
        NO_ASSIGNMENTS, // все ок, но ответов еще нет
        ASSIGNMENTS_REQUEST_ERROR, // развал при запросе ответов
        ASSIGNMENTS_MAPPING_ERROR // ответы получили, но развалились при заполнении обращения
    }

    @BeforeEach
    public void setUp() {
        when(tolokaClient.clonePool(REFERENCE_POOL_ID)).thenReturn(POOL_CLONE_PENDING_OPERATION);
        when(tolokaClient.getOperation(POOL_CLONE_OPERATION_ID)).thenReturn(POOL_CLONE_SUCCESS_OPERATION);
        when(tolokaClient.getTasks(eq(CREATED_POOL_ID), anyInt())).thenReturn(EMPTY_TASKS_LIST);
        when(tolokaClient.createTasks(anyList())).thenReturn(TASKS_CREATE_SUCCESS_RESPONSE);
        when(tolokaClient.openPool(CREATED_POOL_ID)).thenReturn(POOL_OPEN_PENDING_OPERATION);
        when(tolokaClient.getOperation(POOL_OPEN_OPERATION_ID)).thenReturn(POOL_OPEN_SUCCESS_OPERATION);
    }

    @AfterEach
    public void tearDown() {
        txService.runInNewTx(this::clear);
    }

    @ParameterizedTest(name = "Scenario = {0}")
    @EnumSource(Scenario.class)
    public void tolokaResultImportTest(Scenario scenario) {

        var context = new Context(); // для переноса значений через транзакции
        var assessmentTicketCount = TOLOKA_TASKS.size();

        txService.runInNewTx(() -> triggerService.withSyncTriggersMode(() -> {
            setTolokaExchangeEnabled(true);

            var rules = generateAssessmentRules(3);
            var ruleToAssign = rules[1];
            var ruleToNotAssign = rules[0];
            bcpService.edit(ruleToAssign, AssessmentRule.POOL_ID, REFERENCE_POOL_ID);

            var services = generateServices(3);
            bcpService.edit(services[1], Service.ASSESSMENT_RULE, ruleToAssign); // очереди назначили правило

            for (var service : services) { // наполняем очереди тикетами
                for (int i = 0; i < TOLOKA_TASKS.size(); i++) {
                    ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                    ticketTestUtils.createTicket(NO_ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                }
            }
            tolokaExchanger.publishTasks(ruleToAssign);

            context.ruleToAssign = ruleToAssign;
            context.ruleToNotAssign = ruleToNotAssign;
        }));

        // независимо от сценария, не должно быть обращений по "левому" правилу
        tolokaExchanger.getResults(context.ruleToNotAssign);
        verify(tolokaClient, never()).getAssignments(anyString(), any(), anyBoolean());
        clearInvocations(tolokaClient);

        Set<Ticket> assessmentTicketsBefore = findAssessmentTickets(Ticket.STATUS_ASSESSMENT);
        switch (scenario) {
            case EXCHANGE_DISABLED -> { // обмен с толокой выключен
                setTolokaExchangeEnabled(false);
                tolokaExchanger.getResults(context.ruleToAssign);

                verifyNoInteractions(tolokaClient);
                assertEquals(assessmentTicketCount, findAssessmentTickets(Ticket.STATUS_ASSESSMENT).size());
                assertEquals(assessmentTicketsBefore, findAssessmentTickets(Ticket.STATUS_ASSESSMENT));
            }
            case POSITIVE -> { // получили ответы
                setTolokaExchangeEnabled(true);
                TolokaAssignmentList assignments = prepareAssignments(ASSESSMENT_RESULTS_PATH);
                when(tolokaClient.getAssignments(eq(CREATED_POOL_ID), any(), anyBoolean())).thenReturn(assignments);
                tolokaExchanger.getResults(context.ruleToAssign);

                verify(tolokaClient, times(1)).getAssignments(eq(CREATED_POOL_ID), any(), anyBoolean());
                verifyNoMoreInteractions(tolokaClient);
                assertEquals(1, findAssessmentTickets(Ticket.STATUS_REOPENED).size());
                assertEquals(0, findAssessmentTickets(Ticket.STATUS_ASSESSMENT).size());
                assertEquals(2, findAssessmentTickets(Ticket.STATUS_RESOLVED).size());
            }
            case NO_ASSIGNMENTS -> { // все ок, но ответов еще нет
                setTolokaExchangeEnabled(true);
                when(tolokaClient.getAssignments(eq(CREATED_POOL_ID), any(), anyBoolean()))
                        .thenReturn(ASSIGNMENTS_EMPTY);
                tolokaExchanger.getResults(context.ruleToAssign);

                verify(tolokaClient, times(1)).getAssignments(eq(CREATED_POOL_ID), any(), anyBoolean());
                verifyNoMoreInteractions(tolokaClient);
                assertEquals(assessmentTicketCount, findAssessmentTickets(Ticket.STATUS_ASSESSMENT).size());
                assertEquals(assessmentTicketsBefore, findAssessmentTickets(Ticket.STATUS_ASSESSMENT));
            }
            case ASSIGNMENTS_REQUEST_ERROR -> { // развал при запросе ответов
                setTolokaExchangeEnabled(true);
                when(tolokaClient.getAssignments(anyString(), any(), anyBoolean()))
                        .thenThrow(new RuntimeException());
                tolokaExchanger.getResults(context.ruleToAssign);

                verify(tolokaClient, times(1)).getAssignments(eq(CREATED_POOL_ID), any(), anyBoolean());
                verifyNoMoreInteractions(tolokaClient);
                assertEquals(assessmentTicketCount, findAssessmentTickets(Ticket.STATUS_ASSESSMENT).size());
                assertEquals(assessmentTicketsBefore, findAssessmentTickets(Ticket.STATUS_ASSESSMENT));
            }
            case ASSIGNMENTS_MAPPING_ERROR -> { // ответы получили, но развалились при заполнении части обращений
                setTolokaExchangeEnabled(true);
                TolokaAssignmentList brokenAssignments = prepareAssignments(ASSESSMENT_BROKEN_RESULTS_PATH);
                when(tolokaClient.getAssignments(eq(CREATED_POOL_ID), any(), anyBoolean()))
                        .thenReturn(brokenAssignments);
                tolokaExchanger.getResults(context.ruleToAssign);

                verify(tolokaClient, times(1)).getAssignments(eq(CREATED_POOL_ID), any(), anyBoolean());
                verifyNoMoreInteractions(tolokaClient);
                assertEquals(2, findAssessmentTickets(Ticket.STATUS_REOPENED).size());
                assertEquals(0, findAssessmentTickets(Ticket.STATUS_ASSESSMENT).size());
                assertEquals(1, findAssessmentTickets(Ticket.STATUS_RESOLVED).size());
            }
        }
    }

    private TolokaAssignmentList prepareAssignments(String path) {
        try {
            byte[] bytes = ResourceHelpers.getResource(path);
            return objectMapper.readValue(bytes, TolokaAssignmentList.class);
        } catch (IOException e) {
            return null;
        }
    }

    private Set<Ticket> findAssessmentTickets(String status) {
        return txService.doInNewTx(() -> {
            Query assessmentTicketsQuery = Query.of(Ticket.FQN)
                    .withFilters(Filters.eq(Ticket.STATUS, status));
            return CrmCollections.asSet(entityStorage.list(assessmentTicketsQuery));
        });
    }

    private void clear() {
        Stream.of(Ticket.FQN, TicketVersion.FQN, AssessmentRule.FQN, AssessmentPool.FQN)
                .map(Query::of)
                .map(dbService::list)
                .flatMap(Collection::stream)
                .forEach(dbService::delete);
        dbService.list(Query.of(Service.FQN).withFilters(Filters.eq(Service.BRAND, Brands.BERU_SMM)))
                .forEach(dbService::delete);
    }

    private static class Context {
        private AssessmentRule ruleToAssign;
        private AssessmentRule ruleToNotAssign;
    }

}
