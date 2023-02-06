package ru.yandex.market.crm.operatorwindow.toloka;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.toloka.AssessmentConfiguration;
import ru.yandex.market.jmf.module.toloka.AssessmentRule;
import ru.yandex.market.jmf.module.toloka.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.crm.operatorwindow.toloka.TolokaTestConstants.NO_ASSESSMENT_TICKET_FQN;
import static ru.yandex.market.jmf.module.toloka.Service.ASSESSMENT_RULE;

/**
 * Регистрация нового обращения в статусе {@link Ticket#STATUS_ASSESSMENT_REQUIRED assessmentRequired}<p/>
 *
 * Через статусную модель Толоки проходят только обращения с подключенной логикой {@code assessmentTicket}<p/>
 *
 * Если {@link AssessmentConfiguration#TOLOKA_EXCHANGE_ENABLED галка конфигурации} обмена с Толокой
 * <ul>
 *   <li> <b>включена</b> и {@link Service очереди} создаваемого тикета назначено {@link AssessmentRule правило}
 *     ассессмента, то обращение должно получить статус {@link Ticket#STATUS_ASSESSMENT_REQUIRED assessmentRequired}
 *   </li>
 *   <li> <b>включена</b> и {@link Service очереди} создаваемого тикета <b>НЕ</b> назначено
 *     {@link AssessmentRule правило} ассессмента, то обращение <b>НЕ</b> должно получить статус
 *     {@link Ticket#STATUS_ASSESSMENT_REQUIRED assessmentRequired}
 *   </li>
 *   <li> <b>выключена</b> и {@link Service очереди} создаваемого тикета назначено {@link AssessmentRule правило}
 *     ассессмента, то обращение <b>НЕ</b> должно получить статус {@link Ticket#STATUS_ASSESSMENT_REQUIRED
 *     assessmentRequired}
 *   </li>
 *   <li> <b>выключена</b> и {@link Service очереди} создаваемого тикета <b>НЕ</b> назначено
 *     {@link AssessmentRule правило} ассессмента, то обращение <b>НЕ</b> должно получить статус
 *     {@link Ticket#STATUS_ASSESSMENT_REQUIRED assessmentRequired}
 *   </li>
 * </ul>
 */
public class TolokaTicketCreationTest extends TolokaAbstractTest {

    @Transactional
    @ParameterizedTest(name = "Exchange enabled = {0}")
    @ValueSource(booleans = {true, false})
    public void tolokaAssessmentRequiredStatusTest(boolean isExchangeEnabled) {
        triggerService.withSyncTriggersMode(() -> {
            setTolokaExchangeEnabled(isExchangeEnabled);

            var rules = generateAssessmentRules(3);
            var services = generateServices(3);
            var serviceWithRule = services[0]; // Только одна очередь будет иметь правило ассессмента

            bcpService.edit(serviceWithRule, ASSESSMENT_RULE, rules[0]);

            // В каждой из созданных очередей создаем два тикета, один с логикой ассессмента, другой без
            // При включенном обмене с Толокой только одно обращение получит статус assessmentRequired
            for (Service service : services) {
                ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
                ticketTestUtils.createTicket(NO_ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
            }

            var tickets = entityStorage.list(Query.of(ru.yandex.market.jmf.module.ticket.Ticket.FQN));
            assertEquals(6, tickets.size(), "Incorrect ticket total quantity");

            var assessmentRequiredTickets = tickets.stream()
                    .filter(entity -> Ticket.STATUS_ASSESSMENT_REQUIRED.equals(entity.getAttribute(Ticket.STATUS)))
                    .collect(Collectors.toList());
            assertEquals(isExchangeEnabled ? 1 : 0, assessmentRequiredTickets.size(),
                    "Incorrect quantity of assessment required tickets ");

            if (isExchangeEnabled) {
                assertEquals(serviceWithRule, assessmentRequiredTickets.get(0).getAttribute(Ticket.SERVICE),
                        "Assessment required ticket has wrong service");
            }
        });
    }
}
