package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.Assert;

import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.imap.matchers.IsNotExtended.not;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.04.14
 * Time: 18:45
 */
public class IdleResponse extends ImapResponse<IdleResponse> {
    public static final String IDLING = "idling";
    private final Collection<Integer> expunged = new ArrayList<>();
    private String idling;
    private Integer exists;
    private Integer recent;
    private String fetch;

    @Override
    protected void parse(String line) {
        parseIdle(line);
    }

    @Override
    protected void validate() {
        assertThat("В ответе больше одной строки IDLE ", lines().size(), equalTo(1));
        assertThat("В ответе нет idling ", idling, not(nullValue()));
    }

    private void parseIdle(String line) {
        Matcher matcher = Pattern.compile("\\+ (.*)$").matcher(line);
        if (matcher.matches()) {
            idling = matcher.group(1);
        }

        Matcher matcherExpunged = Pattern.compile("(?i)^\\* ([0-9]*) EXPUNGE$").matcher(line);
        Matcher matcherExists = Pattern.compile("(?i)^\\* ([0-9]*) EXISTS$").matcher(line);
        Matcher matcherRecent = Pattern.compile("(?i)^\\* ([0-9]*) RECENT$").matcher(line);
        Matcher matcherFetch = Pattern.compile("(?i)^\\* ([0-9]*) FETCH .*$").matcher(line);

        if (matcherExpunged.matches()) {
            expunged.add(Integer.valueOf(matcherExpunged.group(1)));
        }

        if (matcherExists.matches()) {
            exists = Integer.valueOf(matcherExists.group(1));
        }

        if (matcherRecent.matches()) {
            recent = Integer.valueOf(matcherRecent.group(1));
        }

        if (matcherFetch.matches()) {
            fetch = line;
        }
    }

    @Step("Ожидаем получить + {0}")
    public IdleResponse hasIdling(String expected) {
        Assert.assertThat(idling, equalTo(expected));
        return this;
    }

    @Step("В ответе не должно быть событий с <EXISTS>")
    public IdleResponse existsShouldBeEmpty() {
        assertThat(exists, is(Matchers.nullValue()));
        return this;
    }

    @Step("В ответе должен быть <EXISTS> равный {0}")
    public IdleResponse existsShouldBe(Integer value) {
        assertThat("В ответе должен быть определенный <EXISTS>", exists, equalTo(value));
        return this;
    }

    @Step("В ответе не должно быть событий с <EXPUNGE>")
    public IdleResponse expungedShouldBeEmpty() {
        assertThat("В ответе не должно быть событий с <EXPUNGE>", expunged, is(empty()));
        return this;
    }

    @Step("В ответе должен быть <EXPUNGE> со значениями: {0}")
    public IdleResponse expungeShouldBe(Integer... values) {
        assertThat("В ответе должен быть <EXPUNGE> со значениями",
                newArrayList(expunged), hasSameItemsAsList(newArrayList(values)));
        return this;
    }

    @Step("В ответе не должно быть событий с <RECENT>")
    public IdleResponse recentShouldBeEmpty() {
        assertThat("В ответе не должно быть событий с <RECENT>", recent, is(Matchers.nullValue()));
        return this;
    }

    @Step("В ответе должен быть <RECENT> со значениями: {0}")
    public IdleResponse recentShouldBe(Integer value) {
        assertThat("В ответе должен быть <RECENT> со значениями", recent, equalTo(value));
        return this;
    }

    @Step("В ответе должен быть <FETCH>: {0}")
    public IdleResponse fetchShouldBe(String value) {
        System.out.println(fetch);
        assertThat("В ответе должен быть <FETCH>: ", fetch, equalTo(value));
        return this;
    }
}
