package ru.yandex.market.tsum.core.pollers;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.market.tsum.clients.pollers.PollerOptions;
import ru.yandex.market.tsum.core.notify.common.startrek.Status;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.StatusRef;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class StartrekPollerTest {
    private StartrekPoller poller;

    @Before
    public void setUp() throws Exception {
        Stream<String> statusStream = Stream.concat(
            Stream.of(Status.values()).filter(status -> status != Status.CLOSED).map(Status::getApiStatus),
            Stream.of("bogus-status", Status.CLOSED.getApiStatus()));

        List<String> statusOrder = statusStream.collect(Collectors.toCollection(LinkedList::new));

        Issues issues = mock(Issues.class);
        Issue issue = mock(Issue.class);
        StatusRef statusRef = mock(StatusRef.class);
        when(statusRef.getKey()).then((Answer<String>) invocation -> statusOrder.remove(0));
        when(issue.getStatus()).thenReturn(statusRef);
        when(issues.get(any(Issue.class))).thenReturn(issue);

        poller = new StartrekPoller(issues, PollerOptions.builder(),
            duration -> {
                // no-op
            });
    }

    @Test
    public void poll() throws TimeoutException, InterruptedException {
        poller.pollIssue(mock(Issue.class), Status.CLOSED);
    }
}
