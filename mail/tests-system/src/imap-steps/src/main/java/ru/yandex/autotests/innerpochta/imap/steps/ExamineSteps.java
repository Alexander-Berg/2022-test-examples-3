package ru.yandex.autotests.innerpochta.imap.steps;

import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.05.14
 * Time: 17:17
 */
public class ExamineSteps {

    private final ImapClient client;

    private ExamineSteps(ImapClient imap) {
        this.client = imap;
    }

    public static ExamineSteps with(ImapClient imapClient) {
        return new ExamineSteps(imapClient);
    }

    @Step("Выбираем для чтения INBOX")
    public void inbox() {
        client.request(examine(Folders.INBOX)).shouldBeOk();
    }
}
