package ru.yandex.autotests.innerpochta.imap.steps;

import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.requests.StatusRequest.status;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.04.14
 * Time: 14:09
 */
public class StatusSteps {

    private final ImapClient client;

    private StatusSteps(ImapClient imap) {
        this.client = imap;
    }

    public static StatusSteps with(ImapClient imapClient) {
        return new StatusSteps(imapClient);
    }

    @Step("Получаем количество сообщений в папке <{0}>")
    public int getNumberOfMessages(String folder) {
        return client.request(status(folder).messages()).shouldBeOk().numberOfMessages();
    }

    @Step("Получаем количество новых сообщений в папке <{0}>")
    public int getNumberOfRecentMessages(String folder) {
        return client.request(status(folder).recent()).shouldBeOk().numberOfRecentMessages();
    }

    @Step("Получаем количество непрочитанных сообщений в папке <{0}>")
    public int getNumberOfUnseenMessages(String folder) {
        return client.request(status(folder).unseen()).shouldBeOk().numberOfUnseenMessages();
    }

    @Step("Получаем uid validity (timestamp создания) для папки <{0}>")
    public int getUidValidity(String folder) {
        return client.request(status(folder).uidValidity()).shouldBeOk().uidValidity();
    }

    @Step("Получаем uid next  для папки INBOX")
    public int getUidNext() {
        return client.request(status(Folders.INBOX).uidNext()).shouldBeOk().uidNext();
    }

    @Step("Получаем uid next  для папки <{0}>")
    public int getUidNext(String folder) {
        return client.request(status(folder).uidNext()).shouldBeOk().uidNext();
    }

    @Step("Количество сообщений в папке INBOX, должно быть {1}")
    public void numberOfMessagesInInboxShouldBe(int value) {
        assertThat(getNumberOfMessages(Folders.INBOX), is(value));
    }

    @Step("Количество сообщений в папке {0}, должно быть {1}")
    public void numberOfMessagesShouldBe(String folder, int value) {
        assertThat(getNumberOfMessages(folder), is(value));
    }

    @Step("Количество новых сообщений в папке {0}, должно быть {1}")
    public void numberOfRecentMessagesShouldBe(String folder, int value) {
        assertThat(getNumberOfRecentMessages(folder), is(value));
    }

    @Step("Uid next в папке {0}, должно быть {1}")
    public void uidNextShouldBe(String folder, int value) {
        assertThat(getUidNext(folder), is(value));
    }

    @Step("Uid validity в папке {0}, должно быть {1}")
    public void uidValidityShouldBe(String folder, int value) {
        assertThat(getUidValidity(folder), is(value));
    }

    @Step("Количество непрочитанных сообщений в папке {0}, должно быть {1}")
    public void numberOfUnseenMessagesShouldBe(String folder, int value) {
        assertThat(getNumberOfUnseenMessages(folder), is(value));
    }

    @Step("В папке {0} не должно быть сообщений")
    public void shouldBeNoMessagesInFolder(String folder) {
        assertThat(getNumberOfMessages(folder), is(0));
    }

    @Step("В папке INBOX не должно быть сообщений")
    public void shouldBeNoMessagesInInbox() {
        assertThat(getNumberOfMessages(Folders.INBOX), is(0));
    }
}
