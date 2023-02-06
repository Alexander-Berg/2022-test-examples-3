package ru.yandex.autotests.innerpochta.imap.responses;


import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.google.common.base.Joiner;

import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static javax.mail.Session.getDefaultInstance;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static ru.lanwen.verbalregex.VerbalExpression.regex;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/*в общем случае fetch нельзя распарсить регуляркой, так как он по своей сути сложнорекурсивный
поэтому будем стараться распарсить то что сможем
*/
public final class FetchStoreResponse extends ImapResponse<FetchStoreResponse> {

    public static final String STORE_READ_ONLY_FOLDER = "[CLIENTBUG] STORE Can not store in read-only folder";
    public static final String FETCH_NO_MESSAGES = "[CLIENTBUG] FETCH completed (no messages)";
    public static final String FETCH_WRONG_SESSION_STATE = "[CLIENTBUG] FETCH Wrong session state for command";
    public static final String STORE_WRONG_SESSION_STATE = "[CLIENTBUG] STORE Wrong session state for command";

    //private static final Pattern FETCH_PATTERN = Pattern.compile("(?i)^\\* (\\S*) FETCH \\((.*)\\)$");
    private static final Pattern UID_PATTERN = Pattern.compile(".*UID ([\\d]+).*");
    private static final Pattern FLAGS_PATTERN = Pattern.compile(".*FLAGS \\(([^\\)]*)\\).*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\S* (\\d+) FETCH .*");

    private String uid;
    private String number;
    private List<String> flags;

    @Override
    protected void parse(String line) {
        parseFetch(line);
    }

    @Override
    protected void validate() {
    }

    private void parseFetch(String line) {
        Matcher matcher = UID_PATTERN.matcher(line);
        if (matcher.matches()) {
            uid = matcher.group(1);
        }

        matcher = FLAGS_PATTERN.matcher(line);
        if (matcher.matches()) {
            flags = asList(matcher.group(1).split(" "));
        }

        matcher = NUMBER_PATTERN.matcher(line);
        if (matcher.matches()) {
            number = matcher.group(1);
        }
    }

    public int uid() {
        assertThat("UID должен быть числом", uid, matchesPattern(regex().digit().oneOrMore().build().toString()));
        return Integer.parseInt(uid);
    }

    public int number() {
        assertThat("UID должен быть числом", number, matchesPattern(regex().digit().oneOrMore().build().toString()));
        return Integer.parseInt(number);
    }

    @Step("UID должен быть равен {0}")
    public FetchStoreResponse uidShouldBe(int value) {
        assertThat("UID должен быть равен определенному числу", uid(), is(value));
        return this;
    }

    @Step("NUMBER должен быть равен {0}")
    public FetchStoreResponse numberShouldBe(int value) {
        assertThat("NUMBER должен быть равен определенному числу", number(), is(value));
        return this;
    }

    public List<String> flags() {
        return flags;
    }

    @Step("Должны увидеть флаг {0}")
    public FetchStoreResponse flagShouldBe(String flag) {
        assertThat("Среди флагов нет нужного", flags(), hasItems(flag));
        return this;
    }

    public FetchStoreResponse flagsShouldBe(String... flags) {
        return flagsShouldBe(asList(flags));
    }

    @Step("Должны увидеть набор флагов {0}")
    public FetchStoreResponse flagsShouldBe(List<String> flags) {
        assertThat("Набор флагов не должен отличаться", flags(), hasSameItemsAsList(flags));
        return this;
    }

    @Step("Должно быть пустое множество флагов в ответе: ...(FLAGS ())")
    public FetchStoreResponse shouldBeEmptyFlags() {
        assertThat(flags(), hasSameItemsAsList(newArrayList("")));
        return this;
    }

    @Step("Не должны увидеть в ответе флагов")
    public FetchStoreResponse shouldBeNoFlags() {
        assertThat(flags(), nullValue());
        return this;
    }

    @Step("Не должны увидеть ни одного флага")
    public FetchStoreResponse shouldHasMessageId(String mimeMessageId) throws MessagingException {
        assertThat(constructMimeMessage().getMessageID(), equalTo(mimeMessageId));
        return this;
    }


    @Step("Составить из пришедшего текста MimeMessage")
    public MimeMessage constructMimeMessage() throws MessagingException {
        return new MimeMessage(getDefaultInstance(new Properties()),
                toInputStream(Joiner.on("\n").join(lines().subList(1, lines().size() - 1))));
    }
}
