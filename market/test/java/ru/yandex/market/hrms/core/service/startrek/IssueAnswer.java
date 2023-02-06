package ru.yandex.market.hrms.core.service.startrek;

import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

@RequiredArgsConstructor
public class IssueAnswer implements Answer<Issue> {
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Session session;
    private final String queueName;

    private String summary;

    @Override
    public Issue answer(InvocationOnMock invocation) throws Throwable {
        long id = idCounter.getAndIncrement();
        Issue issue = Mockito.mock(Issue.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(issue.getId()).thenReturn(String.valueOf(id));
        Mockito.when(issue.getKey()).thenReturn(String.format("%s-%s", queueName, id));
        Mockito.when(issue.getQueue().getKey()).thenReturn(queueName);
        Mockito.when(issue.getStatus().getKey()).thenReturn("approved");
        Mockito.when(issue.getStatus().getDisplay()).thenReturn("Утвержден");
        if (summary != null) {
            Mockito.when(issue.getSummary()).thenReturn(summary);
        }

        return issue;
    }

    public IssueAnswer withSummary(String summary) {
        this.summary = summary;
        return this;
    }
}
