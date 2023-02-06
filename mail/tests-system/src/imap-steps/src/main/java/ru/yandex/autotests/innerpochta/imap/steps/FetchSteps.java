package ru.yandex.autotests.innerpochta.imap.steps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

import ru.lanwen.verbalregex.VerbalExpression;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.FetchStoreResponse;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.lanwen.verbalregex.VerbalExpression.regex;
import static ru.yandex.autotests.innerpochta.imap.matchers.FlagsExistMatcher.hasFlags;
import static ru.yandex.autotests.innerpochta.imap.matchers.WaitMatcher.withWaitFor;
import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 06.05.14
 * Time: 21:15
 */
public class FetchSteps {

    private static final long TIMEOUT = 10;

    private final ImapClient client;

    private FetchSteps(ImapClient imap) {
        this.client = imap;
    }

    public static FetchSteps with(ImapClient imapClient) {
        return new FetchSteps(imapClient);
    }

    /**
     * . FETCH 1 uid
     * 1 FETCH (UID 1139)
     * . OK fetch completed
     *
     * @param id
     * @return
     */
    @Step("Определяем UID сообщения по номеру <{0}>")
    public int uid(String id) {
        return client.request(fetch(id).uid()).shouldBeOk().uid();
    }

    @Step("UID у сообщения с номером <{0}> должен быть {1}")
    public FetchStoreResponse uidShouldBe(String id, int uidNext) {
        return client.request(fetch(id).uid()).shouldBeOk().uidShouldBe(uidNext);
    }

    /**
     * . FETCH 1 flags
     * 1 FETCH (FLAGS ())
     * . OK fetch completed
     *
     * @param id
     * @return
     */
    @Step("Определяем по номеру сообщения <{0}> флаги")
    public List<String> flags(String id) {
        return client.request(fetch(id).flags()).shouldBeOk().flags();
    }

    @Step("Должен быть флаг {1} у сообщения с номером {0}")
    public FetchStoreResponse flagShouldBe(String id, String flag) {
        return client.request(fetch(id).flags()).shouldBeOk().flagShouldBe(flag);
    }

    @Step("Должены быть флаги {1} у сообщения с номером {0}")
    public FetchStoreResponse flagsShouldBe(String id, String... flags) {
        return client.request(fetch(id).flags()).shouldBeOk().flagsShouldBe(flags);
    }

    @Step("Должены быть флаги {1} у сообщения с номером {0}")
    public FetchStoreResponse flagsShouldBe(String id, List<String> flags) {
        return client.request(fetch(id).flags()).shouldBeOk().flagsShouldBe(flags);
    }

    @Step("У сообщения с номером {0} не должно быть флагов")
    public FetchStoreResponse shouldBeNoFlags(String id) {
        return flagShouldBe(id, "");
    }

    @Step("У сообщения <{0}> должны быть флаги {1}")
    public void waitFlags(String id, List<String> flags) {
        assertThat(client, withWaitFor(hasFlags(id, flags), TIMEOUT, SECONDS));
    }

    @Step("У сообщения <{0}> должны быть флаги {1}")
    public void waitFlags(String id, String... flags) {
        waitFlags(id, asList(flags));
    }

    @Step("Ждем что у сообщения <{0}> не будет флагов")
    public void waitNoFlags(String id) {
        assertThat(client, withWaitFor(hasFlags(id, ""), TIMEOUT, SECONDS));
    }

    //планируется:

    /**
     * . FETCH 1 BODY[HEADER.fields (SUBJECT FROM CC DATE)]
     * 1 FETCH (BODY[HEADER.FIELDS (DATE CC FROM SUBJECT)] {168}
     * From: =?koi8-r?B?9MXT1MXSIPTF09TP18ne?= <testtoemail@yandex.ru>
     * Cc: vicdev@xn--80aalbavookw.xn--p1ai
     * Subject: 16hr22tiz2tph
     * Date: Wed, 23 Apr 2014 23:47:36 +0700
     *
     * @return
     */
    @Step("Получаем тему письма по id = {0}")
    public String getSubject(String id) throws MessagingException {
        return client.request(fetch(id).body("header")).constructMimeMessage().getSubject();
    }

    @Step("Получаем поле to письма по id = {0}")
    public String getToField(String id) throws MessagingException {
        Address[] address = client.request(fetch(id).body("header")).constructMimeMessage()
                .getRecipients(Message.RecipientType.TO);
        assertThat(address, arrayWithSize(greaterThan(0)));
        VerbalExpression testRegex = regex()
                .startOfLine().anything().then("<").capt().anything().endCapt().then(">").endOfLine().build();
        if (testRegex.test(address[0].toString())) {
            return testRegex.getText(address[0].toString(), 1);
        }
        return address[0].toString();
    }

    @Step("Получаем поле from письма по id = {0}")
    public String getFromField(String id) throws MessagingException {
        Address[] address = client.request(fetch(id).body("header")).constructMimeMessage().getFrom();
        assertThat(address, arrayWithSize(greaterThan(0)));
        VerbalExpression testRegex = regex()
                .startOfLine().anything().then("<").capt().anything().endCapt().then(">").endOfLine().build();
        if (testRegex.test(address[0].toString())) {
            return testRegex.getText(address[0].toString(), 1);
        }
        return address[0].toString();
    }

    @Step("Получаем копию сс письма по id = {0}")
    public String getCcField(String id) throws MessagingException {
        Address[] address = client.request(fetch(id).body("header")).constructMimeMessage()
                .getRecipients(Message.RecipientType.CC);
        assertThat(address, arrayWithSize(greaterThan(0)));
        return address[0].toString();
    }

    @Step("Получаем скрытую копию бсс письма по id = {0}")
    public String getBccField(String id) throws MessagingException {
        Address[] address = client.request(fetch(id).body("header")).constructMimeMessage()
                .getRecipients(Message.RecipientType.BCC);
        assertThat(address, arrayWithSize(greaterThan(0)));
        return address[0].toString();
    }

    @Step("Получаем дату отправки письма по id = {0}")
    public String getSentDate(String id) throws MessagingException {
        Date date = client.request(fetch(id).body("header")).constructMimeMessage().getSentDate();
        return new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(date);
    }

    @Step("Получаем дату отправки письма по id = {0} и берём дату с разницей в {1}*день")
    public String getSentDateNeighbourhood(String id, int dateShift) throws MessagingException {
        Date date = client.request(fetch(id).body("header")).constructMimeMessage().getSentDate();
        date.setTime(date.getTime() + dateShift * TimeUnit.DAYS.toMillis(1));
        return new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(date);
    }

    @Step("Получаем размер письма по id = {0}")
    public int getSize(String id) throws MessagingException {
        FetchStoreResponse res = client.request(fetch(id).rfc822size());
        VerbalExpression testRegex = regex()
                .startOfLine().then("*").anything().then("(RFC822.SIZE ").capt().anything().endCapt().then(")")
                .endOfLine().build();
        if (testRegex.test(res.lines().get(0))) {
            return Integer.valueOf(testRegex.getText(res.lines().get(0), 1));
        }
        return -1;
    }

    @Step("Получаем текст письма по id = {0}")
    public List<String> getText(String id) throws MessagingException {
        List<String> full = client.request(fetch(id).body("text")).lines();
        assertThat("Нет текста в сообщении", full, hasSize(greaterThan(3)));
        return full.subList(1, full.size() - 2);
    }

    @Step("Получаем тело письма по id = {0}")
    public List<String> body(String id) {
        return client.request(fetch(id).bodyPeek("subject")).shouldBeOk().lines();
    }
}
