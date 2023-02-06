package ru.yandex.market.pers.tms.timer.startrek;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.util.db.ConfigurationService;
import ru.yandex.startrek.client.error.ForbiddenException;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.QueueRef;
import ru.yandex.startrek.client.model.ResolutionRef;
import ru.yandex.startrek.client.model.UserRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.03.2020
 */
public class UserDataRemovalExecutorTest extends MockedPersTmsTest {

    @Autowired
    private StartrekService startrekService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private UserDataRemovalExecutor executor;
    @Autowired
    @Qualifier("basketJdbcTemplate")
    private JdbcTemplate basketJdbcTemplate;

    @Value("${pers.startrek.userDataRemoval.queue.name}")
    private String queueName;

    @Before
    public void prepare() {
        when(basketJdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(1L);
        configurationService.mergeValue(UserDataRemovalExecutor.ACTIVE_KEY, "true");
    }

    private QueueRef queueRef(String name) {
        QueueRef res = mock(QueueRef.class);
        when(res.getKey()).thenReturn(name);
        return res;
    }

    @Test
    public void testSimpleCreation() throws Exception {
        configurationService.mergeValue(UserDataRemovalExecutor.LAST_TICKET_ID_KEY, "42");

        int ticketId = 43;
        int lastTicketId = 48;

        // need 3 tickets: removal, other, any closed
        when(startrekService.tryGetTicket(getTicketKey(ticketId))).then(invocation -> {
            Issue issue = mockIssueBasic("Удаление данных пользователя puid=123456789 с сервиса Покупки на Маркете (ex Беру)");
            return Optional.of(issue);
        });
        when(startrekService.tryGetTicket(getTicketKey(44))).then(invocation -> {
            Issue issue = mockIssueBasic("Удаление данных пользователя puid=123456789 с сервиса Покупки на Маркете (ex Беру)");
            when(issue.getResolution()).thenReturn(Option.of(mock(ResolutionRef.class)));
            return Optional.of(issue);
        });
        when(startrekService.tryGetTicket(getTicketKey(45))).then(invocation -> {
            Issue issue = mockIssueBasic("Что-то ещё");
            return Optional.of(issue);
        });
        when(startrekService.tryGetTicket(getTicketKey(46))).thenThrow(ForbiddenException.class);
        when(startrekService.tryGetTicket(getTicketKey(47))).then(invocation -> {
            Issue issue = mockIssueBasic("Удаление данных пользователя puid=123456789 с сервиса Покупки на Маркете (ex Беру)");
            QueueRef queue = queueRef("TESTQUEUE_UNKNOWN");
            when(issue.getQueue()).thenReturn(queue);
            return Optional.of(issue);
        });
        when(startrekService.tryGetTicket(getTicketKey(lastTicketId))).then(invocation -> {
            return Optional.empty();
        });

        // run

        executor.runTmsJob();

        assertEquals(lastTicketId - 1,
            configurationService.getValue(UserDataRemovalExecutor.LAST_TICKET_ID_KEY, Integer.class).intValue());

        // check only one is now in table - it is not processed
        assertEquals(1L,
            pgJdbcTemplate.queryForObject("select count(*) from user_data_removal_ticket", Long.class).longValue());
        assertEquals(1L,
            pgJdbcTemplate.queryForObject(
                "select count(*) from user_data_removal_ticket where ticket_key = ? and user_id = 123456789",
                Long.class, getTicketKey(ticketId)).longValue());

        verify(startrekService, times(1)).createComment(any(), anyString());

        // like by no-approver - run - check nothing happened
        initMocks();

        ListF<UserRef> votedBy = new ArrayListF<>();

        when(startrekService.tryGetTicket(getTicketKey(ticketId))).then(invocation -> {
            UserRef someUserRef = mock(UserRef.class);
            when(someUserRef.getLogin()).thenReturn("some_login");
            votedBy.add(someUserRef);

            Issue issue = mockIssueBasic("Удаление данных пользователя puid=123456789 с сервиса Покупки на Маркете (ex Беру)");
            when(issue.getVotedBy()).thenReturn(votedBy);
            return Optional.of(issue);
        });
        when(startrekService.tryGetTicket(getTicketKey(lastTicketId))).then(invocation -> {
            return Optional.empty();
        });

        // run
        executor.runTmsJob();

        assertEquals(1L,
            pgJdbcTemplate.queryForObject(
                "select count(*) from user_data_removal_ticket where ticket_key = ? and state = 0",
                Long.class, getTicketKey(ticketId)).longValue());

        // like by approver - check data was attempted to be removed
        UserRef approverRef = mock(UserRef.class);
        when(approverRef.getLogin()).thenReturn("ilyakis");
        votedBy.add(approverRef);

        // run
        executor.runTmsJob();

        assertEquals(1L,
            pgJdbcTemplate.queryForObject(
                "select count(*) from user_data_removal_ticket where ticket_key = ? and state = 1",
                Long.class, getTicketKey(ticketId)).longValue());

        verify(startrekService, times(1)).createComment(any(), anyString());
        verify(startrekService, times(1)).closeTicket(any());
    }

    @Test
    public void testNotSoSimpleCreation() throws Exception {
        configurationService.mergeValue(UserDataRemovalExecutor.LAST_TICKET_ID_KEY, "42");

        int ticketId = 43;
        int lastTicketId = 44;

        // this text is special: contains nbsp, non-breaking space
        when(startrekService.tryGetTicket(getTicketKey(ticketId))).then(invocation -> {
            Issue issue = mockIssueBasic("Удаление данных пользователя puid=657559798 с сервиса Покупки на Маркете (ex Беру)");
            return Optional.of(issue);
        });
        when(startrekService.tryGetTicket(getTicketKey(lastTicketId))).then(invocation -> {
            return Optional.empty();
        });

        // run
        executor.runTmsJob();

        assertEquals(lastTicketId - 1,
            configurationService.getValue(UserDataRemovalExecutor.LAST_TICKET_ID_KEY, Integer.class).intValue());

        // check only one is now in table - it is not processed
        assertEquals(1L,
            pgJdbcTemplate.queryForObject("select count(*) from user_data_removal_ticket", Long.class).longValue());
        assertEquals(1L,
            pgJdbcTemplate.queryForObject(
                "select count(*) from user_data_removal_ticket where ticket_key = ? and user_id = 657559798",
                Long.class, getTicketKey(ticketId)).longValue());

        verify(startrekService, times(1)).createComment(any(), anyString());
    }

    @Test
    public void testDeadline() throws Exception {
        // create removal ticket
        configurationService.mergeValue(UserDataRemovalExecutor.LAST_TICKET_ID_KEY, "42");

        int ticketId = 43;
        int lastTicketId = 44;

        // need 3 tickets: removal, other, any closed
        when(startrekService.tryGetTicket(getTicketKey(ticketId))).then(invocation -> {
            Issue issue = mockIssueBasic("Удаление данных пользователя puid=123456789 с сервиса Покупки на Маркете (ex Беру)");
            return Optional.of(issue);
        });
        when(startrekService.tryGetTicket(getTicketKey(lastTicketId))).then(invocation -> {
            return Optional.empty();
        });

        // run
        executor.runTmsJob();

        // update date to the past
        pgJdbcTemplate.update("" +
                        "update user_data_removal_ticket " +
                        "set update_time = now() - interval '10 day' " +
                        "where ticket_key = ?",
            getTicketKey(ticketId));

        // run again - should be exception
        try {
            executor.runTmsJob();
            fail();
        } catch (RuntimeException e) {
            assertEquals("ALARM! There are 1 tickets for user data removal: [MARKETPERSTEST-43]",
                e.getMessage());
        }
    }

    @Test
    public void testForceDeleteCase() throws Exception {
        //emulate no data for user
        when(basketJdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(0L);

        configurationService.mergeValue(UserDataRemovalExecutor.LAST_TICKET_ID_KEY, "42");

        int ticketId = 43;
        int lastTicketId = 44;

        // need 3 tickets: removal, other, any closed
        when(startrekService.tryGetTicket(getTicketKey(ticketId))).then(invocation -> {
            Issue issue = mockIssueBasic("Удаление данных пользователя puid=123456789 с сервиса Покупки на Маркете (ex Беру)");
            return Optional.of(issue);
        });
        when(startrekService.tryGetTicket(getTicketKey(lastTicketId))).then(invocation -> {
            return Optional.empty();
        });

        // run

        executor.runTmsJob();

        assertEquals(lastTicketId - 1,
            configurationService.getValue(UserDataRemovalExecutor.LAST_TICKET_ID_KEY, Integer.class).intValue());

        // check only one is now in table - it is not processed
        assertEquals(1L,
            pgJdbcTemplate.queryForObject("select count(*) from user_data_removal_ticket", Long.class).longValue());
        assertEquals(1L,
            pgJdbcTemplate.queryForObject(
                "select count(*) from user_data_removal_ticket where ticket_key = ? and user_id = 123456789",
                Long.class, getTicketKey(ticketId)).longValue());

        // run
        executor.runTmsJob();

        //check that deleted without approve
        assertEquals(1L,
            pgJdbcTemplate.queryForObject(
                "select count(*) from user_data_removal_ticket where ticket_key = ? and state = 1",
                Long.class, getTicketKey(ticketId)).longValue());

        assertEquals("robot-market-pers",
            pgJdbcTemplate.queryForObject(
                "select approved from user_data_removal_ticket where ticket_key = ?",
                String.class, getTicketKey(ticketId)));

        verify(startrekService, times(2)).createComment(any(), anyString());
        verify(startrekService, times(1)).closeTicket(any());
    }

    @NotNull
    private Issue mockIssueBasic(String text) {
        Issue issue = mock(Issue.class);
        when(issue.getSummary()).thenReturn(text);
        when(issue.getVotedBy()).thenReturn(new ArrayListF<>());
        when(issue.getResolution()).thenReturn(Option.empty());
        QueueRef queue = queueRef(queueName);
        when(issue.getQueue()).thenReturn(queue);
        return issue;
    }

    @NotNull
    private String getTicketKey(int id) {
        return queueName + "-" + id;
    }

}
