package ru.yandex.autotests.innerpochta.imap.steps;

import org.hamcrest.FeatureMatcher;

import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.exceptions.RetryAfterErrorException;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.SelectRequest;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.innerpochta.imap.matchers.WaitMatcher.withWaitFor;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.04.14
 * Time: 20:07
 */
public class SelectSteps {

    private final ImapClient client;

    private SelectSteps(ImapClient imap) {
        this.client = imap;
    }

    public static SelectSteps with(ImapClient imapClient) {
        return new SelectSteps(imapClient);
    }

    @Step("Повторяем запрос до тех пор, пока не будет сообщений в папке INBOX, затем, делаем UNSELECT")
    public void waitNoMessagesInInbox() {
        waitMsgs(Folders.INBOX, 0);
    }

    @Step("Повторяем запрос до тех пор, пока не будет сообщений в папке {0}, затем, делаем UNSELECT")
    public void waitNoMessages(String folder) {
        waitMsgs(folder, 0);
    }

    @Step("Повторяем запрос до тех пор, пока 1 сообщение не появятся в INBOX-е, затем, делаем UNSELECT")
    public void waitMsgInInbox() {
        waitMsgs(Folders.INBOX, 1);
    }


    @Step("Повторяем запрос до тех пор, пока {0} сообщений не появятся в INBOX-е, затем, делаем UNSELECT")
    public void waitMsgsInInbox(final Integer value) {
        waitMsgs(Folders.INBOX, value);
    }


    @Step("Повторяем запрос до тех пор, пока {1} сообщений не появятся в папке <{0}>, затем, делаем UNSELECT")
    public void waitMsgs(String folder, final Integer value) {
        client.noop().pullChanges();
        final SelectRequest req = select(folder);
        org.hamcrest.Matcher<? super ImapClient> matcher = new FeatureMatcher<ImapClient, Integer>(
                equalTo(value), "EXISTS", "") {
            @Override
            protected Integer featureValueOf(ImapClient actual) {
                return actual.request(req).exist();
            }
        };
        try {
            assertThat("Не дождались нужного значения EXISTS", client, withWaitFor(matcher, 60, SECONDS));
        } catch (AssertionError error) {
            throw new RetryAfterErrorException(error);
        }

        client.getLastResponse();
    }

    @Step("Проверяем, что папка <{0}> не пустая")
    public void notEmptyFolder(String folder) {
        client.noop().pullChanges();
        final SelectRequest req = select(folder);
        org.hamcrest.Matcher<? super ImapClient> matcher = new FeatureMatcher<ImapClient, Integer>(
                greaterThan(0), "EXISTS", "") {
            @Override
            protected Integer featureValueOf(ImapClient actual) {
                return actual.request(req).exist();
            }
        };
        assertThat("Не дождались нужного значения EXISTS", client, withWaitFor(matcher, 10, SECONDS));
        client.request(unselect());
    }

    @Step("Селектим INBOX")
    public void inbox() {
        client.request(select(Folders.INBOX)).shouldBeOk();
    }

    @Step("Селектим {0}")
    public void folder(String folder) {
        client.request(select(folder)).shouldBeOk();
    }

}
