package ru.yandex.market.delivery.transport_manager.service.startrek;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.StartrekEntity;
import ru.yandex.market.delivery.transport_manager.domain.entity.StartrekEntityType;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.create.TicketCreationDto;
import ru.yandex.market.delivery.transport_manager.service.ticket.StartrekTicketCreationService;
import ru.yandex.market.delivery.transport_manager.service.ticket.TicketQueue;
import ru.yandex.startrek.client.IssuesClient;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;

public class StartrekTicketCreationServiceTest extends AbstractContextualTest {

    public static final String LONG_TAG = "More then 500 chars. ".repeat(50);
    @Autowired
    private StartrekTicketCreationService startrekService;

    @Autowired
    private StartrekClient startrekClient;

    @Autowired
    private IssuesClient issuesClient;

    private static final IssueCreate ISSUE_MODEL = IssueCreate.builder()
        .queue(TicketQueue.FAILED_TRANSPORTATION.getQueueName())
        .project(1)
        .summary("ErrorTitle")
        .description("Error message")
        .type("task")
        .deadline(org.joda.time.Instant.parse("2021-10-10T21:00:00Z"))
        .tags(new String[]{
            "Tag1",
            "Tag2",
            "Tag with forbidden symbols",
            LONG_TAG.substring(0, StartrekTicketCreationService.MAX_TAG_LENGTH)
        })
        .build();

    @BeforeEach
    void setUp() {
        Mockito.when(issuesClient.create(Mockito.any())).thenReturn(
            new Issue(null, null, "KEY-1", "", 0, new EmptyMap<>(), null)
        );
        Mockito.when(startrekClient.issues(Mockito.any())).thenReturn(issuesClient);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/after/after_insert_from_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void parameters() {
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        startrekService.createTicket(
            new TicketCreationDto()
                .setDeadline(Instant.parse("2021-10-10T21:00:00Z"))
                .setProjectId(1L)
                .setHash("hash")
                .setDate(LocalDate.of(2021, 10, 10))
                .setTicketQueue(TicketQueue.FAILED_TRANSPORTATION)
                .setEntity(new StartrekEntity(1L, StartrekEntityType.TRANSPORTATION))
                .setMessage("Error message")
                .setTags(List.of("Tag1", "Tag2", "Tag\twith\nforbidden,symbols", "", LONG_TAG))
                .setTitle("ErrorTitle")
        );
        Mockito.verify(issuesClient).create(captor.capture());
        IssueCreate issueCreate = captor.getValue();
        assertThatModelEquals(ISSUE_MODEL, issueCreate);
    }

    @Test
    void noTokenLeft() {
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        startrekService.createTicket(
            new TicketCreationDto()
                .setTicketQueue(TicketQueue.FAILED_TRANSPORTATION)
                .setEntity(new StartrekEntity(1L, StartrekEntityType.TRANSPORTATION))
                .setMessage("Method not equal request type, <root><token>Nsk92bGS021Khe</token><uniq>orUY</uniq>")
                .setTags(List.of("Tag1", "Tag2"))
                .setTitle("ErrorTitle")
        );
        Mockito.verify(issuesClient).create(captor.capture());
        IssueCreate issueCreate = captor.getValue();
        softly.assertThat(issueCreate.getValues().get("description"))
            .isEqualTo("Method not equal request type, <root><uniq>orUY</uniq>");
    }

    @Test
    @DatabaseSetup("/repository/startrek/startrek_issues.xml")
    @ExpectedDatabase(
        value = "/repository/startrek/after/after_update_from_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateExistingOnCreation() {
        Issue existingIssue = Mockito.mock(Issue.class);
        Mockito.when(issuesClient.get(Mockito.anyString())).thenReturn(existingIssue);
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        startrekService.createTicket(
            new TicketCreationDto()
                .setTicketQueue(TicketQueue.FAILED_TRANSPORTATION)
                .setEntity(new StartrekEntity(1L, StartrekEntityType.TRANSPORTATION))
                .setMessage("Error message")
                .setTags(List.of())
                .setDate(LocalDate.of(2021, 6, 24))
                .setTitle("ErrorTitle")
        );

        Mockito.verify(issuesClient, Mockito.times(0)).create(Mockito.any());
        Mockito.verify(existingIssue).update(captor.capture());

        softly.assertThat(captor.getValue().getComment().get().getComment().get())
            .isEqualTo("Error message");
    }

    @Test
    @DatabaseSetup("/repository/startrek/startrek_issues.xml")
    @ExpectedDatabase(
        value = "/repository/startrek/after/after_update_from_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateExistingWithNullDate() {
        Issue existingIssue = Mockito.mock(Issue.class);
        Mockito.when(issuesClient.get(Mockito.anyString())).thenReturn(existingIssue);
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        startrekService.createTicket(
            new TicketCreationDto()
                .setTicketQueue(TicketQueue.FAILED_TRANSPORTATION)
                .setEntity(new StartrekEntity(15L, StartrekEntityType.TRANSPORTATION))
                .setMessage("Error message")
                .setTags(List.of())
                .setDate(null)
                .setTitle("ErrorTitle")
        );

        Mockito.verify(issuesClient, Mockito.times(0)).create(Mockito.any());
        Mockito.verify(existingIssue).update(captor.capture());

        softly.assertThat(captor.getValue().getComment().get().getComment().get())
            .isEqualTo("Error message");
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/after/after_insert_transportation_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transportationTask() {
        Mockito.when(issuesClient.create(Mockito.any())).thenReturn(
            new Issue(null, null, "KEY-2", "", 0, new EmptyMap<>(), null)
        );

        IssueCreate model = IssueCreate.builder()
            .queue(TicketQueue.FAILED_TRANSPORTATION.getQueueName())
            .summary("ErrorTitle")
            .deadline(null)
            .description("Error message")
            .type("task")
            .tags(new String[]{
                "Tag1",
                "Tag2",
                "Tag with forbidden symbols",
                LONG_TAG.substring(0, StartrekTicketCreationService.MAX_TAG_LENGTH)
            })
            .build();

        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        startrekService.createTicket(
            new TicketCreationDto()
                .setTicketQueue(TicketQueue.FAILED_TRANSPORTATION)
                .setEntity(new StartrekEntity(1L, StartrekEntityType.TRANSPORTATION_TASK))
                .setTags(List.of())
                .setMessage("Error message")
                .setTags(List.of("Tag1", "Tag2", "Tag\twith\nforbidden,symbols", "", LONG_TAG))
                .setTitle("ErrorTitle")
        );
        Mockito.verify(issuesClient).create(captor.capture());
        IssueCreate issueCreate = captor.getValue();
        assertThatModelEquals(model, issueCreate);
    }
}
