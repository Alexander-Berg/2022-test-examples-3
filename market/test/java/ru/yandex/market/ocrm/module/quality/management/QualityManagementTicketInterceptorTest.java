package ru.yandex.market.ocrm.module.quality.management;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.TestChannels;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.AuthRunnerService;
import ru.yandex.market.jmf.security.SecurityDataService;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.ocrm.module.quality.management.domain.QualityManagementTicket;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ModuleQualityManagementTestConfiguration.class)
public class QualityManagementTicketInterceptorTest {

    private static final Fqn TEST_FQN = Fqn.of("ticket$testQM");

    @Inject
    private AuthRunnerService authRunnerService;
    @Inject
    private DbService dbService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private TxService txService;
    @Inject
    private SecurityDataService securityDataService;

    @AfterEach
    public void reset() throws Exception {
        ((MockAuthRunnerService) authRunnerService).reset();
    }

    /**
     * В тесте проверяется сценарий:
     * Есть два сотрудника, у одного из них есть тикет КК
     * Если первый из них хочет получить список тикетов с результатами КК, то ему вернётся список с одним тикетом КК
     * Если второй из них хочет получить список тикетов с результатами КК, то ему вернётся пустой список
     * Хотим, чтобы у операторов была возможность видеть тикеты КК, в которых оценили их работу независимо от того,
     * просматривают ли они очередь КК
     */
    @Test
    @Transactional
    public void qualityManagementResult() {
        var ou = ticketTestUtils.createOu();
        var team = ticketTestUtils.createTeam();
        var service = ticketTestUtils.createService24x7(team);
        var channel = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);

        var anotherEmployee = ticketTestUtils.createEmployee(ou);
        var evaluatedEmployee = ticketTestUtils.createEmployee(ou);

        var evaluatedTicket = ticketTestUtils.createTicket(TEST_FQN, Map.of(
                Ticket.CHANNEL, channel,
                Ticket.RESPONSIBLE_EMPLOYEE, evaluatedEmployee,
                Ticket.RESPONSIBLE_OU, ou,
                Ticket.RESPONSIBLE_TEAM, team,
                Ticket.SERVICE, service,
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ));
        var qualityManagementTicket = ticketTestUtils.createTicket(
                QualityManagementTicket.FQN,
                Map.of(
                        QualityManagementTicket.EVALUATED_EMPLOYEE, evaluatedEmployee,
                        QualityManagementTicket.EVALUATED_TICKET, evaluatedTicket,
                        QualityManagementTicket.ITERATION_START, OffsetDateTime.now(),
                        QualityManagementTicket.ITERATION_END, OffsetDateTime.now(),
                        QualityManagementTicket.RESOLUTION, QualityManagementTicket.RESOLUTION_PUBLISHED
                )
        );

        var query = Query.of(QualityManagementTicket.FQN)
                .withFilters(Filters.eq(QualityManagementTicket.EVALUATED_EMPLOYEE, evaluatedEmployee));

        ((MockAuthRunnerService) authRunnerService).setCurrentUserSuperUser(false);
        ((MockSecurityDataService) securityDataService).setInitialEmployee(evaluatedEmployee);

        var result = dbService.<QualityManagementTicket>list(query);

        Assertions.assertEquals(List.of(qualityManagementTicket), result);

        ((MockSecurityDataService) securityDataService).setInitialEmployee(anotherEmployee);

        result = dbService.list(query);

        Assertions.assertTrue(result.isEmpty());
    }

    /**
     * В тесте проверяется сценарий:
     * Есть два отдела, у каждого из них свой супервизор, за каждым из отделов есть по тикету КК
     * Проверяем, что каждый из супервизоров видит только те тикеты КК, для которых он является супервизором
     * Хотим, чтобы у супервзиоров была возможность видеть тикеты КК, в которых оценили работу их операторов независимо
     * от того, просматривают ли они очередь КК
     */
    @Test
    @Transactional
    public void qualityManagementResultForSupervisor() {
        var parentOu = ticketTestUtils.createOu();
        var supervisor1 = ticketTestUtils.createEmployee(parentOu);
        var supervisor2 = ticketTestUtils.createEmployee(parentOu);
        var ou1 = ticketTestUtils.createSubOu(parentOu, supervisor1);
        var ou2 = ticketTestUtils.createSubOu(parentOu, supervisor2);
        var employee1 = ticketTestUtils.createEmployee(ou1);
        var employee2 = ticketTestUtils.createEmployee(ou2);

        var channel = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);
        var team = ticketTestUtils.createTeam();
        var service = ticketTestUtils.createService24x7(team);

        var ticket1 = ticketTestUtils.createTicket(TEST_FQN, Map.of(
                Ticket.CHANNEL, channel,
                Ticket.RESPONSIBLE_EMPLOYEE, employee1,
                Ticket.RESPONSIBLE_OU, ou1,
                Ticket.RESPONSIBLE_TEAM, team,
                Ticket.SERVICE, service,
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ));
        var ticket2 = ticketTestUtils.createTicket(TEST_FQN, Map.of(
                Ticket.CHANNEL, channel,
                Ticket.RESPONSIBLE_EMPLOYEE, employee2,
                Ticket.RESPONSIBLE_OU, ou2,
                Ticket.RESPONSIBLE_TEAM, team,
                Ticket.SERVICE, service,
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ));
        var qualityManagementTicket1 = ticketTestUtils.createTicket(
                QualityManagementTicket.FQN,
                Map.of(
                        QualityManagementTicket.EVALUATED_EMPLOYEE, employee1,
                        QualityManagementTicket.EVALUATED_TICKET, ticket1,
                        QualityManagementTicket.ITERATION_START, OffsetDateTime.now(),
                        QualityManagementTicket.ITERATION_END, OffsetDateTime.now(),
                        QualityManagementTicket.RESOLUTION, QualityManagementTicket.RESOLUTION_PUBLISHED
                )
        );
        var qualityManagementTicket2 = ticketTestUtils.createTicket(
                QualityManagementTicket.FQN,
                Map.of(
                        QualityManagementTicket.EVALUATED_EMPLOYEE, employee2,
                        QualityManagementTicket.EVALUATED_TICKET, ticket2,
                        QualityManagementTicket.ITERATION_START, OffsetDateTime.now(),
                        QualityManagementTicket.ITERATION_END, OffsetDateTime.now(),
                        QualityManagementTicket.RESOLUTION, QualityManagementTicket.RESOLUTION_PUBLISHED
                )
        );

        var query = Query.of(QualityManagementTicket.FQN);

        ((MockAuthRunnerService) authRunnerService).setCurrentUserSuperUser(false);
        ((MockSecurityDataService) securityDataService).setInitialEmployee(supervisor1);

        var result = dbService.<QualityManagementTicket>list(query);

        Assertions.assertEquals(List.of(qualityManagementTicket1), result);

        ((MockSecurityDataService) securityDataService).setInitialEmployee(supervisor2);

        result = dbService.list(query);

        Assertions.assertEquals(List.of(qualityManagementTicket2), result);
    }
}
