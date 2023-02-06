package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public final class StatusResponse extends ImapResponse<StatusResponse> {

    public static final String NO_SUCH_FOLDER = "[TRYCREATE] STATUS No such folder.";

    private static final Pattern STATUS_PATTERN = Pattern.compile("(?i)^\\* STATUS (\\S*) \\((.*)\\)$");
    private final Map<String, Integer> items = new HashMap<>();
    private String folderName = null;

    @Override
    protected void parse(String line) {
        parseStatus(line);
    }

    @Override
    protected void validate() {
        // TODO: проверить folderName и items по запросу
    }

    private void parseStatus(String line) {
        Matcher matcher = STATUS_PATTERN.matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки STATUS", folderName, is(nullValue()));

            folderName = matcher.group(1);

            String[] splitted = matcher.group(2).split(" ");
            assertThat("В строке STATUS в скобках нечётное количество строк", splitted.length % 2, is(0));

            for (int i = 0; i < splitted.length; i += 2) {
                items.put(splitted[i].toUpperCase(), Integer.valueOf(splitted[i + 1]));
            }
        }
    }

    @Step("Получаем количество сообщений")
    public int numberOfMessages() {
        return items.get("MESSAGES");
    }

    @Step
    public int numberOfRecentMessages() {
        return items.get("RECENT");
    }

    @Step
    public int uidNext() {
        return items.get("UIDNEXT");
    }

    @Step
    public int uidValidity() {
        return items.get("UIDVALIDITY");
    }

    @Step
    public int numberOfUnseenMessages() {
        return items.get("UNSEEN");
    }

    @Step("Ожидаем, что количество сообщений будет равно {0}")
    public StatusResponse numberOfMessagesShouldBe(int value) {
        assertThat("Неверное количество сообщений", numberOfMessages(), is(value));
        return this;
    }

    @Step("Количество последних (RECENT) сообщений должно быть {0}")
    public StatusResponse numberOfRecentMessagesShouldBe(int value) {
        assertThat("Неверное количество последних сообщений", numberOfRecentMessages(), is(value));
        return this;
    }

    @Step("next uid should be {0}")
    public StatusResponse uidNextShouldBe(int value) {
        assertThat(uidNext(), is(value));
        return this;
    }

    @Step("uid validity should be {0}")
    public StatusResponse uidValidityShouldBe(int value) {
        assertThat(uidValidity(), is(value));
        return this;
    }

    @Step("number of unseen messages should be {0}")
    public StatusResponse numberOfUnseenMessagesShouldBe(int value) {
        assertThat(numberOfUnseenMessages(), is(value));
        return this;
    }
}
