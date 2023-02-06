package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;

import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.03.15
 * Time: 15:43
 */
public final class AppendResponse extends ImapResponse<AppendResponse> {

    private final Collection<Integer> expunged = new ArrayList<>();
    private final Collection<Integer> exists = new ArrayList<>();
    private final Collection<Integer> recent = new ArrayList<>();
    private Integer uidvalidity;
    private Integer uid;

    @Override
    protected void parse(String line) {
        parseAppend(line);
    }

    @Override
    protected void validate() {
    }

    private void parseAppend(String line) {
        java.util.regex.Matcher matcherExpunged = Pattern.compile("(?i)^\\* ([0-9]*) EXPUNGE$").matcher(line);
        java.util.regex.Matcher matcherExists = Pattern.compile("(?i)^\\* ([0-9]*) EXISTS$").matcher(line);
        java.util.regex.Matcher matcherRecent = Pattern.compile("(?i)^\\* ([0-9]*) RECENT$").matcher(line);
        java.util.regex.Matcher matcherAppendUid = Pattern.compile("(?i)^\\S* OK \\[APPENDUID (\\S+) (\\S+)\\](.*)$").matcher(line);

        if (matcherExpunged.matches()) {
            expunged.add(Integer.valueOf(matcherExpunged.group(1)));
        }

        if (matcherExists.matches()) {
            exists.add(Integer.valueOf(matcherExists.group(1)));
        }

        if (matcherRecent.matches()) {
            recent.add(Integer.valueOf(matcherRecent.group(1)));
        }

        if (matcherAppendUid.matches()) {
            uidvalidity = Integer.valueOf(matcherAppendUid.group(1));
            uid = Integer.valueOf(matcherAppendUid.group(2));
        }
    }

    @Step("В ответе не должно быть событий с <EXISTS>")
    public AppendResponse existsShouldBeEmpty() {
        MatcherAssert.assertThat(exists, CoreMatchers.is(empty()));
        return this;
    }

    @Step("В ответе должен быть <EXISTS> равный {0}")
    public AppendResponse existsShouldBe(Integer... values) {
        MatcherAssert.assertThat("В ответе должен быть определенный <EXISTS>", newArrayList(exists),
                hasSameItemsAsList(newArrayList(values)));
        return this;
    }

    @Step("В ответе не должно быть событий с <EXPUNGE>")
    public AppendResponse expungedShouldBeEmpty() {
        MatcherAssert.assertThat("В ответе не должно быть событий с <EXPUNGE>", expunged, CoreMatchers.is(empty()));
        return this;
    }

    @Step("В ответе должен быть <EXPUNGE> со значениями: {0}")
    public AppendResponse expungeShouldBe(Integer... values) {
        MatcherAssert.assertThat("В ответе должен быть <EXPUNGE> со значениями",
                newArrayList(expunged), hasSameItemsAsList(newArrayList(values)));
        return this;
    }

    @Step("В ответе не должно быть событий с <RECENT>")
    public AppendResponse recentShouldBeEmpty() {
        MatcherAssert.assertThat("В ответе не должно быть событий с <RECENT>", recent, CoreMatchers.is(empty()));
        return this;
    }

    @Step("В ответе должен быть <RECENT> со значениями: {0}")
    public AppendResponse recentShouldBe(Integer... values) {
        MatcherAssert.assertThat("В ответе должен быть <RECENT> со значениями", newArrayList(recent),
                hasSameItemsAsList(newArrayList(values)));
        return this;
    }

    @Step("В ответе должен быть <UIDVALIDITY> со значениями: {0}")
    public AppendResponse uidvalidityShouldBe(Integer value) {
        MatcherAssert.assertThat("В ответе должен быть <UIDVALIDITY> со значениями", uidvalidity, equalTo(value));
        return this;
    }

    @Step("В ответе должен быть <UID> со значениями: {0}")
    public AppendResponse uidShouldBe(Integer value) {
        MatcherAssert.assertThat("В ответе должен быть <UID> со значениями", uid, equalTo(value));
        return this;
    }
}

