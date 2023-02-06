package ru.yandex.market.tsum.clients.startrek;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.StatusRef;
import ru.yandex.startrek.client.model.UserRef;

import static org.mockito.Mockito.mock;

public class IssueBuilder {
    private final String key;

    private String status = "open";
    private String display;
    private String assigneeNickName;
    private String assigneeName;

    private IssueBuilder(String key) {
        this.key = key;
    }

    public static IssueBuilder newBuilder(String key) {
        return new IssueBuilder(key);
    }

    public IssueBuilder setStatus(String status) {
        this.status = status;
        return this;
    }

    public IssueBuilder setDisplay(String display) {
        this.display = display;
        return this;
    }

    public IssueBuilder setAssignee(String assigneeNickName, String assigneeName) {
        this.assigneeNickName = assigneeNickName;
        this.assigneeName = assigneeName;
        return this;
    }

    public Issue build() {
        return createIssue(key, display,
            ImmutableMap.<String, Object>builder()
                .put("status", statusRef(status))
                .put("assignee", assignee(assigneeNickName, assigneeName))
                .build());
    }

    private static Issue createIssue(String key, String display, Map<String, Object> values) {
        long versionId = 0L;
        URI uri = URI.create("https://st.yandex-team.ru/" + key);
        return new Issue(key, uri, key, display, versionId, DefaultMapF.wrap(values), mock(Session.class));
    }

    private static Option<UserRef> assignee(String assigneeNickName, String assigneeName) {
        if (assigneeNickName == null || assigneeName == null) {
            return Option.empty();
        }

        ImmutableMap<String, Object> map = ImmutableMap.of(
            "id", assigneeNickName,
            "display", assigneeName
        );

        try {
            return Option.of(StartrekApiObjectMapper.get()
                .readValue(new TreeTraversingParser(StartrekApiObjectMapper.get().valueToTree(map)), UserRef.class));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static StatusRef statusRef(String status) {
        long id = 0;
        ImmutableMap<String, Object> map = ImmutableMap.of(
            "id", id,
            "key", status,
            "display", status
        );

        try {
            return StartrekApiObjectMapper.get()
                .readValue(new TreeTraversingParser(StartrekApiObjectMapper.get().valueToTree(map)), StatusRef.class);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
