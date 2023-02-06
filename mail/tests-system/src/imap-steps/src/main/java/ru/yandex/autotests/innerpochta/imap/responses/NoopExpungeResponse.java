package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.06.14
 * Time: 14:50
 */
public final class NoopExpungeResponse extends ImapResponse<NoopExpungeResponse> {

    public static final String WRONG_SESSION_STATE = "[CLIENTBUG] EXPUNGE Wrong session state for command";

    private final Collection<Integer> expunged = new ArrayList<>();
    private final Collection<Integer> exists = new ArrayList<>();
    private final Collection<Integer> recent = new ArrayList<>();


    @Override
    protected void parse(String line) {
        parseNoOp(line);
    }

    @Override
    protected void validate() {
    }

    private void parseNoOp(String line) {
        Matcher matcherExpunged = Pattern.compile("(?i)^\\* ([0-9]*) EXPUNGE$").matcher(line);
        Matcher matcherExists = Pattern.compile("(?i)^\\* ([0-9]*) EXISTS$").matcher(line);
        Matcher matcherRecent = Pattern.compile("(?i)^\\* ([0-9]*) RECENT$").matcher(line);

        if (matcherExpunged.matches()) {
            expunged.add(Integer.valueOf(matcherExpunged.group(1)));
        }

        if (matcherExists.matches()) {
            exists.add(Integer.valueOf(matcherExists.group(1)));
        }

        if (matcherRecent.matches()) {
            recent.add(Integer.valueOf(matcherRecent.group(1)));
        }
    }

    @Step("В ответе не должно быть событий с <EXISTS>")
    public NoopExpungeResponse existsShouldBeEmpty() {
        assertThat(exists, is(empty()));
        return this;
    }

    @Step("В ответе должен быть <EXISTS> равный {0}")
    public NoopExpungeResponse existsShouldBe(Integer... values) {
        assertThat("В ответе должен быть определенный <EXISTS>", newArrayList(exists),
                hasSameItemsAsList(newArrayList(values)));
        return this;
    }

    @Step("В ответе не должно быть событий с <EXPUNGE>")
    public NoopExpungeResponse expungedShouldBeEmpty() {
        assertThat("В ответе не должно быть событий с <EXPUNGE>", expunged, is(empty()));
        return this;
    }

    @Step("В ответе должен быть <EXPUNGE> со значениями: {0}")
    public NoopExpungeResponse expungeShouldBe(Integer... values) {
        assertThat("В ответе должен быть <EXPUNGE> со значениями",
                newArrayList(expunged), hasSameItemsAsList(newArrayList(values)));
        return this;
    }

    @Step("В ответе не должно быть событий с <RECENT>")
    public NoopExpungeResponse recentShouldBeEmpty() {
        assertThat("В ответе не должно быть событий с <RECENT>", recent, is(empty()));
        return this;
    }

    @Step("В ответе должен быть <RECENT> со значениями: {0}")
    public NoopExpungeResponse recentShouldBe(Integer... values) {
        assertThat("В ответе должен быть <RECENT> со значениями", newArrayList(recent),
                hasSameItemsAsList(newArrayList(values)));
        return this;
    }
}
