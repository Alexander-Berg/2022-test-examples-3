package ru.yandex.market.crm.operatorwindow.toloka;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.toloka.AssessmentPool;
import ru.yandex.market.jmf.module.toloka.AssessmentRule;
import ru.yandex.market.jmf.module.toloka.Service;
import ru.yandex.market.jmf.module.toloka.Ticket;
import ru.yandex.market.jmf.module.toloka.TolokaExchanger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.CREATED_POOL_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.EMPTY_TASKS_LIST;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.NO_ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_FAIL_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_OPERATION_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_PENDING_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_SUCCESS_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_OPEN_FAIL_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_OPEN_OPERATION_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_OPEN_PENDING_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_OPEN_SUCCESS_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.REFERENCE_POOL_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.TASKS_CREATE_EMPTY_RESPONSE;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.TASKS_CREATE_SUCCESS_RESPONSE;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.TOLOKA_TASKS;

/**
 * Проверка различных сценариев публикации обращений в толоку. <b>Важно!</b> Проверяется только
 * вызов методов клиента Толоки. Работоспособность конечного API Толоки не может быть проверена<p/>
 * <p>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1323 (Частично - см. описание выше)<br/>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1326<br/>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1327<br/>
 */
public class TolokaPublicationTest extends TolokaAbstractTest {

    @Inject
    private TolokaExchanger tolokaExchanger;

    private enum Scenario {
        POSITIVE, // все ок
        CLONE_OPERATION_FAILED, // Запрос создания успешен, но при получении статуса операции сбой Толоки
        CLONE_OPERATION_ERROR, // Успешный запрос создания пула, но развал при получении статуса операции
        PUSH_TASKS_ERROR, // Развал при публикации заданий в пул
        PUSH_TASKS_EMPTY_RESPONSE, // Толока просто вернула пустой список, и мы не можем замапить таски
        OPENING_FAILED_AS_RESPONSE, // Ответ от Толоки о невозможности открыть пул
        OPENING_REQUEST_ERROR, // Развал при запросе открытия пула
        OPENING_OPERATION_FAILED, // Успешный запрос открытия, но операция неуспешна у Толоки
        OPENING_OPERATION_ERROR // Развал при запросе состояния операции открытия
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

    @Transactional
    @ParameterizedTest(name = "Scenario = {0}")
    @EnumSource(Scenario.class)
    public void tolokaPublicationTest(Scenario scenario) {

        switch (scenario) {
            case CLONE_OPERATION_FAILED -> // Запрос создания успешен, но при получении статуса операции сбой Толоки
                    when(tolokaClient.getOperation(POOL_CLONE_OPERATION_ID)).thenReturn(POOL_CLONE_FAIL_OPERATION);
            case CLONE_OPERATION_ERROR -> // Успешный запрос создания пула, но развал при получении статуса операции
                    when(tolokaClient.getOperation(POOL_CLONE_OPERATION_ID)).thenThrow(new RuntimeException());
            case PUSH_TASKS_ERROR -> // Развал при публикации заданий в пул
                    when(tolokaClient.createTasks(anyList())).thenThrow(new RuntimeException());
            case PUSH_TASKS_EMPTY_RESPONSE -> // Толока просто вернула пустой список, и мы не можем замапить таски
                    when(tolokaClient.createTasks(anyList())).thenReturn(TASKS_CREATE_EMPTY_RESPONSE);
            case OPENING_FAILED_AS_RESPONSE -> // Ответ от Толоки о невозможности открыть пул
                    when(tolokaClient.openPool(CREATED_POOL_ID)).thenReturn(POOL_OPEN_FAIL_OPERATION);
            case OPENING_REQUEST_ERROR -> // Развал при запросе открытия пула
                    when(tolokaClient.openPool(CREATED_POOL_ID)).thenThrow(new RuntimeException());
            case OPENING_OPERATION_FAILED -> // Успешный запрос открытия, но операция неуспешна у Толоки
                    when(tolokaClient.getOperation(POOL_OPEN_OPERATION_ID)).thenReturn(POOL_OPEN_FAIL_OPERATION);
            case OPENING_OPERATION_ERROR -> // Развал при запросе состояния операции открытия
                    when(tolokaClient.getOperation(POOL_OPEN_OPERATION_ID)).thenThrow(new RuntimeException());
        }

        triggerService.withSyncTriggersMode(() -> {
            setTolokaExchangeEnabled(true);

            AssessmentRule[] assessmentRules = generateAssessmentRules(3);
            var ruleToAssign = assessmentRules[1]; // три правила создали - одно используем
            bcpService.edit(ruleToAssign, AssessmentRule.POOL_ID, REFERENCE_POOL_ID);

            var services = generateServices(3);
            var serviceToAssign = services[1];
            bcpService.edit(serviceToAssign, Service.ASSESSMENT_RULE, ruleToAssign); // одной очереди назначили правило

            var totalTicketCount = (TOLOKA_TASKS.size() - 1) * services.length * 2 + 2;
            var assessmentTicketCount = TOLOKA_TASKS.size();

            Set<Ticket> expectedTickets = new HashSet<>();
            for (var service : services) {
                for (int i = 0; i < TOLOKA_TASKS.size() - 1; i++) {
                    Ticket ticket = ticketTestUtils.createTicket(
                            ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                    if (Objects.equals(service, serviceToAssign)) {
                        expectedTickets.add(ticket);
                    }
                    ticketTestUtils.createTicket(NO_ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                }
            }
            // очередь подходит, но в обращении указано другое правило ассессмента, что более приоритетно
            ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(
                    Ticket.SERVICE, serviceToAssign,
                    Ticket.ASSESSMENT_RULE, assessmentRules[0],
                    Ticket.STATUS, Ticket.STATUS_ASSESSMENT_REQUIRED
            ));
            // очередь не подходит, но в обращении указано наше правило ассессмента, что более приоритетно
            expectedTickets.add(ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(
                    Ticket.SERVICE, services[0],
                    Ticket.ASSESSMENT_RULE, ruleToAssign,
                    Ticket.STATUS, Ticket.STATUS_ASSESSMENT_REQUIRED
            )));

            tolokaExchanger.publishTasks(ruleToAssign);

            var poolsAll = entityStorage.<AssessmentPool>list(Query.of(AssessmentPool.FQN));
            assertEquals(1, poolsAll.size());

            var ticketsAll = entityStorage.<Ticket>list(Query.of(Ticket.FQN));
            var ticketsWithPool = CrmCollections.filter(ticketsAll, ticket -> ticket.getAssessmentPool() != null);
            var ticketsWithTask = CrmCollections.filter(ticketsAll, ticket -> ticket.getAssessmentTaskId() != null);
            assertEquals(totalTicketCount, ticketsAll.size());

            var pool = poolsAll.get(0);
            assertNotNull(pool.getAssessmentRule(), "Created pool must have a rule");
            assertEquals(pool.getAssessmentRule(), ruleToAssign, "Created pool has wrong rule");

            Runnable rollbackStateCheck = () -> { // контроль стейта после негативных сценариев
                assertNull(pool.getClosingTime(),
                        "При ошибках, таймер принудительного закрытия пула должен быть сброшен");
                assertEquals(0, ticketsWithPool.size(),
                        "При ошибках, обращениям должен быть очищен пул, для возможности подбора в другой пул");
                assertEquals(0, ticketsWithTask.size(),
                        "При ошибках, обращениям сбрасывается маппинг на таску Толоки для подбора в другой пул");
                assertEquals(0, ticketsAll.stream().map(Ticket::getStatus)
                                .filter(Ticket.STATUS_ASSESSMENT::equals).count(),
                        "При ошибках, обращения не должны зависнуть в статусе assessment");
                assertEquals(assessmentTicketCount, expectedTickets.stream().map(Ticket::getStatus)
                                .filter(Ticket.STATUS_ASSESSMENT_REQUIRED::equals).count(),
                        "При ошибках, все обращения должны откатиться к статусу assessmentRequired");
            };

            switch (scenario) {
                case POSITIVE -> {
                    // проверяем стейт
                    assertEquals(AssessmentPool.STATUS_OPENED, pool.getStatus());
                    assertEquals(ruleToAssign.getAssessmentTime(), pool.getClosingTime());
                    assertEquals(assessmentTicketCount, ticketsWithPool.size());
                    assertIterableEquals(ticketsWithTask, ticketsWithPool,
                            "Assessment pool and assessments task ids must be assigned to the same tickets");
                    assertEquals(expectedTickets, Set.copyOf(ticketsWithPool));
                    assertTrue(
                            ticketsWithPool.stream().map(Ticket::getStatus).allMatch(Ticket.STATUS_ASSESSMENT::equals),
                            "Wrong ticket status");
                    // проверяем обращения к клиенту
                    verify(tolokaClient, times(1)).clonePool(REFERENCE_POOL_ID);
                    verify(tolokaClient, times(1)).getOperation(POOL_CLONE_OPERATION_ID);
                    verify(tolokaClient, times(1)).createTasks(anyList());
                    verify(tolokaClient, times(1)).openPool(CREATED_POOL_ID);
                    verify(tolokaClient, times(1)).getOperation(POOL_OPEN_OPERATION_ID);
                    verify(tolokaClient, never()).closePool(CREATED_POOL_ID);
                }
                case CLONE_OPERATION_FAILED, CLONE_OPERATION_ERROR -> {
                    assertEquals(AssessmentPool.STATUS_CREATION_FAILED, pool.getStatus());
                    verify(tolokaClient, never()).closePool(CREATED_POOL_ID);
                    rollbackStateCheck.run();
                }
                case PUSH_TASKS_ERROR, PUSH_TASKS_EMPTY_RESPONSE -> {
                    assertEquals(AssessmentPool.STATUS_CREATION_FAILED, pool.getStatus());
                    verify(tolokaClient, times(1)).closePool(CREATED_POOL_ID);
                    rollbackStateCheck.run();
                }
                case OPENING_FAILED_AS_RESPONSE, OPENING_REQUEST_ERROR, OPENING_OPERATION_FAILED,
                        OPENING_OPERATION_ERROR -> {
                    assertEquals(AssessmentPool.STATUS_OPENING_FAILED, pool.getStatus());
                    verify(tolokaClient, times(1)).closePool(CREATED_POOL_ID);
                    rollbackStateCheck.run();
                }
            }
        });
    }
}
