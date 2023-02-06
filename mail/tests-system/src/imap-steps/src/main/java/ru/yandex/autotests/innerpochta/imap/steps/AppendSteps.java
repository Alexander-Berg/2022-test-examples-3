package ru.yandex.autotests.innerpochta.imap.steps;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.utils.ImapMessage;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getRandomMessage;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 19.04.14
 * Time: 15:45
 */
public class AppendSteps {

    private static final int MAX_COUNT = 5;
    private final ImapClient client;
    private List<String> messages = new ArrayList<>();

    private AppendSteps(ImapClient imap) {
        this.client = imap;
    }

    public static AppendSteps with(ImapClient imapClient) {
        return new AppendSteps(imapClient);
    }

    @Step("Аппендим рандомное сообщение в INBOX")
    public void appendRandomMessageInInbox() throws Exception {
        appendRandomMessages(Folders.INBOX, 1);
    }

    @Step("Аппендим рандомное сообщение в папку {0}")
    public void appendRandomMessage(String folderName) throws Exception {
        appendRandomMessages(folderName, 1);
    }

    @Step("Аппендим рандомное сообщение в папку {0}")
    public void appendRandomMessage(String folderName, ImapMessage message) throws Exception {
        client.request(append(folderName, literal(message.construct()))).shouldBeOk();
    }

    /**
     * Метод подходит для заполнения папки небольшим количеством сообщений: <20
     * Так как достаточно медленный
     *
     * @param folderName
     * @param count
     * @throws Exception
     */
    @Step("Аппендим {1} рандомных сообщений в папку {0}")
    public void appendRandomMessages(String folderName, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            client.request(append(folderName, literal(getRandomMessage()))).shouldBeOk();
        }
    }

    @Step("Аппендим {1} рандомных сообщений в папку {0}")
    public List<String> appendRandomMessagesToFolder(String folderName, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            String name = Utils.generateName();
            client.request(append(folderName, literal(getRandomMessage(name)))).shouldBeOk();
            messages.add(name);
        }
        return messages;
    }

    /**
     * Суровый метод подходит для заполнения папки большим количеством сообщений > 20
     * Аппедим MAX_COUNT сообщений, затем клонируем сообщения
     *
     * @param folderName
     * @param count
     * @throws Exception
     */
    @Step("Аппендим большое количество ({1}) рандомных сообщений в папку {0}")
    public void appendManyRandomMessagesWithCopy(String folderName, int count) throws Exception {
        if (count <= MAX_COUNT) {
            appendRandomMessages(folderName, count);
            client.select().waitMsgs(folderName, count);
        } else {
            int step = MAX_COUNT;

            appendRandomMessages(folderName, MAX_COUNT);
            client.select().waitMsgs(folderName, MAX_COUNT);

            client.request(select(folderName)).shouldBeOk();
            int current = step;

            while (current + step < count) {
                client.copy().cloneSequenceMessages(1, step, folderName);
                current += step;
                step = current;
            }

            step = count - current;
            client.copy().cloneSequenceMessages(1, step, folderName);

            client.request(unselect()).shouldBeOk();
        }
    }

    @Step("Аппендим {0} рандомных сообщений в папку INBOX")
    public void appendRandomMessagesInInbox(int i) throws Exception {
        appendRandomMessages(Folders.INBOX, i);
    }
}
