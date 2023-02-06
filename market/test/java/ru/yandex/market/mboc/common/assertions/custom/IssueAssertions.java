package ru.yandex.market.mboc.common.assertions.custom;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import ru.yandex.market.mbo.tracker.utils.IssueUtils;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.UserRef;

/**
 * @author s-ermakov
 */
public class IssueAssertions extends AbstractAssert<IssueAssertions, Issue> {

    public IssueAssertions(Issue actual) {
        super(actual, IssueAssertions.class);
    }

    public static IssueAssertions assertThat(Issue actual) {
        return new IssueAssertions(actual);
    }

    public IssueAssertions isOpen() {
        isNotNull();

        boolean open = IssueUtils.isOpen(actual);
        if (!open) {
            failWithMessage("Expected issue '%s' to be in open. Actual '%s'.",
                actual.getKey(), actual.getStatus().getKey());
        }

        return this;
    }

    public IssueAssertions isResolved() {
        isNotNull();

        boolean resolved = IssueUtils.isResolved(actual);
        if (!resolved) {
            failWithMessage("Expected issue '%s' to be in resolved. Actual '%s'.",
                actual.getKey(), actual.getStatus().getKey());
        }

        return this;
    }

    public IssueAssertions isClosed() {
        isNotNull();

        boolean closed = IssueUtils.isClosed(actual);
        if (!closed) {
            failWithMessage("Expected issue '%s' to be in closed. Actual '%s'.",
                actual.getKey(), actual.getStatus().getKey());
        }

        return this;
    }

    public IssueAssertions hasSummaryEqualTo(String expected) {
        isNotNull();

        if (!Objects.equals(expected, actual.getSummary())) {
            failWithMessage("Expected issue '%s' to have summary '%s'. Actual '%s'.",
                actual.getKey(), expected, actual.getSummary());
        }

        return this;
    }

    public IssueAssertions hasExactlyTagsInAnyOrder(String... tags) {
        isNotNull();
        Assertions.assertThat(actual.getTags()).containsExactlyInAnyOrder(tags);
        return this;
    }

    public IssueAssertions hasFollowers(List<String> expected) {
        isNotNull();
        List<String> actualFollowers = actual.getFollowers()
            .stream()
            .map(UserRef::getLogin)
            .collect(Collectors.toList());

        if (!actualFollowers.containsAll(expected)) {
            failWithMessage("Expected issue '%s' to have followers '%s'. Actual '%s'.",
                actual.getKey(), String.join(",", expected), String.join(",", actualFollowers));
        }
        return this;
    }
}
