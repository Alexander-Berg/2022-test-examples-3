package ru.yandex.market.hrms.core.service.startrek;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.Transition;

@RequiredArgsConstructor
public class IssueGetAnswer implements Answer<Issue> {
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

        Transition transition = Mockito.mock(Transition.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(transition.getTo().getKey())
                .thenReturn("closed");

        Mockito.when(issue.getTransitions()).thenReturn(Cf.wrap(List.of(
                transition
        )));

        Mockito.when(issue.executeTransition(Mockito.any(Transition.class), Mockito.any(IssueUpdate.class)))
                .thenAnswer((inv) -> {
                    return session.transitions().execute(
                            issue,
                            inv.<Transition>getArgument(0),
                            inv.getArgument(1)
                    );
                });

        return issue;
    }
}
