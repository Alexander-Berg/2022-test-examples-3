package ru.yandex.market.mbo.tracker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.joda.time.Instant;
import org.joda.time.LocalDate;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.PriorityRef;
import ru.yandex.startrek.client.model.StatusRef;
import ru.yandex.startrek.client.model.Transition;
import ru.yandex.startrek.client.model.UserRef;

/**
 * @author s-ermakov
 */
public class IssueMock extends Issue {
    private static final String DEFAULT_PRIORITY = "normal";

    private String key;
    private String summary;
    private String description;
    private String[] tags;
    private String[] followers;
    private IssueStatus status = IssueStatus.OPEN;
    private String author;
    private String assignee;
    private Instant createdAt;
    private Map<String, Object> fields = new HashMap<>();
    private Option<LocalDate> deadline;
    private String priority;

    public IssueMock() {
        super(null, null, null, null, 0, new OpenHashMap<>(), null);
    }

    public IssueMock setAuthor(String author) {
        this.author = author;
        return this;
    }

    @Override
    public UserRef getCreatedBy() {
        return new UserRef(author, null, "", null) {
        };
    }

    @Override
    public Option<UserRef> getAssignee() {
        UserRef userRef = new UserRef(assignee, null, "", null) {
        };
        return Option.of(userRef);
    }

    public IssueMock setAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    public IssueMock setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public String getKey() {
        return key;
    }

    public IssueMock setKey(String key) {
        this.key = key;
        return this;
    }

    @Override
    public ListF<String> getTags() {
        return new ArrayListF<>(Arrays.asList(tags));
    }

    public IssueMock setTags(String... tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public StatusRef getStatus() {
        return new StatusRef(0, null, status.getIssueKey(), null, null) {
        };
    }

    public IssueStatus getIssueStatus() {
        return this.status;
    }

    public IssueMock setIssueStatus(IssueStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    public IssueMock setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    @Override
    public Option<String> getDescription() {
        return Option.ofNullable(description);
    }

    public IssueMock setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public ListF<UserRef> getFollowers() {
        return Cf.list(followers).map(f -> new UserRef(f, null, f, null) {
        });
    }

    public IssueMock setFollowers(String[] followers) {
        this.followers = followers;
        return this;
    }

    @Override
    public PriorityRef getPriority() {
        return new PriorityRef(0, null, priority == null ? DEFAULT_PRIORITY : priority,
            null, null) {
        };
    }

    public IssueMock setPriority(String priority) {
        this.priority = priority;
        return this;
    }

    public void setCustomField(String key, Object value) {
        fields.put(key, value);
    }

    public Object getCustomField(String key) {
        return fields.get(key);
    }

    @Override
    public Option<LocalDate> getDeadline() {
        return deadline;
    }

    public IssueMock setDeadline(Option<LocalDate> deadline) {
        this.deadline = deadline;
        return this;
    }

    @Override
    public ListF<Transition> getTransitions() {
        return Arrays.stream(IssueStatus.values())
            .map(IssueStatus::getTransitionKey)
            .map(st -> new Transition(st, null, null, Option.empty(), null))
            .collect(Collectors.toCollection(ArrayListF::new));
    }

    @Override
    public ListF<Transition> executeTransition(Transition transition) {
        setIssueStatus(IssueStatus.fromTransitionKey(transition.getId()));
        return getTransitions();
    }

    @Override
    public ListF<Transition> executeTransition(Transition transition, IssueUpdate update) {
        setIssueStatus(IssueStatus.fromTransitionKey(transition.getId()));
        return getTransitions();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IssueMock issueMock = (IssueMock) o;
        return Objects.equals(key, issueMock.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "IssueMock{" +
            "key='" + key + '\'' +
            '}';
    }
}
