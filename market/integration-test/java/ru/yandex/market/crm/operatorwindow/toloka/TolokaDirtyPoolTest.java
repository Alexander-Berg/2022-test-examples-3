package ru.yandex.market.crm.operatorwindow.toloka;


import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.toloka.AssessmentPool;
import ru.yandex.market.jmf.module.toloka.AssessmentRule;
import ru.yandex.market.jmf.module.toloka.Service;
import ru.yandex.market.jmf.module.toloka.Ticket;
import ru.yandex.market.jmf.module.toloka.TolokaExchanger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.CREATED_POOL_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.NOT_EMPTY_TASKS_LIST;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_OPERATION_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_PENDING_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_SUCCESS_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.REFERENCE_POOL_ID;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.TASKS_CREATE_SUCCESS_RESPONSE;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.TOLOKA_TASKS;

/**
 * При публикации заданий в пул, проверяем, что пул пуст на стороне Толоки/Янга. Если в пуле уже есть задания, то
 * прерываем публикацию, расформировываем пул на нашей стороне (высвобождаем обращения для подбора в другой пул) и
 * закрываем пул в Толоке
 */
public class TolokaDirtyPoolTest extends TolokaAbstractTest {

    @Inject
    private TolokaExchanger tolokaExchanger;

    @BeforeEach
    public void setUp() {
        when(tolokaClient.clonePool(REFERENCE_POOL_ID)).thenReturn(POOL_CLONE_PENDING_OPERATION);
        when(tolokaClient.getOperation(POOL_CLONE_OPERATION_ID)).thenReturn(POOL_CLONE_SUCCESS_OPERATION);
        when(tolokaClient.getTasks(eq(CREATED_POOL_ID), anyInt())).thenReturn(NOT_EMPTY_TASKS_LIST);
        when(tolokaClient.createTasks(anyList())).thenReturn(TASKS_CREATE_SUCCESS_RESPONSE);
    }

    @Test
    @Transactional
    public void publicationInDirtyPool() {

        triggerService.withSyncTriggersMode(() -> {
            setTolokaExchangeEnabled(true);

            var assessmentRule = generateAssessmentRules(1)[0];
            bcpService.edit(assessmentRule, AssessmentRule.POOL_ID, REFERENCE_POOL_ID);

            var service = generateServices(1)[0];
            bcpService.edit(service, Service.ASSESSMENT_RULE, assessmentRule);

            for (int i = 0; i < TOLOKA_TASKS.size(); i++) {
                ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
            }

            tolokaExchanger.publishTasks(assessmentRule);

            var poolsAll = entityStorage.<AssessmentPool>list(Query.of(AssessmentPool.FQN));
            assertEquals(1, poolsAll.size());

            var pool = poolsAll.get(0);
            assertNotNull(pool.getAssessmentRule(), "Created pool must have a rule");
            assertEquals(pool.getAssessmentRule(), assessmentRule, "Created pool has wrong rule");

            assertEquals(AssessmentPool.STATUS_CREATION_FAILED, pool.getStatus());

            var ticketsAll = entityStorage.<Ticket>list(Query.of(Ticket.FQN));
            var ticketsWithPool = CrmCollections.filter(ticketsAll, ticket -> ticket.getAssessmentPool() != null);
            var ticketsWithTask = CrmCollections.filter(ticketsAll, ticket -> ticket.getAssessmentTaskId() != null);

            assertEquals(ticketsAll.size(), TOLOKA_TASKS.size());
            assertEquals(ticketsWithPool.size(), 0);
            assertEquals(ticketsWithTask.size(), 0);

            assertTrue(ticketsAll.stream().map(Ticket::getStatus).allMatch(Ticket.STATUS_ASSESSMENT_REQUIRED::equals));
            assertTrue(ticketsAll.stream().map(Ticket::getAssessmentTaskId).allMatch(Objects::isNull));

            verify(tolokaClient, atLeastOnce()).closePool(CREATED_POOL_ID);
        });
    }
}
