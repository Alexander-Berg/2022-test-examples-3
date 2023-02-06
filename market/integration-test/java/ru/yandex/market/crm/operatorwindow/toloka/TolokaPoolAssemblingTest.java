package ru.yandex.market.crm.operatorwindow.toloka;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.toloka.AssessmentPool;
import ru.yandex.market.jmf.module.toloka.AssessmentRule;
import ru.yandex.market.jmf.module.toloka.Ticket;
import ru.yandex.market.jmf.module.toloka.TolokaExchanger;
import ru.yandex.market.jmf.module.toloka.model.TolokaAsyncOperationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.NO_ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_FAIL_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.POOL_CLONE_PENDING_OPERATION;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.REFERENCE_POOL_ID;
import static ru.yandex.market.jmf.module.toloka.Service.ASSESSMENT_RULE;

/**
 * Перед публикацией обращений находящихся в статусе {@link Ticket#STATUS_ASSESSMENT_REQUIRED assessmentRequired}
 * в виде задач ассессорам, необходимо запросить пул под них на стороне Толоки<p/>
 * <p>
 * Для этого на нашей стороне создается {@link AssessmentPool сущность-проекция} будущего пула и устанавливается
 * как свойство {@link Ticket#ASSESSMENT_POOL assessmentPool} этих обращений. Далее эта проекция служит местом
 * накопления всех необходимых сведений об оригинальном пуле и агрегирующим свойством обращений<p/>
 * <p>
 * При запросе пула на стороне Толоки, вероятны два негативных кейса - невозможность создания пула на стороне Толоки,
 * тогда мы получим нормальный ответ от апи, в котором будет статус {@link TolokaAsyncOperationStatus#FAIL}, либо
 * любая непредсказуемая ошибка (десериализации, 500-ка от апи и т.п.). В любом негативном кейсе мы должны
 * расформировать наш пул обращений (очистить или не заполнить поле {@link Ticket#ASSESSMENT_POOL assessmentPool}),
 * чтобы эти обращения могли быть подобраны в новый пул при следующей попытке, а не зависли
 * <p>
 * https://testpalm.yandex-team.ru/testcase/ocrm-1603
 */
public class TolokaPoolAssemblingTest extends TolokaAbstractTest {

    @Inject
    private TolokaExchanger tolokaExchanger;

    @Transactional
    @ParameterizedTest(name = "Scenario = {0}")
    @EnumSource(Scenario.class)
    public void tolokaPoolAssemblingTest(Scenario scenario) {

        switch (scenario) {
            case POSITIVE, POSITIVE_WITH_LIMIT -> // Ответ об успешном создании пула на стороне Толоки
                    when(tolokaClient.clonePool(REFERENCE_POOL_ID)).thenReturn(POOL_CLONE_PENDING_OPERATION);
            case FAIL_AS_CLONE_RESPONSE -> // Ответ от Толоки о невозможности создать пул
                    when(tolokaClient.clonePool(REFERENCE_POOL_ID)).thenReturn(POOL_CLONE_FAIL_OPERATION);
            case CLONE_REQUEST_ERROR -> // Просто развал при попытке запросить пул
                    when(tolokaClient.clonePool(REFERENCE_POOL_ID)).thenThrow(new RuntimeException());
        }

        triggerService.withSyncTriggersMode(() -> {
            setTolokaExchangeEnabled(true);

            var rules = generateAssessmentRules(2);
            var services = generateServices(3);

            var ruleToAssign = rules[0];
            bcpService.edit(ruleToAssign, AssessmentRule.POOL_ID, REFERENCE_POOL_ID);

            if (scenario == Scenario.POSITIVE_WITH_LIMIT) {
                bcpService.edit(ruleToAssign, AssessmentRule.POOL_SIZE_LIMIT, 1);
            }

            var serviceWithRule = services[0];
            bcpService.edit(serviceWithRule, ASSESSMENT_RULE, ruleToAssign);

            for (Service service : services) {
                // В каждой очереди создаем по 6 тикетов - 3 с логикой ассессмента и 3 без
                for (int i = 0; i < 3; i++) {
                    ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                    ticketTestUtils.createTicket(NO_ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                }
            }
            // Запускаем публикацию без триггеров, чтобы проверить промежуточное состояние
            triggerService.withAsyncTriggersMode(() -> tolokaExchanger.publishTasks(ruleToAssign));

            List<Ticket> ticketsAll = entityStorage.list(Query.of(Ticket.FQN));
            List<Ticket> ticketsWithPool = ticketsAll.stream()
                    .filter(ticket -> ticket.getAssessmentPool() != null)
                    .collect(Collectors.toList());

            List<AssessmentPool> poolsAll = entityStorage.list(Query.of(AssessmentPool.FQN));
            Set<AssessmentPool> poolsFromTickets = ticketsWithPool.stream()
                    .map(Ticket::getAssessmentPool)
                    .collect(Collectors.toSet());

            verify(tolokaClient, times(1)).clonePool(REFERENCE_POOL_ID);
            verifyNoMoreInteractions(tolokaClient);

            // В любом сценарии у нас 18 тикетов
            assertEquals(18, ticketsAll.size(), "Incorrect total ticket qty");
            // и три из них в assessmentRequired статусе
            assertEquals(3, ticketsAll.stream().map(Ticket::getStatus)
                    .filter(Ticket.STATUS_ASSESSMENT_REQUIRED::equals)
                    .count(), "Wrong assessmentRequired tickets qty");

            switch (scenario) {
                case POSITIVE -> {
                    assertEquals(3, ticketsWithPool.size());
                    assertEquals(1, poolsFromTickets.size());
                    assertEquals(1, poolsAll.size());

                    AssessmentPool pool = poolsAll.get(0);
                    assertEquals(pool, poolsFromTickets.iterator().next());
                    assertEquals(AssessmentPool.STATUS_REQUESTED, pool.getStatus());
                    assertEquals(pool.getAssessmentRule(), ruleToAssign);
                }
                case POSITIVE_WITH_LIMIT -> {
                    assertEquals(1, ticketsWithPool.size());
                    assertEquals(1, poolsFromTickets.size());
                    assertEquals(1, poolsAll.size());

                    AssessmentPool pool = poolsAll.get(0);
                    assertEquals(pool, poolsFromTickets.iterator().next());
                    assertEquals(AssessmentPool.STATUS_REQUESTED, pool.getStatus());
                    assertEquals(pool.getAssessmentRule(), ruleToAssign);
                }
                case FAIL_AS_CLONE_RESPONSE, CLONE_REQUEST_ERROR -> {
                    assertEquals(0, ticketsWithPool.size());
                    assertEquals(0, poolsFromTickets.size());
                    assertEquals(0, poolsAll.size());
                }
            }
        });
    }

    private enum Scenario {
        POSITIVE, POSITIVE_WITH_LIMIT, CLONE_REQUEST_ERROR, FAIL_AS_CLONE_RESPONSE
    }

}
