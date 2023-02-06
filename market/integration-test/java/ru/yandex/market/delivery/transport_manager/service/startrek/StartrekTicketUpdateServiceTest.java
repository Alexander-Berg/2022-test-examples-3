package ru.yandex.market.delivery.transport_manager.service.startrek;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.io.Charsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.Attachment;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.update.TicketUpdateDto;
import ru.yandex.market.delivery.transport_manager.service.ticket.StartrekTicketUpdateService;
import ru.yandex.startrek.client.AttachmentsClient;
import ru.yandex.startrek.client.IssuesClient;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;

@DatabaseSetup("/repository/startrek/startrek_issues.xml")
public class StartrekTicketUpdateServiceTest extends AbstractContextualTest {
    @Autowired
    private StartrekTicketUpdateService updateService;

    @Autowired
    private StartrekClient startrekClient;

    @Autowired
    private IssuesClient issuesClient;

    @Autowired
    private AttachmentsClient attachmentsClient;

    @BeforeEach
    void setUp() {
        Mockito.when(startrekClient.issues(Mockito.any())).thenReturn(issuesClient);
        Mockito.when(startrekClient.attachments(Mockito.any())).thenReturn(attachmentsClient);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/after/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void update() {
        Issue existingIssue = Mockito.mock(Issue.class);
        Mockito.when(issuesClient.get(Mockito.anyString())).thenReturn(existingIssue);
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);

        byte[] byteAttachment = new byte[20];
        new Random(20).nextBytes(byteAttachment);

        ru.yandex.startrek.client.model.Attachment attachment =
            Mockito.mock(ru.yandex.startrek.client.model.Attachment.class);
        Mockito.when(attachment.getId()).thenReturn("1");


        Mockito.when(attachmentsClient.upload(
            Mockito.anyString(),
            Mockito.any(InputStream.class),
            Mockito.any(Charset.class))
        ).thenReturn(attachment);

        updateService.updateTicket(
            new TicketUpdateDto()
                .setIssueId(1L)
                .setHash("hash123")
                .setComment("update comment")
                .setAttachments(List.of(
                    new Attachment().setName("New file").setContent(byteAttachment).setEncoding(Charsets.UTF_8.name())
                ))
        );

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> inputCaptor = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<Charset> charsetCaptor = ArgumentCaptor.forClass(Charset.class);


        Mockito.verify(attachmentsClient, Mockito.times(1))
            .upload(nameCaptor.capture(), inputCaptor.capture(), charsetCaptor.capture());

        Mockito.verify(issuesClient, Mockito.times(0)).create(Mockito.any());
        Mockito.verify(existingIssue).update(captor.capture());

        softly.assertThat(captor.getValue().getComment().get().getComment().get())
            .isEqualTo("update comment");
        ArrayListF<String> attachmentIds = new ArrayListF<>();
        attachmentIds.add("1");
        softly.assertThat(captor.getValue().getComment().get().getAttachments())
            .isEqualTo(attachmentIds);
    }

    @Test
    void noUpdateSameHash() {
        Issue existingIssue = Mockito.mock(Issue.class);
        Mockito.when(issuesClient.get(Mockito.anyString())).thenReturn(existingIssue);


        updateService.updateTicket(
            new TicketUpdateDto()
                .setIssueId(1L)
                .setComment("update comment")
        );

        Mockito.verify(existingIssue, Mockito.times(0)).update(Mockito.any());
    }
}
