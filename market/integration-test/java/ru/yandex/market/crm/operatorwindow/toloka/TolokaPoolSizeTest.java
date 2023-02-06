package ru.yandex.market.crm.operatorwindow.toloka;

import java.time.Duration;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.toloka.AssessmentPool;
import ru.yandex.market.jmf.module.toloka.AssessmentRule;
import ru.yandex.market.jmf.module.toloka.Service;
import ru.yandex.market.jmf.module.toloka.Ticket;
import ru.yandex.market.jmf.module.toloka.TolokaExchanger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSESSMENT_TICKET_FQN;
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

@Transactional
public class TolokaPoolSizeTest extends TolokaAbstractTest {

    @Inject
    private TolokaExchanger tolokaExchanger;

    @BeforeEach
    public void setUp() {
        when(tolokaClient.clonePool(REFERENCE_POOL_ID)).thenReturn(POOL_CLONE_PENDING_OPERATION);
        when(tolokaClient.getOperation(POOL_CLONE_OPERATION_ID)).thenReturn(POOL_CLONE_SUCCESS_OPERATION);
        when(tolokaClient.getTasks(eq(CREATED_POOL_ID), anyInt())).thenReturn(EMPTY_TASKS_LIST);
        when(tolokaClient.createTasks(anyList())).thenReturn(TASKS_CREATE_SUCCESS_RESPONSE);
        when(tolokaClient.openPool(CREATED_POOL_ID)).thenReturn(POOL_OPEN_PENDING_OPERATION);
        when(tolokaClient.getOperation(POOL_OPEN_OPERATION_ID)).thenReturn(POOL_OPEN_SUCCESS_OPERATION);
    }

    @Test
    public void testFormedPoolMinSize() {
        triggerService.withSyncTriggersMode(() -> {
            setTolokaExchangeEnabled(true);

            var ruleToAssign = generateAssessmentRules(1)[0];
            int formedPoolMinSize = TOLOKA_TASKS.size();
            bcpService.edit(ruleToAssign, Map.of(
                    AssessmentRule.POOL_ID, REFERENCE_POOL_ID,
                    AssessmentRule.FORMED_POOL_MIN_SIZE, formedPoolMinSize
            ));

            var services = generateServices(3);
            bcpService.edit(services[1], Service.ASSESSMENT_RULE, ruleToAssign); // очереди назначили правило

            for (var service : services) { // наполняем очереди тикетами
                for (int i = 0; i < formedPoolMinSize - 1; i++) {
                    ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                    ticketTestUtils.createTicket(NO_ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                }
            }
            tolokaExchanger.publishTasks(ruleToAssign);

            var poolsAll = entityStorage.<AssessmentPool>list(Query.of(AssessmentPool.FQN));
            assertEquals(0, poolsAll.size());

            ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, services[1]));

            tolokaExchanger.publishTasks(ruleToAssign);
            poolsAll = entityStorage.list(Query.of(AssessmentPool.FQN));
            assertEquals(1, poolsAll.size());
            var pool = poolsAll.get(0);
            assertEquals(AssessmentPool.STATUS_OPENED, pool.getStatus());
        });
    }

    @Test
    public void testWaitingFormedPoolMaxPeriod() {
        triggerService.withSyncTriggersMode(() -> {
            setTolokaExchangeEnabled(true);

            var ruleToAssign = generateAssessmentRules(1)[0];
            int formedPoolMinSize = TOLOKA_TASKS.size() + 1;
            bcpService.edit(ruleToAssign, Map.of(
                    AssessmentRule.POOL_ID, REFERENCE_POOL_ID,
                    AssessmentRule.FORMED_POOL_MIN_SIZE, formedPoolMinSize,
                    AssessmentRule.WAITING_FORMED_POOL_MAX_PERIOD, Duration.ofSeconds(3)
            ));

            var services = generateServices(3);
            bcpService.edit(services[1], Service.ASSESSMENT_RULE, ruleToAssign); // очереди назначили правило

            for (var service : services) { // наполняем очереди тикетами
                for (int i = 0; i < formedPoolMinSize - 1; i++) {
                    ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                    ticketTestUtils.createTicket(NO_ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                }
            }
            tolokaExchanger.publishTasks(ruleToAssign);

            var poolsAll = entityStorage.<AssessmentPool>list(Query.of(AssessmentPool.FQN));
            assertEquals(0, poolsAll.size());

            Thread.sleep(3000);
            tolokaExchanger.publishTasks(ruleToAssign);

            poolsAll = entityStorage.list(Query.of(AssessmentPool.FQN));
            assertEquals(1, poolsAll.size());
            var pool = poolsAll.get(0);
            assertEquals(AssessmentPool.STATUS_OPENED, pool.getStatus());
        });
    }
}
