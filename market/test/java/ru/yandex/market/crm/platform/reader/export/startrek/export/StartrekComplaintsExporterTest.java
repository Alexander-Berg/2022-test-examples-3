package ru.yandex.market.crm.platform.reader.export.startrek.export;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.mcrm.startrek.support.StartrekResult;
import ru.yandex.market.mcrm.startrek.support.StartrekService;
import ru.yandex.market.mcrm.startrek.support.impl.service.StartrekSecretSupplier;
import ru.yandex.market.mcrm.startrek.support.impl.service.StartrekServiceImpl;
import ru.yandex.startrek.client.Attachments;
import ru.yandex.startrek.client.Comments;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Event;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StartrekComplaintsExporterTest {
    private StartrekComplaintsExporter exporter;
    private Session session;

    @Before
    public void before() {
        StartrekClient client = mock(StartrekClient.class);
        session = mock(Session.class);

        when(client.getSession(notNull())).thenReturn(session);

        StartrekSecretSupplier secretSupplier = new StartrekSecretSupplier("<fake Oauth>");
        StartrekService startrekService = new StartrekServiceImpl(client, secretSupplier, 100, 100);
        exporter = new StartrekComplaintsExporter(startrekService, "BLUEMARKETORDER");
    }

    @Test
    public void exportTest() {
        Issue issue1 = mock(Issue.class);
        when(issue1.getId()).thenReturn("1");
        when(issue1.getUpdatedAt()).thenReturn(Instant.now());

        Issue issue2 = mock(Issue.class);
        when(issue2.getId()).thenReturn("2");
        when(issue2.getUpdatedAt()).thenReturn(Instant.now());

        ListF<Event> events1 = Cf.list(mockEvent("1_event1"));
        ListF<Event> events2 = Cf.list(mockEvent("2_event1"), mockEvent("2_event2"));
        when(issue1.getEvents()).thenReturn(events1.iterator());
        when(issue2.getEvents()).thenReturn(events2.iterator());

        Comments comments = mock(Comments.class);
        when(comments.getAll("1", Cf.arrayList(Comments.Expand.ATTACHMENTS))).thenReturn(Cf.emptyIterator());

        ListF<Comment> allComments2 = new ArrayListF<>(Arrays.asList(mockComment(1), mockComment(2), mockComment(3)));
        when(comments.getAll("2", Cf.arrayList(Comments.Expand.ATTACHMENTS))).thenReturn(allComments2.iterator());

        Attachments attachments = mock(Attachments.class);
        when(attachments.getAll("1")).thenReturn(Cf.emptyIterator());

        ListF<Attachment> allAttachments2 = new ArrayListF<>(Arrays.asList(mockAttachment("11"), mockAttachment("20")));
        when(attachments.getAll("2")).thenReturn(allAttachments2.iterator());

        when(session.comments()).thenReturn(comments);
        when(session.attachments()).thenReturn(attachments);

        ListF<Issue> availableIssues = new ArrayListF<>(Arrays.asList(issue1, issue2));
        Issues issues = mock(Issues.class);
        when(issues.find((String) notNull(), notNull())).thenReturn(availableIssues.iterator());

        when(session.issues()).thenReturn(issues);

        AtomicReference<StartrekResult> startrekResultRef = new AtomicReference<>();
        exporter.doExport(0L, startrekResultRef::set);

        List<String> expectedIssuesIds = Arrays.asList("1", "2", "3");
        Map<Issue, Set<Long>> expectedCommentsIds = ImmutableMap.of(
            issue1, Collections.emptySet(),
            issue2, ImmutableSet.of(1L, 2L, 3L)
        );

        Map<Issue, Set<String>> expectedAttachmentsIds = ImmutableMap.of(
            issue1, Collections.emptySet(),
            issue2, ImmutableSet.of("11", "20")
        );

        check(
            startrekResultRef.get(),
            expectedIssuesIds,
            expectedCommentsIds,
            expectedAttachmentsIds
        );
    }

    private void check(StartrekResult export,
                       List<String> expectedIssuesIds,
                       Map<Issue, Set<Long>> expectedCommentsIds,
                       Map<Issue, Set<String>> expectedAttachmentsIds) {
        Iterator<String> issuesIdsIt = expectedIssuesIds.iterator();
        for (Issue issue : export.getIssues()) {
            Assert.assertEquals(issuesIdsIt.next(), issue.getId());
        }

        for (Map.Entry<Issue, Set<Long>> commentEntry : expectedCommentsIds.entrySet()) {
            List<Comment> comments = export.getComments(commentEntry.getKey());

            Assert.assertEquals(commentEntry.getValue().size(), comments.size());
            for (Comment comment : comments) {
                Assert.assertTrue(commentEntry.getValue().contains(comment.getId()));
            }
        }

        for (Map.Entry<Issue, Set<String>> attachmentEntry : expectedAttachmentsIds.entrySet()) {
            List<Attachment> attachments = export.getAttachments(attachmentEntry.getKey());

            Assert.assertEquals(attachmentEntry.getValue().size(), attachments.size());
            for (Attachment attachment : attachments) {
                Assert.assertTrue(attachmentEntry.getValue().contains(attachment.getId()));
            }
        }
    }

    private Attachment mockAttachment(String id) {
        Attachment attachment = mock(Attachment.class);
        when(attachment.getId()).thenReturn(id);
        return attachment;
    }

    private Comment mockComment(long id) {
        Comment comment = mock(Comment.class);
        when(comment.getId()).thenReturn(id);
        return comment;
    }

    private Event mockEvent(String id) {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(id);
        return event;
    }
}
