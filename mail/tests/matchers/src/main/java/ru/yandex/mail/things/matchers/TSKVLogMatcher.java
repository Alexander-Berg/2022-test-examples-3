package ru.yandex.mail.things.matchers;

import ch.ethz.ssh2.StreamGobbler;
import org.apache.log4j.Logger;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.mail.common.execute.Execute;
import ru.yandex.mail.common.execute.Shell;
import ru.yandex.mail.common.user_journal.generated.UserJournalType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Splitter.onPattern;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.log4j.Logger.getLogger;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

public class TSKVLogMatcher extends TypeSafeMatcher<Execute> {
    private static Logger LOG = getLogger(TSKVLogMatcher.class);

    private String fileToGrep;
    private List<String> patterns;
    private Map<String, String> logMap = new HashMap<>();
    private Matcher<Map<? extends String, ? extends String>> matcher;
    private Integer entry = 0;
    private long timeoutMs = 0;

    // Почему тут не лист матчеров? спроси @massaraksh
    private TSKVLogMatcher(Matcher<Map<? extends String, ? extends String>> matchers,
                           String fileToGrep, String pattern, long timeoutMs) {
        this.matcher = matchers;
        this.fileToGrep = fileToGrep;
        this.patterns = Arrays.asList(pattern);
        this.timeoutMs = timeoutMs;
    }

    private TSKVLogMatcher(Matcher<Map<? extends String, ? extends String>> matchers,
                           String fileToGrep, List<String> patterns, long timeoutMs) {
        this.matcher = matchers;
        this.fileToGrep = fileToGrep;
        this.patterns = patterns;
        this.timeoutMs = timeoutMs;
    }

    private TSKVLogMatcher(Matcher<Map<? extends String, ? extends String>> matchers,
                           String fileToGrep, String pattern, Integer entry, long timeoutMs) {
        this.matcher = matchers;
        this.fileToGrep = fileToGrep;
        this.patterns = Arrays.asList(pattern);
        this.entry = entry;
        this.timeoutMs = timeoutMs;
    }

    private TSKVLogMatcher(Matcher<Map<? extends String, ? extends String>> matchers,
                           String fileToGrep, List<String> patterns, Integer entry, long timeoutMs) {
        this.matcher = matchers;
        this.fileToGrep = fileToGrep;
        this.patterns = patterns;
        this.entry = entry;
        this.timeoutMs = timeoutMs;
    }

    @Factory
    public static TSKVLogMatcher logEntryShouldMatch(Matcher<Map<? extends String, ? extends String>> matchers,
                                                     String fileToGrep, String pattern, long timeoutMs) {
        return new TSKVLogMatcher(matchers, fileToGrep, pattern, timeoutMs);
    }

    @Factory
    public static TSKVLogMatcher logEntryShouldMatch(Matcher<Map<? extends String, ? extends String>> matchers,
                                                     String fileToGrep, List<String> patterns, long timeoutMs) {
        return new TSKVLogMatcher(matchers, fileToGrep, patterns, timeoutMs);
    }

    @Factory
    public static TSKVLogMatcher logEntryShouldMatch(Matcher<Map<? extends String, ? extends String>> matchers,
                                                     String fileToGrep, String pattern, Integer entry, long timeoutMs) {
        return new TSKVLogMatcher(matchers, fileToGrep, pattern, entry, timeoutMs);
    }

    @Factory
    public static TSKVLogMatcher logEntryShouldMatch(Matcher<Map<? extends String, ? extends String>> matchers,
                                                     String fileToGrep, List<String> patterns, Integer entry, long timeoutMs) {
        return new TSKVLogMatcher(matchers, fileToGrep, patterns, entry, timeoutMs);
    }

    private static Map<String, String> parse(String line) {
        return onPattern("\\s+").omitEmptyStrings().splitToList(line).stream()
                .map(o -> o.split("=", 2))
                .collect(toMap(
                        arr -> arr[0],
                        arr -> arr.length > 1 ? arr[1] : ""
                ));
    }

    private static ArrayList<String> fgrep(Execute execute, Logger log, String pattern, String logToGrep, long timeoutMs) throws IOException {
        ArrayList<String> result = new ArrayList<String>();
        try (Shell shell = execute.shell()) {
            shell.exec("fgrep " + pattern + " " + logToGrep, timeoutMs);
            InputStream stdout = new StreamGobbler(shell.getStdout());

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.contains(pattern)) {
                    result.add(line + "\n");
                }
            }
        } catch (Exception e) {
            log.error("cannot execute fgrep", e);
        }

        return result;
    }

    @Override
    protected boolean matchesSafely(Execute execute) {
        try {
            final String firstPattern = patterns.isEmpty() ? "" : patterns.get(0);
            ArrayList<String> logs = fgrep(execute, LOG, firstPattern, fileToGrep, timeoutMs);
            patterns.stream().skip(1).forEach(pattern -> {
                logs.removeIf(log -> !log.contains(pattern));
            });
            logMap = logs.isEmpty() ? emptyMap() : parse(logs.get(entry));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return matcher.matches(logMap);
    }

    @Override
    public void describeTo(Description description) {
        description.appendDescriptionOf(matcher);
        description.appendText("\nПолученные в лог записи: \n").appendText(logMap.toString());
    }

    public static Matcher<Map<? extends String, ? extends String>> entry(UserJournalType key, Matcher<String> value) {
        return hasEntry(is(key.toString()), value);
    }
}
