package ru.yandex.mail.micronaut.http_logger;

import lombok.val;
import one.util.streamex.StreamEx;
import org.apache.logging.log4j.core.LogEvent;
import org.assertj.core.api.AbstractAssert;
import org.intellij.lang.annotations.Language;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.util.function.Predicate.not;

class LogEventAssertions extends AbstractAssert<LogEventAssertions, LogEvent> {
    private final List<String> lines;

    private String getMessage() {
        return actual.getMessage().getFormattedMessage();
    }

    private LogEventAssertions(LogEvent logEvent) {
        super(logEvent, LogEventAssertions.class);
        lines = StreamEx.of(getMessage().split("\n"))
            .filter(not(String::isBlank))
            .toImmutableList();
    }

    public static LogEventAssertions assertThat(LogEvent event) {
        return new LogEventAssertions(event);
    }

    public LogEventAssertions containsLine(String line) {
        if (!lines.contains(line)) {
            failWithMessage("%nExpecting:%n <%s>%nto contain line:%n <%s>", getMessage(), line);
        }

        return this;
    }

    public LogEventAssertions doesNotContainLine(String line) {
        if (lines.contains(line)) {
            failWithMessage("%nExpecting:%n <%s>%ndoes not contain:%n <%s>", getMessage(), line);
        }

        return this;
    }

    public LogEventAssertions containsLineMatching(@Language("RegExp") String regex) {
        return containsLineMatching(Pattern.compile(regex));
    }

    public LogEventAssertions containsLineMatching(Pattern regex) {
        if (lines.stream().noneMatch(regex.asMatchPredicate())) {
            failWithMessage("%nExpecting:%n <%s>%nto contain line matching:%n <%s>", getMessage(), regex);
        }

        return this;
    }

    public LogEventAssertions doesNotContainLineMatching(@Language("RegExp") String regex) {
        return doesNotContainLineMatching(Pattern.compile(regex));
    }

    public LogEventAssertions doesNotContainLineMatching(Pattern regex) {
        if (lines.stream().anyMatch(regex.asMatchPredicate())) {
            failWithMessage("%nExpecting:%n <%s>%ndoes not contain line matching:%n <%s>", getMessage(), regex);
        }

        return this;
    }

    public LogEventAssertions containsThrowableSatisfying(Consumer<Throwable> assertion) {
        org.assertj.core.api.Assertions.assertThat(actual.getThrown())
            .describedAs("Expecting %s to contain throwable, but it doesn't", actual)
            .isNotNull();
        assertion.accept(actual.getThrown());
        return this;
    }

    public LogEventAssertions doesNotContainThrowable() {
        val thrown = actual.getThrown();
        org.assertj.core.api.Assertions.assertThat(thrown)
            .describedAs("Expecting %s not to contain throwable, but it contains %s", actual, thrown)
            .isNull();
        return this;
    }
}
