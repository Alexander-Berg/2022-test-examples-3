package ru.yandex.autotests.innerpochta.imap.steps;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.imap.requests.CopyRequest.copy;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.05.14
 * Time: 19:36
 */
public class CopySteps {

    private final ImapClient client;

    private CopySteps(ImapClient imap) {
        this.client = imap;
    }

    public static CopySteps with(ImapClient imapClient) {
        return new CopySteps(imapClient);
    }

    @Step("Клонируем последовательность сообщений {0}:{1} в <{2}>")
    public void cloneSequenceMessages(int since, int last, String folderName) {
        String command = String.format("%s:%s", since, last);
        client.request(copy(command, folderName));
    }
}
