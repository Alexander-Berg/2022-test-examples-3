package ru.yandex.market.wms.servicebus.async.service;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageHeaders;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.wms.common.model.dto.startrek.AttachItem;
import ru.yandex.market.wms.servicebus.api.external.startrek.dto.ConstraintsIssuesDto;
import ru.yandex.market.wms.servicebus.configuration.StartrekConstraintsProperties;
import ru.yandex.startrek.client.Attachments;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.startrek.client.auth.AuthenticationInterceptor;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConstraintsIssueStartrekServiceTest {

    private final StartrekClient startrekClient = mock(StartrekClient.class);
    private final AuthenticationInterceptor interceptor = mock(AuthenticationInterceptor.class);
    private final Issues issues = mock(Issues.class);
    private final Attachments attachments = mock(Attachments.class);

    private final StartrekConstraintsProperties startrekConstraintsProperties = new StartrekConstraintsProperties();
    private final ConstraintsIssueStartrekService underTest =
            new ConstraintsIssueStartrekService(startrekClient, interceptor, startrekConstraintsProperties);

    ConstraintsIssueStartrekServiceTest() {
        startrekConstraintsProperties.setQueue("MMDM");
        startrekConstraintsProperties.setBaseTag("MMDMCellSize");
        startrekConstraintsProperties.setReceivingTag("receiving");
    }

    @Test
    public void createTask() {
        mockStartrek();

        AttachItem item = new AttachItem("cargo_type_issues", "content".getBytes(), "xlsx");
        ConstraintsIssuesDto issue = new ConstraintsIssuesDto("147", "2022-04-11", false, item);
        underTest.receiveConstraintsIssues(issue, new MessageHeaders(null));

        ArgumentCaptor<IssueCreate> issueCaptor = ArgumentCaptor.forClass(IssueCreate.class);

        verify(startrekClient).issues(interceptor);
        verify(issues).create(issueCaptor.capture());
        verify(attachments).upload(eq(item.getFilename()), isA(InputStream.class), isA(ContentType.class));

        IssueCreate issueCreate = issueCaptor.getValue();
        checkIssueCreate(issueCreate, List.of("MMDMCellSize"));
    }

    @Test
    public void createTaskForStorageCategoryIssue() {
        mockStartrek();

        AttachItem item = new AttachItem("cargo_type_issues", "content".getBytes(), "xlsx");
        ConstraintsIssuesDto issue = new ConstraintsIssuesDto("147", "2022-04-11", true, item);
        underTest.receiveConstraintsIssues(issue, new MessageHeaders(null));

        ArgumentCaptor<IssueCreate> issueCaptor = ArgumentCaptor.forClass(IssueCreate.class);

        verify(startrekClient).issues(interceptor);
        verify(issues).create(issueCaptor.capture());
        verify(attachments).upload(eq(item.getFilename()), isA(InputStream.class), isA(ContentType.class));

        IssueCreate issueCreate = issueCaptor.getValue();
        checkIssueCreate(issueCreate, List.of("MMDMCellSize", "receiving"));
    }

    private void mockStartrek() {
        Issue createdIssue = new Issue("1", URI.create("testurl"), "key", "summary", 1L,
                new EmptyMap<>(), null);
        Attachment createdAttachment = mock(Attachment.class);

        when(startrekClient.issues(interceptor)).thenReturn(issues);
        when(startrekClient.attachments(interceptor)).thenReturn(attachments);
        when(issues.create(any())).thenReturn(createdIssue);
        when(attachments.upload(any(), any(), isA(ContentType.class))).thenReturn(createdAttachment);
    }

    private void checkIssueCreate(IssueCreate issueCreate, List<String> tags) {
        assertThat(issueCreate.getValues()).containsAllEntriesOf(Map.of(
                "type", "task",
                "queue", "MMDM",
                "description", "Не удалось разместить товары из-за несоответствия карготипов. Список товаров во " +
                        "вложении",
                "summary",
                "147: корректировка разметки карготипами, 2022-04-11"
        ));

        Optional<Object> optionalTags = issueCreate.getValues().getOptional("tags");
        assertThat(optionalTags).containsInstanceOf(List.class);
        assertThat((List<String>) optionalTags.get())
                .containsExactlyInAnyOrderElementsOf(tags);
    }
}
