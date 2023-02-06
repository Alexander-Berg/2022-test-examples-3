package ru.yandex.autotests.innerpochta.imap.steps;

import java.util.List;

import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.StoreRequest;
import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.base.Joiner.on;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.DELETED;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.FLAGS;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.05.14
 * Time: 21:47
 */
public class StoreSteps {

    private final ImapClient client;

    private StoreSteps(ImapClient imap) {
        this.client = imap;
    }

    public static StoreSteps with(ImapClient imapClient) {
        return new StoreSteps(imapClient);
    }

    @Step
    public void deleteOnUid(int uid) {
        client.request(store(String.valueOf(uid), FLAGS, roundBraceList(DELETED.value()))
                .uid(true)).shouldBeOk();
    }

    @Step("Ставим флаг /Deleted")
    public void deletedOnSequence(String seq) {
        client.request(store(seq, StoreRequest.FLAGS, roundBraceList(MessageFlags.DELETED.value())))
                .shouldBeOk();
    }

    @Step("Ставим флаг /Deleted на сообщения")
    public void deletedOnMessages(List<String> letters) {
        assertThat("Для пометки сообщений флагом, их количество должно быть больше 0",
                letters, hasSize(greaterThan(0)));
        client.request(store(on(",").join(letters),
                StoreRequest.FLAGS, roundBraceList(MessageFlags.DELETED.value())))
                .shouldBeOk();
    }
}
