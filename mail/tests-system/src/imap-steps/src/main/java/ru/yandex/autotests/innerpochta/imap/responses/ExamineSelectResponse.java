package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import org.hamcrest.Matcher;

import ru.yandex.qatools.allure.annotations.Step;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ExamineSelectResponse<T extends ImapResponse<T>> extends ImapResponse<T> {

    public static final String SELECT_FOLDER_ENCODING_ERROR = "[CLIENTBUG] SELECT Folder encoding error.";
    public static final String EXAMINE_FOLDER_ENCODING_ERROR = "[CLIENTBUG] EXAMINE Folder encoding error.";
    public static final String NO_SUCH_FOLDER = "SELECT No such folder.";
    private final List<String> requiredResponseCodes;
    private Iterable<String> flags = null;
    private Integer exists = null;
    private Integer recent = null;
    private Integer firstUnseenMessageId = null;
    private Iterable<String> permanentFlags = null;
    private Integer uidNext = null;
    private Integer uidValidity = null;

    public ExamineSelectResponse(String... responseCodes) {
        requiredResponseCodes = Arrays.asList(responseCodes);
    }

    @Override
    protected void parse(String line) {
        parseFlags(line);
        parseExists(line);
        parseRecent(line);
        parseUnseen(line);
        parsePermanentFlags(line);
        parseUidNext(line);
        parseUidValidity(line);
    }

    @Override
    protected void validate() {
        assertThat("В ответе нет строки FLAGS", flags, is(notNullValue()));
        assertThat("В ответе нет строки EXISTS", exists, is(notNullValue()));
        assertThat("В ответе нет строки RECENT", recent, is(notNullValue()));
        assertThat("В ответе нет строки PERMANENTFLAGS", permanentFlags, is(notNullValue()));
        assertThat("В ответе нет строки UIDNEXT", uidNext, is(notNullValue()));
        assertThat("В ответе нет строки UIDVALIDITY", uidValidity, is(notNullValue()));
    }

    private void parseFlags(String line) {
        java.util.regex.Matcher matcher = Pattern.compile("(?i)^\\* FLAGS \\((.*)\\)$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки FLAGS", flags, is(nullValue()));
            flags = Splitter.on(' ').split(matcher.group(1));
        }
    }

    private void parseExists(String line) {
        java.util.regex.Matcher matcher = Pattern.compile("(?i)^\\* ([0-9]*) EXISTS$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки EXISTS", exists, is(nullValue()));
            exists = Integer.valueOf(matcher.group(1));
        }
    }

    private void parseRecent(String line) {
        java.util.regex.Matcher matcher = Pattern.compile("(?i)^\\* ([0-9]*) RECENT$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки RECENT", recent, is(nullValue()));
            recent = Integer.valueOf(matcher.group(1));
        }
    }

    private void parseUnseen(String line) {
        java.util.regex.Matcher matcher = Pattern.compile("(?i)^\\* OK \\[UNSEEN ([0-9]*)](.*)$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки UNSEEN", firstUnseenMessageId, is(nullValue()));
            firstUnseenMessageId = Integer.valueOf(matcher.group(1));
        }
    }

    private void parsePermanentFlags(String line) {
        java.util.regex.Matcher matcher = Pattern.compile("(?i)^\\* OK \\[PERMANENTFLAGS \\((.*)\\)](.*)$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки PERMANENTFLAGS", permanentFlags, is(nullValue()));
            permanentFlags = Splitter.on(' ').split(matcher.group(1));
        }
    }

    private void parseUidNext(String line) {
        java.util.regex.Matcher matcher = Pattern.compile("(?i)^\\* OK \\[UIDNEXT ([0-9]*)](.*)$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки UIDNEXT", uidNext, is(nullValue()));
            uidNext = Integer.valueOf(matcher.group(1));
        }
    }

    private void parseUidValidity(String line) {
        java.util.regex.Matcher matcher = Pattern.compile("(?i)^\\* OK \\[UIDVALIDITY ([0-9]*)](.*)$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки UIDVALIDITY", uidValidity, is(nullValue()));
            uidValidity = Integer.valueOf(matcher.group(1));
        }
    }

    @Step("FLAGS should be {0}")
    public T flagsShould(Matcher<Iterable<String>> matcher) {
        assertThat(flags, matcher);
        return (T) this;
    }

    @Step("Должны увидеть флаги - {0}")
    public T shouldContainFlags(Flags[] expectedFlags) {
        for (Flags flag : expectedFlags) {
            assertThat(flags, hasItem(flag.value()));
        }
        return (T) this;
    }

    @Step("Должны обнаружить {0} в EXISTS")
    public T existsShouldBe(int value) {
        assertThat(format("Мы должны были обнаружить %d сообщений в строке EXISTS", value),
                exists, is(value));
        return (T) this;
    }

    @Step("RECENT should contain {0}")
    public T recentShouldBe(int value) {
        assertThat(recent, is(value));
        return (T) this;
    }

    @Step("UNSEEN should be {0}")
    public T firstUnseenMessageIdShouldBe(int value) {
        assertThat(firstUnseenMessageId, is(value));
        return (T) this;
    }

    @Step("PERMANENTFLAGS should be {0}")
    public T permanentFlagsShould(Matcher<Iterable<String>> matcher) {
        assertThat(permanentFlags, matcher);
        return (T) this;
    }

    @Step("UIDNEXT should be {0}")
    public T uidNextShouldBe(int value) {
        assertThat(uidNext, is(value));
        return (T) this;
    }

    @Step("UIDVALIDITY should be {0}")
    public T uidValidityShouldBe(int value) {
        assertThat(uidValidity, is(value));
        return (T) this;
    }

    @Step("Возвращаем значение EXISTS")
    public Integer exist() {
        return exists;
    }

    @Step("Возвращаем значение RECENT")
    public Integer recent() {
        return recent;
    }

    @Step
    public Integer firstUnseenMessageId() {
        return firstUnseenMessageId;
    }

    @Step("Возвращаем значение UIDNEXT")
    public Integer uidNext() {
        return uidNext;
    }

    @Step("Возвращаем значение UIDVALIDITY")
    public Integer uidValidity() {
        return uidValidity;
    }

    public static enum Flags {
        ANSWERED("\\Answered"),
        SEEN("\\Seen"),
        DRAFT("\\Draft"),
        DELETED("\\Deleted"),
        FORWARDED("$Forwarded");

        private String value;

        private Flags(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
