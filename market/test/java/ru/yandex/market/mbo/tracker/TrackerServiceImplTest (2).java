package ru.yandex.market.mbo.tracker;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.IssueCreate;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 22.08.2019
 */
public class TrackerServiceImplTest {

    private TrackerServiceImpl trackerService;

    @Before
    public void setUp() throws IOException {
        trackerService = new TrackerServiceImpl(null);
    }

    @Test
    public void assigneeNullAsIs() {
        IssueCreate issueCreate = trackerService.convertCreate(createTicket());

        assertThat(issueCreate.getValues().getTs("assignee")).isNull();
    }

    @Test
    public void assigneeToLowerCase() {
        Ticket ticket = createTicket();
        ticket.setAssignee("SomeUser");
        IssueCreate issueCreate = trackerService.convertCreate(ticket);

        assertThat(issueCreate.getValues().getTs("assignee")).isEqualTo("someuser");
    }

    @Test
    public void authorToLowerCase() {
        Ticket ticket = createTicket();
        ticket.setAuthor("SomeUser");
        IssueCreate issueCreate = trackerService.convertCreate(ticket);

        assertThat(issueCreate.getValues().getTs("author")).isEqualTo("someuser");
    }

    @Test
    public void summoneesToLowerCase() {
        TicketComment comment = new TicketComment("text");
        comment.setSummonees(Arrays.asList("Bob", "ElizaBet"));

        CommentCreate commentCreate = trackerService.convert(comment);

        assertThat(commentCreate.getComment()).isEqualTo(Option.ofNullable(comment.getComment()));
        assertThat(commentCreate.getSummonees()).containsExactly("bob", "elizabet");
    }

    @Test
    public void followersToLowerCase() {
        Ticket ticket = createTicket();
        ticket.getFollowers().add("UncleBob");
        ticket.getFollowers().add("Steve Buscemi");
        IssueCreate issueCreate = trackerService.convertCreate(ticket);

        Object followers = issueCreate.getValues().getTs("followers");
        assertThat((String[]) followers).containsExactly("unclebob", "steve buscemi");

    }

    private static Ticket createTicket() {
        Ticket ticket = new Ticket();
        ticket.setType(TicketType.TASK);
        return ticket;
    }
}
