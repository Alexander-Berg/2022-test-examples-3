package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.ArrayList;
import java.util.List;

import ru.lanwen.verbalregex.VerbalExpression;

import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.base.Splitter.on;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.lanwen.verbalregex.VerbalExpression.regex;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

public final class SearchResponse extends ImapResponse<SearchResponse> {
    public static final String NO_MESSAGES = "SEARCH Completed (no messages).";
    public static final String WRONG_SESSION_STATE = "[CLIENTBUG] SEARCH Wrong session state for command";
    public static final String UID_SEARCH_WRONG_SESSION_STATE = "[CLIENTBUG] UID SEARCH Wrong session state for command";
    public static final String BADCHARSET = "[BADCHARSET] SEARCH Unsupported text encoding.";

    private List<String> messageIds = new ArrayList<>();
    private Boolean untaggedResp = false;

    @Override
    protected void parse(String line) {
        parseSearch(line);
    }

    @Override
    protected void validate() {
        assertThat("В ответе нет строки SEARCH", messageIds, is(notNullValue()));
    }

    private void parseSearch(String line) {
        VerbalExpression testRegex = regex()
                .startOfLine().then("* SEARCH ").capt().anything().endCapt().endOfLine().build();

        VerbalExpression untaggedRespRegex = regex()
                .startOfLine().then("* SEARCH").endOfLine().build();

        if (untaggedRespRegex.test(line)) {
            untaggedResp = true;
        }

        if (testRegex.test(line)) {
            assertThat("В ответе больше одной строки SEARCH", messageIds, hasSize(0));
            messageIds = on(' ').splitToList(testRegex.getText(line, 1));
        }
    }

    public List<String> getMessages() {
        return messageIds;
    }

    public String getLastMessage() {
        return messageIds.get(messageIds.size() - 1);
    }

    @Step("Должны быть найдены сообщения с номерами: {0}")
    public SearchResponse shouldSeeMessages(String... messageIds) {
        assertThat(format("Должны быть найдены сообщения с номерами «%s»", java.util.Arrays.toString(messageIds)),
                this.messageIds, hasSameItemsAsList(asList(messageIds)));
        return this;
    }

    @Step("Должно быть найдено 0 сообщений")
    public SearchResponse shouldBeEmpty() {
        assertThat("Ожидалось, что будет найдено 0 сообщений", messageIds, hasSize(0));
        return this;
    }

    @Step("Должно найтись не пустое множество писем")
    public SearchResponse shouldNotBeEmpty() {
        assertThat("Ожидалось, что будет найдено не 0 сообщений", messageIds, hasSize(greaterThan(0)));
        return this;
    }

    @Step("Количество сообщений должно быть {0}")
    public SearchResponse shouldHasSize(int i) {
        assertThat("Ожидалось, что будет найдено сообщений количеством (" + i + ")", messageIds, hasSize(i));
        return this;
    }

    @Step("Должны быть найдены сообщения: {0}")
    public SearchResponse shouldContain(String... messageIds) {
        assertThat("Некоторые из сообщений не найдены", this.messageIds, hasItems(messageIds));
        return this;
    }

    @Step("Списки должны совпадать")
    public SearchResponse shouldContain(List<String> messageIds) {
        assertThat("Списки не совпадают", this.messageIds, hasSameItemsAsList(messageIds));
        return this;
    }

    @Step("НЕ должны быть найдены сообщения {0}")
    public SearchResponse shouldNotContain(String... messageIds) {
        assertThat("Не должны были найти сообщения", this.messageIds, not(hasItems(messageIds)));
        return this;
    }

    @Step("Ответ должен содержать * SEARCH")
    public SearchResponse shouldSeeUntaggedResponse() {
        assertThat("Ответ не содержит * SEARCH", untaggedResp, is(true));
        return this;
    }

    @Step("Ответ НЕ должен содержать * SEARCH")
    public SearchResponse shouldNotSeeUntaggedResponse() {
        assertThat("Ответ содержит * SEARCH", untaggedResp, is(false));
        return this;
    }
}
