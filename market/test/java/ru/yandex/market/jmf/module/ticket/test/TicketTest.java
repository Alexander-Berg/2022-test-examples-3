package ru.yandex.market.jmf.module.ticket.test;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.notification.Notification;
import ru.yandex.market.jmf.module.notification.NotificationsService;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.EmployeeChannelService;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.NeedsHelpAlert;
import ru.yandex.market.jmf.module.ticket.NeedsHelpService;
import ru.yandex.market.jmf.module.ticket.OmniChannelSettingsService;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.operations.DistributeTicketOperationHandler;
import ru.yandex.market.jmf.module.ticket.operations.distribution.FindTicketEmployeeDistributionStatusOperationHandler;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.impl.TimerTriggerHandler;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.jmf.utils.Maps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.jmf.module.ticket.operations.distribution.InitProcessEmployeeDistributionStatusOperationHandler.DISABLE_PLAY_MODE_IF_PROCESSING_TICKET_EXISTS;

@javax.transaction.Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class TicketTest {

    @Inject
    private BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private DistributionService distributionService;
    @Inject
    private DbService dbService;
    @Inject
    private TimerTriggerHandler timerTriggerHandler;
    @Inject
    private NeedsHelpService needsHelpService;
    @Inject
    private OuTestUtils ouTestUtils;
    @Inject
    private EmployeeChannelService employeeChannelService;
    @Inject
    private OmniChannelSettingsService omniChannelSettingsService;
    @Inject
    private NotificationsService ticketNotificationService;
    @Inject
    private ConfigurationService configurationService;

    private TicketTestUtils.TestContext ctx;

    @Inject
    private TimerTestUtils timerTestUtils;


    @BeforeEach
    public void setUp() {
        ctx = ticketTestUtils.create();

        var ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch1));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> List.of());
    }

    /**
     * Проверяем, что создалось базовое окружение теста. Ожидаем отсутствие ошибки
     */
    @Test
    public void createEnvironment() {
        ticketTestUtils.create();
    }

    /**
     * Проверяем переход сотрудника в play-режим для сотрудников никогда не работавших в нем
     */
    @Test
    public void distribution_doStart_checkProcessingTicket_DontExistAnyTicket() {
        var employee = ctx.employee0;
        processingTicketDataHelper(ctx, true, false, null, null);

        DistributionService.DistributionResult result = distributionService.doStart(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, result.distribution().getStatus());
    }

    @Test
    public void distribution_doStart_checkProcessingTicket_DontExistAnyTicketInProgress() {
        var employee = ctx.employee0;
        processingTicketDataHelper(ctx, true, true, employee, Ticket.STATUS_RESOLVED);

        DistributionService.DistributionResult result = distributionService.doStart(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, result.distribution().getStatus());
    }

    @Test
    public void distribution_doStart_checkProcessingTicket_ExistTicketInProgress_AnotherEmployee() {
        processingTicketDataHelper(ctx, true, true, ctx.employee1, Ticket.STATUS_PROCESSING);

        DistributionService.DistributionResult result = distributionService.doStart(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, result.distribution().getStatus());
    }

    @Test
    public void distribution_doStart_checkProcessingTicket_ExistTicketInProgress() {
        var employee = ctx.employee0;
        processingTicketDataHelper(ctx, true, true, employee, Ticket.STATUS_PROCESSING);

        DistributionService.DistributionResult result = distributionService.doStart(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_NOT_READY, result.distribution().getStatus());

        List<Notification> notifications = ticketNotificationService.getUnreadNotifications(employee, 0, 100);
        Assertions.assertEquals(1, notifications.size());
    }

    @Test
    public void distribution_doStart_dontCheckProcessingTicket_ExistTicketInProgress() {
        var employee = ctx.employee0;
        processingTicketDataHelper(ctx, false, true, employee, Ticket.STATUS_PROCESSING);

        DistributionService.DistributionResult result = distributionService.doStart(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, result.distribution().getStatus());
    }

    /**
     * Проверяем переход сотрудника из play-режима в зависимости от фичи-флага и наличия тикета в процессинге
     */
    @Test
    public void playMode_ResolvedTicket_dontCheckProcessingTicket_ExistTicketInProgress() {
        var employee = ctx.employee0;

        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        processingTicketDataHelper(ctx, false, true, employee, Ticket.STATUS_PROCESSING);

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));

        final var distribution = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution.getStatus());
    }

    @Test
    public void playMode_ResolvedTicket_checkProcessingTicket_DontExistTicketAnyTicket() {
        var employee = ctx.employee0;

        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        processingTicketDataHelper(ctx, true, false, null, null);

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));

        final var distribution = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution.getStatus());
    }

    @Test
    public void playMode_ResolvedTicket_checkProcessingTicket_DontExistTicketAnyTicketInProgress() {
        var employee = ctx.employee0;

        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        processingTicketDataHelper(ctx, true, true, employee, Ticket.STATUS_RESOLVED);

        distributionService.currentStatus(employee);

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));

        final var distribution = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution.getStatus());
    }

    @Test
    public void playMode_ResolvedTicket_checkProcessingTicket_ExistTicketInProgress() {
        var employee = ctx.employee0;

        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1)).getGid();

        processingTicketDataHelper(ctx, true, true, employee, Ticket.STATUS_PROCESSING);

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        distributionService.currentStatus(employee);
        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));
        distributionService.currentStatus(employee);

        final var distribution = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_NOT_READY, distribution.getStatus());

        List<Notification> notifications = ticketNotificationService.getUnreadNotifications(employee, 0, 100);
        Assertions.assertEquals(1, notifications.size());
    }

    //Не смотря на существование тикета в процессинге оператор продолжать работать в Play режиме, пока не закончит
    // все свои задачи
    @Test
    public void playMode_ResolvedTicket_checkProcessingTicket_ExistTicketInProgress_ButExistNextTicket() {
        var employee = ctx.employee0;

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        processingTicketDataHelper(ctx, true, true, employee, Ticket.STATUS_PROCESSING);

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));

        distributionService.currentStatus(employee);

        final var distribution = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());
    }

    /**
     * Находясь в Play, режиме не смотря на существвание тикетов в процессе, запрашивая перерыв,
     * оператор дорабатывает тикеты и переходит в статус перерыва, а не "Не готов"
     */
    @Test
    public void playMode_doBreak_checkProcessingTicket_ExistTicketInProgress() {
        var employee = ctx.employee0;

        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1)).getGid();

        processingTicketDataHelper(ctx, true, true, employee, Ticket.STATUS_PROCESSING);

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        distributionService.currentStatus(employee);
        //Оператор запрашивает перерыв
        distributionService.doBreak(ctx.employee0, EmployeeDistributionStatus.STATUS_LUNCH);
        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));
        distributionService.currentStatus(employee);

        final var distribution = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_LUNCH, distribution.getStatus());
    }

    @Test
    public void playMode_doBreak_withTwoTickets_checkProcessingTicket_ExistTicketInProgress() {
        var employee = ctx.employee0;

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        processingTicketDataHelper(ctx, true, true, employee, Ticket.STATUS_PROCESSING);
        //В плэй режиме назначается первый тикет, второй тикет в InactiveTickets
        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        distributionService.currentStatus(employee);
        //Оператор запрашивает перерыв
        distributionService.doBreak(ctx.employee0, EmployeeDistributionStatus.STATUS_LUNCH);
        //Оператор завершает работу над первым тикетом
        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));
        distributionService.currentStatus(employee);
        //Проверяем что статус не поменялся
        var distribution = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());
        //Завершаем второй тикет
        bcpService.edit(ticketGid2, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));
        distributionService.currentStatus(employee);
        //Проверяем что перешли в перерыв, а не в Не готов, не смотря на существование тикетов в процессинге.
        distribution = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_LUNCH, distribution.getStatus());
    }

    private void processingTicketDataHelper(TicketTestUtils.TestContext ctx,
                                            Boolean disablePlayModeIfProcessingTicketExists,
                                            Boolean isTicketExists,
                                            Employee employee,
                                            String TicketStatus) {
        configurationService.setValue(DISABLE_PLAY_MODE_IF_PROCESSING_TICKET_EXISTS.key(),
                disablePlayModeIfProcessingTicketExists);

        if (isTicketExists) {
            Map<String, Object> ticketAttributes = buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1);
            ticketAttributes.put(Ticket.RESPONSIBLE_EMPLOYEE, employee);
            ticketAttributes.put(Ticket.STATUS, TicketStatus);
            bcpService.create(TicketTestConstants.TICKET_TEST_FQN, ticketAttributes);
        }
    }

    @Test
    public void createTaskManual() {

        createTicket(ctx);
    }

    private Ticket createTicket(TicketTestUtils.TestContext ctx) {
        return createTicket(ctx, ctx.team0);
    }

    private Ticket createTicket(TicketTestUtils.TestContext ctx, Team team) {
        return create(buildTicketAttributes(ctx, team, TestChannels.CH1));
    }

    private Map<String, Object> buildTicketAttributes(TicketTestUtils.TestContext ctx, Team team, String channel) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("title", Randoms.string());
        attributes.put(Ticket.DESCRIPTION, Randoms.string());
        attributes.put(Ticket.SERVICE, ctx.service0);
        attributes.put(Ticket.TIME_ZONE, "Europe/Moscow");
        attributes.put(Ticket.PRIORITY, "10");
        attributes.put(Ticket.CHANNEL, channel);
        attributes.put(Ticket.RESPONSIBLE_TEAM, team);
        attributes.put(Ticket.RESOLUTION_TIME, Duration.ofHours(4));
        return attributes;
    }

    @Test
    public void responsibleOu() {
        Map<String, Object> ticketAttributes = buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1);
        ticketAttributes.put(Ticket.RESPONSIBLE_EMPLOYEE, ctx.employee0);

        Ticket ticket = bcpService.create(TicketTestConstants.TICKET_TEST_FQN, ticketAttributes);
        Ticket reloadedTicket = dbService.get(ticket.getGid());
        Assertions.assertEquals(
                ctx.employee0.getOu(), reloadedTicket.getResponsibleOu(),
                "При создании тикета, в responsibleOu должно записаться значение из responsibleEmployee.ou"
        );

        bcpService.edit(ticket, Maps.of(Ticket.RESPONSIBLE_EMPLOYEE, null));
        reloadedTicket = dbService.get(ticket.getGid());
        Assertions.assertNull(
                reloadedTicket.getResponsibleOu(),
                "При сбросе значение responsibleEmployee для тикета, в responsibleOu должно записаться null"
        );

        bcpService.edit(ticket, Maps.of(Ticket.RESPONSIBLE_EMPLOYEE, ctx.employee1));
        reloadedTicket = dbService.get(ticket.getGid());
        Assertions.assertEquals(
                ctx.employee1.getOu(), reloadedTicket.getResponsibleOu(),
                "При установке значения responsibleEmployee для тикета, в responsibleOu должно записаться " +
                        "значение из" +
                        " responsibleEmployee.ou"
        );
    }

    /**
     * Проверяем распределение тикета при его создании (есть сотрудник offline)
     */
    @Test
    public void distribute_offline() {

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);

        // вызов системы
        Ticket ticket = createTicket(ctx);

        // проверка утверждений
        Assertions.assertTrue(ticket.getWaitDistribution(), "Тикет должен ожидать распределения т.к. лн не должен " +
                "быть назначен на сотрудника");
        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_OFFLINE, employeeStatus.getStatus());
        Assertions.assertNull(employeeStatus.getTicket());
    }

    /**
     * Проверяем распределение тикета при его создании (есть сотрудник ожидающий назначения)
     */
    @Test
    public void distribute_waitTask() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // вызов системы
        Ticket ticket = createTicket(ctx);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus.getStatus());
        Assertions.assertEquals(ticket, employeeStatus.getTicket());
        Assertions.assertFalse(employeeStatus.getTicket().getWaitDistribution(),
                "Тикет не должен ожидать распределения т.к. он назначен на сотрудника");
    }

    /**
     * Проверяем, что новый тикет письменной коммуникации распределится на сотрудника ожидающего распределения,
     * который не может работать с тикетами телефонии
     * (вызов {@link DistributeTicketOperationHandler#execute})
     */
    @Test
    public void distribute_waitTicket_cantTakePhoneChannel_takeMailChannelTicket() {

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        Channel phoneChannel = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);
        employeeChannelService.markChannelUnavailable(ctx.employee0, phoneChannel);

        // вызов системы
        Ticket ticket = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2));

        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus.getStatus());
        Assertions.assertEquals(ticket, employeeStatus.getTicket());
        Assertions.assertFalse(employeeStatus.getTicket().getWaitDistribution(),
                "Тикет не должен ожидать распределения т.к. он назначен на сотрудника");
    }

    /**
     * Проверяем, что новый тикет телефонии НЕ распределится на сотрудника ожидающего распределения,
     * но который не может работать с тикетами телефонии
     * (вызов {@link DistributeTicketOperationHandler#execute})
     */
    @Test
    public void distribute_waitTicket_cantTakePhoneChannel_dontTakePhoneChannelTicket() {

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        Channel phoneChannel = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);
        employeeChannelService.markChannelUnavailable(ctx.employee0, phoneChannel);

        // вызов системы
        Ticket ticket = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1));

        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, employeeStatus.getStatus());
        Assertions.assertNull(employeeStatus.getTicket(),
                "Тикет не должен назначиться на сотрудника т.к. он не готов обрабатывать обращения телефонии");

    }

    /**
     * Проверяем, что суещствующий тикет письменной коммуникации распределится на нового сотрудника, который стал
     * ожидать
     * распределения и для которого недоступны обращения телефонии
     * (вызов {@link FindTicketEmployeeDistributionStatusOperationHandler#execute})
     */
    @Test
    public void distribute_waitEmployee_cantTakePhoneChannel_takeMailChannelTicket() {

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);

        Channel phoneChannel = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        employeeChannelService.markChannelUnavailable(ctx.employee0, phoneChannel);

        Ticket ticket = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1));

        // вызов системы
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus.getStatus());
        Assertions.assertEquals(ticket, employeeStatus.getTicket());
        Assertions.assertFalse(employeeStatus.getTicket().getWaitDistribution(),
                "Тикет должен быть распределен на сотрудника");
    }

    /**
     * Проверяем, что суещствующий тикет телефонии НЕ распределится на нового сотрудника, который стал ожидать
     * распределения и НЕ может работать с тикетами телефонии
     * (вызов {@link FindTicketEmployeeDistributionStatusOperationHandler#execute})
     */
    @Test
    public void distribute_waitEmployee_cantTakePhoneChannel_dontTakePhoneChannelTicket() {

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);

        Channel phoneChannel = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);
        employeeChannelService.markChannelUnavailable(ctx.employee0, phoneChannel);

        // создадим обращение телефонии
        create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1));

        // вызов системы
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, employeeStatus.getStatus());
        Assertions.assertNull(employeeStatus.getTicket(),
                "Тикет не должен назначиться на сотрудника т.к. он не готов обрабатывать обращения " +
                        "телефонии ");
    }

    /**
     * Проверяем, что в одной транзакции не распределится два тикета на одного оператора
     */
    @Test
    public void distribute_twoTicket_onTx() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // вызов системы
        Ticket ticket1 = bcpService.create(TicketTestConstants.TICKET_TEST_FQN, buildTicketAttributes(ctx,
                ctx.team0, TestChannels.CH1));
        Ticket ticket2 = bcpService.create(TicketTestConstants.TICKET_TEST_FQN, buildTicketAttributes(ctx,
                ctx.team0, TestChannels.CH1));

        // проверка утверждений
        Assertions.assertEquals(ctx.employee0, ticket1.getResponsibleEmployee(), "Должен распределиться первый тикет");
        Assertions.assertNull(ticket2.getResponsibleEmployee(),
                "Второй тикет не должен распределиться т.к. оператор обрабатывает первый тикет");
    }

    /**
     * Проверяем, что в разных транзакциях паралле не распределится два тикета на одного оператора
     */
    @Test
    public void distribute_twoTicket_twoTx() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // вызов системы
        Ticket ticket1 = bcpService.create(TicketTestConstants.TICKET_TEST_FQN, buildTicketAttributes(ctx,
                ctx.team0, TestChannels.CH1));
        Ticket ticket2 = createTicket(ctx);

        // проверка утверждений
        Assertions.assertEquals(ctx.employee0, ticket1.getResponsibleEmployee(),
                "Должен распределиться первый тикет");
        Assertions.assertNull(ticket2.getResponsibleEmployee(),
                "Второй тикет не должен распределиться т.к. оператор обрабатывает первый тикет");
    }

    /**
     * Проверяем распределение тикета при его создании (есть два сотрудника ожидающих назначения)
     */
    @Test
    public void distribute_waitTask_twoEmployee() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // вызов системы
        createTicket(ctx);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus0 = distributionService.getEmployeeStatus(ctx.employee0);
        EmployeeDistributionStatus employeeStatus1 = distributionService.getEmployeeStatus(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus0.getStatus(),
                "Тикет должен ораспределиться на первого сотрудника т.к. он дольше ожидает назначение задачи");
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, employeeStatus1.getStatus(),
                "Второй сотрудник должен продолжить ожидать задачу");
    }

    /**
     * Проверяем распределение 2-х тикетов при их создании (есть два сотрудника ожидающий назначения)
     */
    @Test
    public void distribute_waitTask_twoEmployee_twoTicket() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // вызов системы
        createTicket(ctx);
        createTicket(ctx);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus0 = distributionService.getEmployeeStatus(ctx.employee0);
        EmployeeDistributionStatus employeeStatus1 = distributionService.getEmployeeStatus(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus0.getStatus(),
                "Оба сотрудника должны получить по тикету");
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus1.getStatus(),
                "Оба сотрудника должны получить по тикету");
    }

    /**
     * Проверяем распределение тикета при его создании (есть сотрудник ожидающий назначения, но в другой команде)
     */
    @Test
    public void distribute_waitTask_differentTeams() {
        // настройка системы

        setEmployeeStatus(ctx.employee2, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee2, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // вызов системы
        createTicket(ctx);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus2 = distributionService.getEmployeeStatus(ctx.employee2);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, employeeStatus2.getStatus(),
                "Тикет не должен назначиться на сотрудника т.к. сотрудник из другой команды");
    }

    /**
     * Проверяем распределение тикета при его создании (есть сотрудник ожидающий назначения, но не поддерживающий
     * сервис)
     */
    @Test
    public void distribute_waitTask_differentService() {
        // настройка системы

        setEmployeeStatus(ctx.employee4, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee4, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // вызов системы
        createTicket(ctx);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus2 = distributionService.getEmployeeStatus(ctx.employee4);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, employeeStatus2.getStatus(),
                "Тикет не должен назначиться на сотрудника т.к. сотрудник не поддерживает сервис");
    }

    /**
     * Проверяем распределение тикета при изменении команды (есть сотрудник ожидающий назначения)
     */
    @Test
    public void distribute_waitTask_editTeam() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        Ticket ticket = createTicket(ctx, ctx.team1);

        // вызов системы
        bcpService.edit(ticket, ImmutableMap.of(Ticket.RESPONSIBLE_TEAM, ctx.team0));

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus0 = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus0.getStatus(),
                "Тикет должен назначиться на сотрудника после изменения команды тикета");
    }

    /**
     * Проверяем распределение тикета при его создании (есть сотрудник ожидающий взятие тикета в работу)
     */
    @Test
    public void distribute_waitTaken() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Ticket ticket0 = createTicket(ctx); // этот тикет должен распределиться на сотрудника

        // вызов системы
        Ticket ticket = createTicket(ctx);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus.getStatus());
        Assertions.assertEquals(ticket0, employeeStatus.getTicket());

        Ticket reloaded = dbService.get(ticket.getGid());
        Assertions.assertTrue(reloaded.getWaitDistribution(),
                "Тикет должен ожидать распределения т.к. он не должен распределиться на сотрудника");
    }

    /**
     * Проверяем распределение тикета при его создании (есть сотрудник у которого находится тикет в работе)
     */
    @Test
    public void distribute_processing() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Ticket ticket0 = createTicket(ctx); // этот тикет должен распределиться на сотрудника
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_PROCESSING);

        // вызов системы
        Ticket ticket = createTicket(ctx);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, employeeStatus.getStatus());
        Assertions.assertEquals(ticket0, employeeStatus.getTicket());

        Ticket reloaded = dbService.get(ticket.getGid());
        Assertions.assertTrue(reloaded.getWaitDistribution(),
                "Тикет должен ожидать распределения т.к. он не должен распределиться на сотрудника");
    }

    /**
     * Проверяем, что в play-режиме завершение работы над тикетом прекращает и работу над ним в play-режиме.
     */
    @Test
    public void distribute_stopWork() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Ticket ticket = createTicket(ctx); // этот тикет должен распределиться на сотрудника
        bcpService.edit(ticket, Maps.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));

        // вызов
        distributionService.currentStatus(ctx.employee0);
        bcpService.edit(ticket, Maps.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, employeeStatus.getStatus());
    }

    /**
     * Проверяем распределение тикета при изменении статуса сотрудника на ожидающего назначение (есть тикет для
     * распределения)
     */
    @Test
    public void distribute_waitTask_employeeStatus() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        Ticket ticket = createTicket(ctx);

        // вызов системы
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus.getStatus());
        Assertions.assertEquals(ticket, employeeStatus.getTicket());
        Assertions.assertFalse(employeeStatus.getTicket().getWaitDistribution(),
                "Тикет не должен ожидать распределения т.к. он назначен на сотрудника");
    }

    /**
     * Проверяем переназначение тикета на другого оператора если первый оператор брал тикет в работу слишком долго.
     */
    @Test
    public void distribute_waitTaken_redistribute() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_OFFLINE);

        // вызов системы
        Ticket ticket = createTicket(ctx);
        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        // Пинудительно вызываем срабатывание триггера истечения времени работы над тикетом
        doRedistribute(ctx.employee0);

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus0 = distributionService.getEmployeeStatus(ctx.employee0);
        EmployeeDistributionStatus employeeStatus1 = distributionService.getEmployeeStatus(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_OFFLINE, employeeStatus0.getStatus(),
                "Сотрудник 0 должен перейти в статус offline т.к. слишком долго работал над тикетом");
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus1.getStatus(),
                "Тикет должен назначится на сотрудника 1");
        Assertions.assertNull(employeeStatus0.getTicket());
        Assertions.assertEquals(ticket, employeeStatus1.getTicket());
        Assertions.assertEquals(ctx.employee1, employeeStatus1.getTicket().getResponsibleEmployee(),
                "Ответственным за тикет должен быть оператор на которого он распределен");
        Assertions.assertFalse(employeeStatus1.getTicket().getWaitDistribution(),
                "Тикет не должен ожидать распределеия т.к. он уже распределен на оператора 1");
    }

    /**
     * Проверяем переназначение тикет ана другого оператора если первый оператор работал над тикетом слишком долго.
     */
    @Test
    public void distribute_processing_redistribute() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_OFFLINE);

        // вызов системы
        Ticket ticket = createTicket(ctx);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_PROCESSING);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        // Пинудительно вызываем срабатывание триггера истечения времени работы над тикетом
        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "processingBackTimer");

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus0 = distributionService.getEmployeeStatus(ctx.employee0);
        EmployeeDistributionStatus employeeStatus1 = distributionService.getEmployeeStatus(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_OFFLINE, employeeStatus0.getStatus(),
                "Сотрудник 0 должен перейти в статус offline т.к. слишком долго работал над тикетом");
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus1.getStatus(),
                "Тикет должен назначится на сотрудника 1");
        Assertions.assertNull(employeeStatus0.getTicket());
        Assertions.assertEquals(ticket, employeeStatus1.getTicket());
        Assertions.assertEquals(ctx.employee1, employeeStatus1.getTicket().getResponsibleEmployee(),
                "Ответственным за тикет должен быть оператор на которого он распределен");
        Assertions.assertFalse(employeeStatus1.getTicket().getWaitDistribution(),
                "Тикет не должен ожидать распределеия т.к. он уже распределен на оператора 1");
    }

    /**
     * Проверяем переназначение тикета взятого не в Play режиме.
     */
    @Test
    public void redistribute_ticket_employee_not_in_play() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_OFFLINE);

        // вызов системы
        Ticket ticket = createTicket(ctx);
        Map<String, Object> properties = new HashMap<>();
        properties.put(Ticket.WAIT_DISTRIBUTION, Boolean.FALSE);
        properties.put(Ticket.RESPONSIBLE_EMPLOYEE, ctx.employee0);
        properties.put(Ticket.STATUS, Ticket.STATUS_PROCESSING);

        bcpService.edit(ticket, properties);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        // Пинудительно вызываем срабатывание триггера истечения времени работы над тикетом
        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "processingBackTimer");

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus0 = distributionService.getEmployeeStatus(ctx.employee0);
        EmployeeDistributionStatus employeeStatus1 = distributionService.getEmployeeStatus(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_OFFLINE, employeeStatus0.getStatus(),
                "Сотрудник 0 должен перейти в статус offline т.к. слишком долго работал над тикетом");
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus1.getStatus(),
                "Тикет должен назначится на сотрудника 1");
        Assertions.assertNull(employeeStatus0.getTicket());
        Assertions.assertEquals(ticket, employeeStatus1.getTicket());
        Assertions.assertEquals(ctx.employee1, employeeStatus1.getTicket().getResponsibleEmployee(),
                "Ответственным за тикет должен быть оператор на которого он распределен");
        Assertions.assertFalse(employeeStatus1.getTicket().getWaitDistribution(),
                "Тикет не должен ожидать распределеия т.к. он уже распределен на оператора 1");
    }

    /**
     * Проверяем переназначение тикета взятого не в Play режиме, при этом оператор в Play режиме
     */
    @Test
    public void redistribute_ticket_employee_in_play() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_OFFLINE);

        // вызов системы
        Ticket ticket = createTicket(ctx);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_PROCESSING);

        Ticket ticket2 = createTicket(ctx);

        Map<String, Object> properties = new HashMap<>();
        properties.put(Ticket.WAIT_DISTRIBUTION, Boolean.FALSE);
        properties.put(Ticket.RESPONSIBLE_EMPLOYEE, ctx.employee0);
        properties.put(Ticket.STATUS, Ticket.STATUS_PROCESSING);

        bcpService.edit(ticket2, properties);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        // Пинудительно вызываем срабатывание триггера истечения времени работы над тикетом
        timerTestUtils.simulateTimerExpiration(ticket2.getGid(), "processingBackTimer");

        // проверка утверждений
        EmployeeDistributionStatus employeeStatus0 = distributionService.getEmployeeStatus(ctx.employee0);
        EmployeeDistributionStatus employeeStatus1 = distributionService.getEmployeeStatus(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, employeeStatus0.getStatus(),
                "Сотрудник 0 должен перейти в статус offline т.к. слишком долго работал над тикетом");
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus1.getStatus(),
                "Тикет должен назначится на сотрудника 1");
        Assertions.assertEquals(ticket, employeeStatus0.getTicket());
        Assertions.assertEquals(ticket2, employeeStatus1.getTicket());
        Assertions.assertEquals(ctx.employee1, employeeStatus1.getTicket().getResponsibleEmployee(),
                "Ответственным за тикет должен быть оператор на которого он распределен");
        Assertions.assertFalse(employeeStatus1.getTicket().getWaitDistribution(),
                "Тикет не должен ожидать распределеия т.к. он уже распределен на оператора 1");
    }

    @Test
    public void ftsBody() {

        Map<String, Object> ticketAttributes1 = buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1);
        ticketAttributes1.put(Ticket.DESCRIPTION, "Ехал");
        ticketAttributes1.put(Ticket.CLIENT_NAME, "Грека");
        ticketAttributes1.put(Ticket.CLIENT_EMAIL, Randoms.email());

        Map<String, Object> ticketAttributes2 = buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1);
        ticketAttributes2.put(Ticket.DESCRIPTION, "Видит Грека в реке рак");

        Ticket ticket1 = bcpService.create(TicketTestConstants.TICKET_TEST_FQN, ticketAttributes1);
        Ticket ticket2 = bcpService.create(TicketTestConstants.TICKET_TEST_FQN, ticketAttributes2);

        List<Entity> resultList1 = searchForTicket("Грек ехал через");
        Assertions.assertEquals(1, resultList1.size());
        Assertions.assertTrue(isListContainsEntity(resultList1, ticket1), "должен найтись первый тикет");

        List<Entity> resultList2 = searchForTicket("Рак в реке");
        Assertions.assertEquals(1, resultList2.size());
        Assertions.assertTrue(isListContainsEntity(resultList2, ticket2), "должен найтись второй тикет");

        List<Entity> resultList3 = searchForTicket(null);
        Assertions.assertTrue(isListContainsEntity(resultList3, ticket1), "должен найтись и первый тикет");
        Assertions.assertTrue(isListContainsEntity(resultList3, ticket2), "должен найтись и второй тикет");
    }

    /**
     * Проверяем вычислимый атрибут hierarchyServices
     */
    @Test
    @Transactional
    public void ouHierarchyServices() {
        // настройка системы

        Ou root = ouTestUtils.createOu();
        Ou child = ouTestUtils.createOu(root);

        Ou otherRoot = ouTestUtils.createOu();
        Ou otherChild = ouTestUtils.createOu(otherRoot);

        Service rootService = ticketTestUtils.createService(Maps.of(
                "responsibleOus", root, Service.RESPONSIBLE_TEAM, ctx.team0));
        Service childService = ticketTestUtils.createService(Maps.of(
                "responsibleOus", child, Service.RESPONSIBLE_TEAM, ctx.team0));

        Service otherRootService = ticketTestUtils.createService(Maps.of(
                "responsibleOus", otherRoot, Service.RESPONSIBLE_TEAM, ctx.team0));
        Service otherChildService = ticketTestUtils.createService(Maps.of(
                "responsibleOus", otherChild, Service.RESPONSIBLE_TEAM, ctx.team0));

        dbService.flush();
        dbService.clear();

        // вызов системы
        Entity entity = dbService.get(child.getGid());
        Collection<Service> services = entity.getAttribute("hierarchyServices");

        Assertions.assertEquals(2, services.size());
        Assertions.assertTrue(services.contains(rootService));
        Assertions.assertTrue(services.contains(childService));
    }

    /**
     * Полнотекстовый поиск по тикетам
     *
     * @param queryString текст поискового запроса
     * @return Список тикетов
     */
    private List<Entity> searchForTicket(String queryString) {
        Filter filter = Filters.fullTextMatch(queryString);
        Query query = Query.of(Ticket.FQN).withFilters(filter);

        return dbService.list(query);
    }

    private boolean isListContainsEntity(List<Entity> list, Entity entity) {
        return list.stream().anyMatch(le -> le.getGid().equals(entity.getGid()));
    }


    /**
     * Эиулируем срабатывание триггера перерасределения тикета по операторам
     *
     * @param employee
     */
    private void doRedistribute(Employee employee) {
        EmployeeDistributionStatus employeeStatus0 = distributionService.getEmployeeStatus(employee);
        timerTestUtils.simulateTimerExpiration(employeeStatus0.getGid(), EmployeeDistributionStatus.ALLOWANCE_RESOLUTION_TIME);
    }


    private EmployeeDistributionStatus setEmployeeStatus(Employee employee, String statusWaitTicket) {
        return distributionService.setEmployeeStatus(employee, statusWaitTicket);
    }


    private Ticket create(Map<String, Object> attributes) {
        return bcpService.create(TicketTestConstants.TICKET_TEST_FQN, attributes);
    }

    @Test
    public void needsHelp() {
        // настройка системы

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Ticket ticket0 = createTicket(ctx);

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Ticket ticket1 = createTicket(ctx);

        ctx.ou = dbService.get(ctx.ou.getGid());
        ctx.parentOu = dbService.get(ctx.parentOu.getGid());
        ctx.supervisor = dbService.get(ctx.supervisor.getGid());

        assertNeedHelp(ctx.employee0, 0);
        assertNeedHelp(ctx.employee1, 0);
        assertNeedHelp(ctx.supervisor, 0);

        needsHelpService.setNeedsHelp(ctx.employee0, ticket0.getGid(), true);

        assertNeedHelp(ctx.employee0, 1);
        assertNeedHelp(ctx.employee1, 0);
        assertNeedHelp(ctx.supervisor, 1);

        needsHelpService.setNeedsHelp(ctx.employee1, ticket1.getGid(), true);

        assertNeedHelp(ctx.employee0, 1);
        assertNeedHelp(ctx.employee1, 1);
        assertNeedHelp(ctx.supervisor, 2);

        needsHelpService.setNeedsHelp(ctx.employee0, ticket0.getGid(), false);

        assertNeedHelp(ctx.employee0, 0);
        assertNeedHelp(ctx.employee1, 1);
        assertNeedHelp(ctx.supervisor, 1);

        NeedsHelpAlert alert0 = needsHelpService.setNeedsHelp(ctx.employee0, ticket0.getGid(), true);

        assertNeedHelp(ctx.employee0, 1);
        assertNeedHelp(ctx.employee1, 1);
        assertNeedHelp(ctx.supervisor, 2);

        needsHelpService.setNeedsHelp(ctx.employee1, ticket0.getGid(), false);

        assertNeedHelp(ctx.employee0, 1);
        assertNeedHelp(ctx.employee1, 0);
        assertNeedHelp(ctx.supervisor, 1);

        bcpService.edit(alert0, Maps.of(NeedsHelpAlert.STATUS, NeedsHelpAlert.Statuses.ARCHIVED));

        assertNeedHelp(ctx.employee0, 0);
        assertNeedHelp(ctx.employee1, 0);
        assertNeedHelp(ctx.supervisor, 0);

        needsHelpService.setNeedsHelp(ctx.employee0, null, true);
        needsHelpService.setNeedsHelp(ctx.employee0, null, true);

        assertNeedHelp(ctx.employee0, 1);
        assertNeedHelp(ctx.employee1, 0);
        assertNeedHelp(ctx.supervisor, 1);
    }

    private void assertNeedHelp(Employee employee, int alertsSize) {
        Assertions.assertEquals(alertsSize, needsHelpService.getNeedsHelp(employee).size());
    }

    @Test
    public void deferTest() throws InterruptedException {
        Ticket ticket = createTicket(ctx);

        Assertions.assertEquals(0, (long) ticket.getDeferCount(), "изначально кол-во откладываний должно быть 0");

        // переводим тикет в processing, а затем в deferred
        bcpService.edit(ticket, Maps.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        bcpService.edit(ticket, Maps.of(
                Ticket.STATUS, Ticket.STATUS_DEFERRED,
                Ticket.DEFER_TIME, Duration.ZERO
        ));

        // проверяем корректность выполнения откладывания тикета
        Ticket reloadedTicket = dbService.get(ticket.getGid());
        Assertions.assertEquals(1L, (long) reloadedTicket.getDeferCount(), "в тикет должно записаться количество " +
                "откладываний");
        Assertions.assertEquals(Ticket.STATUS_DEFERRED, reloadedTicket.getStatus(),
                "тикет должен быть в статусе отложен");

        // симулируем срабатывание триггера на истечение времени откладывания
        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "deferBackTimer");

        // проверяем корректность восстановления тикета
        reloadedTicket = dbService.get(ticket.getGid());
        Assertions.assertEquals(Ticket.STATUS_REOPENED, reloadedTicket.getStatus(),
                "тикет должен быть в статусе переоткрыт");
    }

    @Test
    public void testMultipleTicketsWithSameChannel_viaDistribution_viaTicketCreation() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        distributionService.currentStatus(ctx.employee0);

        final var distribution_ = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution_.getStatus());

        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        final var distribution = distributionService.getEmployeeStatus(ctx.employee0);

        Ticket ticket = dbService.get(ticketGid);
        Ticket ticket2 = dbService.get(ticketGid2);

        Assertions.assertEquals(ticket, distribution.getTicket());
        Assertions.assertEquals(ticket2, Iterables.getFirst(distribution.getInactiveTickets(), null));
        Assertions.assertEquals(1, distribution.getInactiveTickets().size());

        Assertions.assertEquals(ctx.employee0, ticket.getResponsibleEmployee());
        Assertions.assertEquals(ctx.employee0, ticket2.getResponsibleEmployee());

        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
        Assertions.assertNotEquals(Ticket.STATUS_PROCESSING, ticket2.getStatus());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());

    }

    @Test
    @Transactional
    public void testMultipleTicketsWithSameChannel_viaDistribution_viaDistribution() {
        TicketTestUtils.TestContext ctx = ticketTestUtils.create();

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        Exceptions.sneakyRethrow(() -> Thread.sleep(10));
        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        distributionService.currentStatus(ctx.employee0);


        final var distribution = distributionService.getEmployeeStatus(ctx.employee0);

        Ticket ticket = dbService.get(ticketGid);
        Ticket ticket2 = dbService.get(ticketGid2);

        Assertions.assertEquals(ticket, distribution.getTicket());
        Assertions.assertEquals(ticket2, Iterables.getFirst(distribution.getInactiveTickets(), null));
        Assertions.assertEquals(1, distribution.getInactiveTickets().size());

        Assertions.assertEquals(ctx.employee0, ticket.getResponsibleEmployee());
        Assertions.assertEquals(ctx.employee0, ticket2.getResponsibleEmployee());

        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
        Assertions.assertNotEquals(Ticket.STATUS_PROCESSING, ticket2.getStatus());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());

    }

    @Test
    public void testMultipleTicketsWithSameChannel_viaTicketCreation_viaTicketCreation() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        distributionService.currentStatus(ctx.employee0);

        final var distribution = distributionService.getEmployeeStatus(ctx.employee0);

        Ticket ticket = dbService.get(ticketGid);
        Ticket ticket2 = dbService.get(ticketGid2);

        Assertions.assertEquals(ticket, distribution.getTicket());
        Assertions.assertEquals(ticket2, Iterables.getFirst(distribution.getInactiveTickets(), null));
        Assertions.assertEquals(1, distribution.getInactiveTickets().size());

        Assertions.assertEquals(ctx.employee0, ticket.getResponsibleEmployee());
        Assertions.assertEquals(ctx.employee0, ticket2.getResponsibleEmployee());

        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
        Assertions.assertNotEquals(Ticket.STATUS_PROCESSING, ticket2.getStatus());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());

    }

    @Test
    public void testSwitchingToPendingTicketWhenOtherIsWaitingResponse() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        distributionService.currentStatus(ctx.employee0);

        var distribution = distributionService.getEmployeeStatus(ctx.employee0);

        Ticket ticket = dbService.get(ticketGid);
        Ticket ticket2 = dbService.get(ticketGid2);

        Assertions.assertEquals(ticket, distribution.getTicket());
        Assertions.assertEquals(ticket2, Iterables.getFirst(distribution.getInactiveTickets(), null));
        Assertions.assertEquals(1, distribution.getInactiveTickets().size());

        Assertions.assertEquals(ctx.employee0, ticket.getResponsibleEmployee());
        Assertions.assertEquals(ctx.employee0, ticket2.getResponsibleEmployee());

        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
        Assertions.assertNotEquals(Ticket.STATUS_PROCESSING, ticket2.getStatus());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, "active"));

        distributionService.currentStatus(ctx.employee0);
        distribution = distributionService.getEmployeeStatus(ctx.employee0);

        ticket = dbService.get(ticketGid);
        ticket2 = dbService.get(ticketGid2);

        Assertions.assertEquals(ticket2, distribution.getTicket());
        Assertions.assertEquals(ticket, Iterables.getFirst(distribution.getInactiveTickets(), null));
        Assertions.assertEquals(1, distribution.getInactiveTickets().size());

        Assertions.assertEquals(ctx.employee0, ticket.getResponsibleEmployee());
        Assertions.assertEquals(ctx.employee0, ticket2.getResponsibleEmployee());

        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket2.getStatus());
        Assertions.assertNotEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());
    }

    @Test
    public void testStayingOnTicketIfNoOtherAvailable() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        distributionService.currentStatus(ctx.employee0);

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, "active"));

        distributionService.currentStatus(ctx.employee0);
        final var distribution = distributionService.getEmployeeStatus(ctx.employee0);

        Ticket ticket = dbService.get(ticketGid);

        Assertions.assertEquals(ticket, distribution.getTicket());
        Assertions.assertTrue(distribution.getInactiveTickets().isEmpty());

        Assertions.assertEquals(ctx.employee0, ticket.getResponsibleEmployee());

        Assertions.assertNotEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());
    }

    @Test
    public void testTicketCouldBeStoppedWhenDistributionIsWaitTaken() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN,
                distributionService.getEmployeeStatus(ctx.employee0).getStatus());
        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, "missed"));

        distributionService.currentStatus(ctx.employee0);
        final var distribution = distributionService.getEmployeeStatus(ctx.employee0);

        Ticket ticket = dbService.get(ticketGid);

        Assertions.assertNull(distribution.getTicket());
        Assertions.assertTrue(distribution.getInactiveTickets().isEmpty());

        Assertions.assertNull(ticket.getResponsibleEmployee());

        Assertions.assertEquals("missed", ticket.getStatus());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution.getStatus());
    }

    @Test
    public void testRequiredBreak() {

        final var ticketGid = createTicket(ctx).getGid();

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        distributionService.currentStatus(ctx.employee0);

        var distribution_ = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution_.getStatus());

        final var ticketGid2 = createTicket(ctx).getGid();

        distributionService.doBreak(ctx.employee0, EmployeeDistributionStatus.STATUS_NOT_READY);

        distribution_ = distributionService.getEmployeeStatus(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution_.getStatus());
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_NOT_READY, distribution_.getRequiredBreak());

        bcpService.edit(ticketGid, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));

        final var distribution = distributionService.getEmployeeStatus(ctx.employee0);

        Ticket ticket = dbService.get(ticketGid);
        Ticket ticket2 = dbService.get(ticketGid2);

        Assertions.assertNull(distribution.getTicket());
        Assertions.assertEquals(Set.of(), distribution.getInactiveTickets());

        Assertions.assertNull(ticket.getResponsibleEmployee());

        Assertions.assertEquals(Ticket.STATUS_RESOLVED, ticket.getStatus());
        Assertions.assertEquals(Ticket.STATUS_REGISTERED, ticket2.getStatus());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_NOT_READY, distribution.getStatus());
    }

    @Test
    public void testReopeningFromInactiveTicketWithOtherEmployeeReadyToTakeTicket() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        final var ticketGid3 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        distributionService.currentStatus(ctx.employee0);
        distributionService.currentStatus(ctx.employee1);

        var distribution1 = getDistribution(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution1.getStatus());
        Assertions.assertEquals(ticketGid, distribution1.getTicket().getGid());
        Assertions.assertEquals(Set.of(ticketGid2),
                distribution1.getInactiveTickets().stream().map(HasGid::getGid).collect(Collectors.toSet()));

        var distribution2 = getDistribution(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution2.getStatus());
        Assertions.assertEquals(ticketGid3, distribution2.getTicket().getGid());
        Assertions.assertTrue(distribution2.getInactiveTickets().isEmpty());

        bcpService.edit(ticketGid2, Map.of(Ticket.STATUS, Ticket.STATUS_REOPENED));

        distribution1 = getDistribution(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution1.getStatus());
        Assertions.assertEquals(ticketGid, distribution1.getTicket().getGid());
        Assertions.assertTrue(distribution1.getInactiveTickets().isEmpty());

        distribution2 = getDistribution(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution2.getStatus());
        Assertions.assertEquals(ticketGid3, distribution2.getTicket().getGid());
        Assertions.assertEquals(Set.of(ticketGid2),
                distribution2.getInactiveTickets().stream().map(HasGid::getGid).collect(Collectors.toSet()));
    }

    @Test
    public void testWaitDistributionOnProcessingTicket() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();
        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH2)).getGid();

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_NOT_READY);

        distributionService.currentStatus(ctx.employee0);
        distributionService.currentStatus(ctx.employee1);

        var distribution1 = getDistribution(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution1.getStatus());
        Assertions.assertEquals(ticketGid, distribution1.getTicket().getGid());
        Assertions.assertEquals(Set.of(ticketGid2),
                distribution1.getInactiveTickets().stream().map(HasGid::getGid).collect(Collectors.toSet()));

        var distribution2 = getDistribution(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_NOT_READY, distribution2.getStatus());
        Assertions.assertTrue(distribution2.getAllTickets().isEmpty());

        bcpService.edit(ticketGid2, Map.of(Ticket.STATUS, "waitForOperator"));
        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        distribution1 = getDistribution(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution1.getStatus());
        Assertions.assertEquals(ticketGid, distribution1.getTicket().getGid());
        Assertions.assertEquals(Set.of(ticketGid2),
                distribution1.getInactiveTickets().stream().map(HasGid::getGid).collect(Collectors.toSet()));

        distribution2 = getDistribution(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution2.getStatus());
        Assertions.assertTrue(distribution2.getAllTickets().isEmpty());
    }

    @Test
    public void redistributeOnPendingTicketReopened() {

        var ch3 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH3);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch3));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH3.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch3)
                        : List.of());

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH3)).getGid();
        final var ticketGid2 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH3)).getGid();
        final var ticketGid3 = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH3)).getGid();

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        distributionService.currentStatus(ctx.employee0);

        var distribution1 = getDistribution(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution1.getStatus());
        Assertions.assertEquals(ticketGid, distribution1.getTicket().getGid());
        Assertions.assertEquals(Set.of(ticketGid2, ticketGid3),
                distribution1.getInactiveTickets().stream().map(HasGid::getGid).collect(Collectors.toSet()));

        setEmployeeStatus(ctx.employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        distributionService.currentStatus(ctx.employee1);

        var distribution2 = getDistribution(ctx.employee1);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution2.getStatus());
        Assertions.assertNull(distribution2.getTicket());
        Assertions.assertTrue(distribution2.getInactiveTickets().isEmpty());

        bcpService.edit(ticketGid2, Maps.of(Ticket.STATUS, Ticket.STATUS_REOPENED));

        distribution1 = getDistribution(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution1.getStatus());
        Assertions.assertEquals(ticketGid, distribution1.getTicket().getGid());
        Assertions.assertEquals(Set.of(ticketGid3),
                distribution1.getInactiveTickets().stream().map(HasGid::getGid).collect(Collectors.toSet()));

        distribution2 = getDistribution(ctx.employee1);
        Assertions.assertEquals(ticketGid2, distribution2.getTicket().getGid());
        Assertions.assertTrue(distribution2.getInactiveTickets().isEmpty());
    }

    @Test
    public void redistributeOnServiceHasChanged() {

        var ch1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch1));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH1.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch1)
                        : List.of());

        final var ticketGid = create(buildTicketAttributes(ctx, ctx.team0, TestChannels.CH1)).getGid();

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        distributionService.currentStatus(ctx.employee0);

        var distribution1 = getDistribution(ctx.employee0);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution1.getStatus());
        Assertions.assertEquals(ticketGid, distribution1.getTicket().getGid());
        Assertions.assertTrue(distribution1.getInactiveTickets().isEmpty());

        setEmployeeStatus(ctx.employee4, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        distributionService.currentStatus(ctx.employee4);

        setEmployeeStatus(ctx.employee5, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        distributionService.currentStatus(ctx.employee5);

        var distribution2 = getDistribution(ctx.employee4);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution2.getStatus());
        Assertions.assertNull(distribution2.getTicket());
        Assertions.assertTrue(distribution2.getInactiveTickets().isEmpty());

        var distribution3 = getDistribution(ctx.employee5);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution3.getStatus());
        Assertions.assertNull(distribution3.getTicket());
        Assertions.assertTrue(distribution3.getInactiveTickets().isEmpty());

        bcpService.edit(ticketGid, Maps.of(
                Ticket.STATUS, "serviceHasChanged",
                Ticket.SERVICE, ctx.service1
        ));

        distributionService.currentStatus(ctx.employee4);
        distributionService.currentStatus(ctx.employee5);

        distribution1 = getDistribution(ctx.employee0);
        distribution2 = getDistribution(ctx.employee4);
        distribution3 = getDistribution(ctx.employee5);

        Ticket ticket = getEntity(ticketGid);

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution1.getStatus());
        Assertions.assertNull(distribution1.getTicket());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution2.getStatus());
        Assertions.assertNull(distribution2.getTicket());

        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution3.getStatus());
        Assertions.assertEquals(ticket, distribution3.getTicket());

        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
        Assertions.assertEquals(ctx.employee5, ticket.getResponsibleEmployee());
    }

    private EmployeeDistributionStatus getDistribution(Employee employee0) {
        var status = distributionService.getEmployeeStatus(employee0);
        status.getAllTickets();
        return status;
    }

    private <T extends Entity> T getEntity(String gid) {
        return dbService.get(gid);
    }
}
