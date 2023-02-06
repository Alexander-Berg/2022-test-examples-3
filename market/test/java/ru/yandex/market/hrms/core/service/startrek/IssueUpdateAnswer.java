package ru.yandex.market.hrms.core.service.startrek;

import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

@RequiredArgsConstructor
public class IssueUpdateAnswer implements Answer<Issue> {
    private final Session session;

    @Override
    public Issue answer(InvocationOnMock invocation) throws Throwable {
        String key = invocation.getArgument(0);
        Issue issue = Mockito.mock(Issue.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(issue.getId()).thenReturn(key);
        Mockito.when(issue.getKey()).thenReturn(key);
        Mockito.when(issue.getQueue().getKey()).thenReturn("HRMS");
        Mockito.when(issue.getStatus().getKey()).thenReturn("new");
        Mockito.when(issue.getStatus().getDisplay()).thenReturn("Новый");

        return issue;
    }
}
