package ru.yandex.market.tsum.tms.tasks.duty.switchduty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.market.tsum.clients.abc.AbcApiClient;
import ru.yandex.market.tsum.clients.blackbox.BlackBoxClient;
import ru.yandex.market.tsum.clients.calendar.CalendarClient;
import ru.yandex.market.tsum.clients.calendar.CalendarEvent;
import ru.yandex.market.tsum.clients.juggler.JugglerApiClient;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.clients.telegraph.TelegraphApiClient;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;
import ru.yandex.market.tsum.core.duty.Duty;
import ru.yandex.market.tsum.core.duty.DutyManager;
import ru.yandex.market.tsum.tms.service.CronTaskDutySwitcher;
import ru.yandex.market.tsum.tms.service.StaffGroupNotificationContext;
import ru.yandex.market.tsum.tms.service.StaffGroupNotifier;
import ru.yandex.market.tsum.tms.service.SwitchDutyResult;
import ru.yandex.market.tsum.tms.tasks.duty.switchduty.tasks.SwitchDutyMarketInfraTest;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.Mockito.mock;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 22.11.2019
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SwitchDutyMarketInfraTaskTest.SwitchDutyConfiguration.class})
public class SwitchDutyMarketInfraTaskTest {
    @Autowired
    private DutyManager dutyManager;
    @Autowired
    private SwitchDutyMarketInfraTest task;
    @Autowired
    private Issues issues;

    /**
     * ssh -f -N -L 8080:calendar-api.tools.yandex.net:80 blacksmith01h.market.yandex.net
     */
    @Test
    public void createIssues() throws InterruptedException {
        Duty duty = dutyManager.getCurrentDuty("market-infra", "incident");
        CalendarEvent baseEvent = getCalendarEvent(dutyManager, duty, new Date());
        Date eventDate = baseEvent.getStartTs();
        CalendarEvent previousEvent = getCalendarEvent(
            dutyManager, duty, Date.from(eventDate.toInstant().minus(1, ChronoUnit.MINUTES))
        );

        Optional<Issue> firstIssue = createAndFindIssue(task, duty, previousEvent);
        Assert.assertTrue(firstIssue.isPresent());

        Optional<Issue> secondIssue = createAndFindIssue(task, duty, baseEvent);
        Assert.assertTrue(secondIssue.isPresent());
        Assert.assertEquals(
            SwitchDutyMarketInfra.getDutyTicketDescription(firstIssue.get().getKey()),
            secondIssue.get().getDescription().get()
        );
    }

    private Optional<Issue> createAndFindIssue(
        SwitchDutyMarketInfra task, Duty duty, CalendarEvent event
    ) throws InterruptedException {
        Date eventDate = event.getStartTs();
        String eventLogin = duty.getLoginExtractor().extractLogin(
            event, LocalDateTime.ofInstant(eventDate.toInstant(), ZoneId.systemDefault())
        );
        createIssue(task, eventLogin, event.getStartTs().toInstant(), event.getEndTs().toInstant());

        String issueSummary = SwitchDutyMarketInfra.getDutyTicketSummary(
            eventLogin, event.getStartTs().toInstant(), event.getEndTs().toInstant()
        );

        TimeUnit.SECONDS.sleep(10);

        return findExistingDutyTickets(issueSummary).stream()
            .filter(i -> issueSummary.equals(i.getSummary()))
            .findFirst();
    }

    private CalendarEvent getCalendarEvent(DutyManager dutyManager, Duty duty, Date date) {
        List<CalendarEvent> calendarEvents = dutyManager.getCalendarEvents(duty, date);
        return DutyManager.getFirstCalendarEventWithLogin(duty, calendarEvents)
            .orElseThrow(() -> new RuntimeException("CalendarEvent is not found"));
    }

    private void createIssue(
        SwitchDutyMarketInfra task, String login, Instant dutyStartInstant, Instant dutyEndInstant
    ) {
        StaffGroupNotificationContext staffGroupNotificationContext = mock(StaffGroupNotificationContext.class);
        Mockito.when(staffGroupNotificationContext.getStaffPerson()).thenReturn(new StaffPerson(
            login,
            1,
            null,
            null,
            null,
            null
        ));
        Mockito.when(staffGroupNotificationContext.getDutyStartInstant()).thenReturn(dutyStartInstant);
        Mockito.when(staffGroupNotificationContext.getDutyEndInstant()).thenReturn(dutyEndInstant);

        SwitchDutyResult result = mock(SwitchDutyResult.class);
        Mockito.when(result.isSuccessful()).thenReturn(false);
        Mockito.when(result.getStaffGroupNotificationContext()).thenReturn(staffGroupNotificationContext);
        task.onComplete(result);
    }

    private List<Issue> findExistingDutyTickets(String summary) {
        String query = String.format("Queue: %s AND Summary: \"%s\"", SwitchDutyMarketInfraTest.TRACKER_QUEUE, summary);
        IteratorF<Issue> issuesIterator = issues.find(query, Cf.list("key", "summary"));
        return issues.find(query, Cf.list("key", "summary", "description")).toList();
    }

    @Configuration
    @PropertySource({"classpath:tsum-tms-test.properties"})
    @ComponentScan("ru.yandex.market.tsum.tms.tasks.duty.switchduty.tasks")
    @Import({TsumDebugRuntimeConfig.class})
    @Lazy
    static class SwitchDutyConfiguration {
        @Bean
        public BlackBoxClient blackBoxClient() {
            return mock(BlackBoxClient.class);
        }

        @Autowired
        private CalendarClient calendarClient;

        @Bean
        public StaffGroupNotifier staffGroupNotifier() {
            return mock(StaffGroupNotifier.class);
        }

        @Bean
        public CronTaskDutySwitcher cronTaskDutySwitcher() {
            return mock(CronTaskDutySwitcher.class);
        }

        @Bean
        public DutyManager dutyManager() {
            DutyManager dutyManager = new DutyManager(
                mock(StaffApiClient.class),
                //TODO календарю нужны tvm тикеты, надо теперь тоже мокать
                calendarClient,
                mock(TelegraphApiClient.class),
                mock(JugglerApiClient.class),
                mock(AbcApiClient.class));

            dutyManager.addDepartmentDuty("market-infra", "incident",
                Duty.newBuilder()
                    .withCalendarLayerId(34699)
                    .withDutyGroupPhone(null)
                    .withJugglerNotificationRuleId("")
                    .withStaffGroupName("")
                    .build()
            );

            return dutyManager;
        }
    }

    @Test
    public void closePreviousTicket() {
        SwitchDutyMarketInfra sd = Mockito.spy(new SwitchDutyMarketInfra());
        Mockito.when(sd.getIssues()).thenReturn(issues);
        sd.closePreviousTicket("MARKETINFRA-9966");
    }
}
