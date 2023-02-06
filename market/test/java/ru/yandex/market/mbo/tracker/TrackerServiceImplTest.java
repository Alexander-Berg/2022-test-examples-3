package ru.yandex.market.mbo.tracker;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.excel.ExcelIgnoresConfig;
import ru.yandex.market.mbo.excel.ExcelIgnoresConfigImpl;
import ru.yandex.market.mbo.tracker.client.SummonMaillistClient;
import ru.yandex.market.mbo.tracker.models.TrackerClientData;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mbo.tracker.utils.TrackerServiceHelper;
import ru.yandex.startrek.client.CommentsClient;
import ru.yandex.startrek.client.IssuesClient;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:magicNumber")
public class TrackerServiceImplTest {

    private TrackerService trackerService;
    private Session startrekClient;
    private SummonMaillistClient summonMaillistClient;
    private CommentsClient commentsClient;
    private IssuesClient issuesClient;
    private TrackerClientData clientData;

    @Before
    public void setUp() throws Exception {
        startrekClient = Mockito.mock(Session.class);
        commentsClient = Mockito.mock(CommentsClient.class);
        issuesClient = Mockito.mock(IssuesClient.class);
        summonMaillistClient = Mockito.mock(SummonMaillistClient.class);
        Mockito.when(startrekClient.comments()).thenReturn(commentsClient);
        Mockito.when(startrekClient.issues()).thenReturn(issuesClient);

        clientData = new TrackerClientData(null, null, null, null);
        TrackerServiceHelper trackerServiceHelper = new TrackerServiceHelper("assignee",
            null, null, null,
            null, null);
        ExcelIgnoresConfig ignoresConfig = new ExcelIgnoresConfigImpl(Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet());
        trackerService = new TrackerServiceImpl(clientData, startrekClient, trackerServiceHelper,
            summonMaillistClient, ignoresConfig);
    }

    @Test
    public void commentIncompleteProcessing() {
        Map<String, String> expectedCategoryManagers = ImmutableMap.of(
            "10", "Петр I",
            "12", "Екатерина II",
            "20", "Нет менеджера"
        );

        Issue issue = new IssueMock();
        String offerIdsStr = Stream.of(1L, 2L, 3L, 4L)
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        trackerService.commentIncompleteProcessing(issue, offerIdsStr);

        final ArgumentCaptor<CommentCreate> captor = ArgumentCaptor.forClass(CommentCreate.class);
        Mockito.verify(commentsClient).create((Issue) Mockito.any(), captor.capture());

        String comment = captor.getValue().getComment().get();
        String[] lines = comment.split("\n");

        //проверяем, что строки (исключая первую) содержат одновременно номер категории и имя менеджера
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            expectedCategoryManagers.entrySet().stream()
                .forEach(k -> {
                        String[] phrases = line.split(",");
                        if (phrases[1].contains(k.getKey())) {
                            assertTrue(phrases[3].contains(k.getValue()));
                        }
                    }
                );
        }
    }


    @Test
    public void shouldAddPassedFollowersToTicketFollowers() {
        ReflectionTestUtils.setField(clientData, "followers", Arrays.asList("default1", "default2"));
        Mockito.when(issuesClient.create(Mockito.any())).thenReturn(Mockito.mock(Issue.class));
        ArgumentCaptor<IssueCreate> issueArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);

        trackerService.createTicket("title", "description", "test",
            Arrays.asList("user1", "user2", "default1"), TicketType.MATCHING);

        Mockito.verify(issuesClient, Mockito.times(1))
            .create(issueArgumentCaptor.capture());
        Assertions.assertThat((String[]) issueArgumentCaptor.getValue().getValues().getO("followers")
            .get()).containsExactlyInAnyOrder("user1", "user2", "default1", "default2");
    }
}
