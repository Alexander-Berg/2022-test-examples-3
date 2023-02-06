package ru.yandex.market.deepmind.tracker_approver.utils;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.NotImplementedException;
import org.mockito.Mockito;

import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

public class SessionUtils {
    private SessionUtils() {
    }

    public static Session create() {
        AtomicInteger ticketId = new AtomicInteger(0);

        var session = Mockito.mock(Session.class, Mockito.withSettings()
            .defaultAnswer(__ -> {
                throw new NotImplementedException("");
            }));
        var issues = Mockito.mock(Issues.class, Mockito.withSettings()
            .defaultAnswer(__ -> {
                throw new NotImplementedException("");
            }));

        Mockito.doReturn(issues).when(session).issues();
        Mockito.doAnswer(invok -> {
            IssueCreate issueCreate = invok.getArgument(0);
            var values = issueCreate.getValues();
            var queue = (String) values.get("queue");

            var id = ticketId.incrementAndGet();
            var issue = Mockito.mock(Issue.class);
            Mockito.doReturn(queue + "-" + id).when(issue).getKey();

            return issue;
        })
            .when(issues).create(Mockito.any());

        return session;
    }
}
