package ru.yandex.mail.micronaut.http_logger;

import lombok.val;
import one.util.streamex.StreamEx;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.error.ElementsShouldSatisfy.UnsatisfiedRequirement;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.error.ElementsShouldSatisfy.elementsShouldSatisfyAny;
import static ru.yandex.mail.micronaut.http_logger.LogUtils.findAppenders;

final class LogAssertions extends AbstractAssert<LogAssertions, Logger> {
    @FunctionalInterface
    public interface Assertion {
        LogEventAssertions check(LogEvent event);
    }

    private final List<ListAppender> appenders;

    private LogAssertions(Logger log) {
        super(log, LogAssertions.class);
        this.appenders = findAppenders(log, ListAppender.class);
    }

    public static LogAssertions assertThat(Logger log) {
        return new LogAssertions(log);
    }

    private static Optional<UnsatisfiedRequirement> isSatisfies(LogEvent event, Assertion assertion) {
        try {
            assertion.check(event);
            return Optional.empty();
        } catch (AssertionError e) {
            return Optional.of(new UnsatisfiedRequirement(event.getMessage().getFormattedMessage(), e.getMessage()));
        }
    }

    public LogAssertions containsEventSatisfying(Assertion assertions) {
        val events = StreamEx.of(appenders)
            .flatCollection(ListAppender::getEvents)
            .toImmutableList();

        if (events.isEmpty()) {
            failWithMessage("Log events not found");
        }

        val checkResults = StreamEx.of(events)
            .map(event -> isSatisfies(event, assertions))
            .toImmutableList();

        if (checkResults.stream().noneMatch(Optional::isEmpty)) {
            val unsatisfiedRequirements = StreamEx.of(checkResults)
                .flatMap(StreamEx::of)
                .toImmutableList();

            failWithMessage(elementsShouldSatisfyAny(actual, unsatisfiedRequirements, info).create());
        }

        return this;
    }
}
