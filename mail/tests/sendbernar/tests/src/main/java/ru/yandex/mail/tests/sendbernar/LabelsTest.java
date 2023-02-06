package ru.yandex.mail.tests.sendbernar;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.mops.Mops;
import ru.yandex.mail.tests.sendbernar.generated.SaveDraftResponse;
import ru.yandex.mail.tests.sendbernar.generated.SendMessageResponse;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLidInFolder;

@Aqua.Test
@Title("Ручка send_message")
@Description("Отвечаем на письмо. Сохраняем ответ в черновики")
@Stories("mail send")
public class LabelsTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.labels;
    }

    private UserCredentials labelsTo = new UserCredentials(Accounts.labelsTo);

    @Rule
    public CleanMessagesMopsRule cleanAnother = new CleanMessagesMopsRule(labelsTo).inbox().outbox();

    private String mid;
    private String messageId;

    private String createLabel(String name) {
        return Mops.newLabelByName(authClient, name);
    }

    @Before
    public void prepareLetter() {
        messageId = sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();


        mid = waitWith.subj(subj).waitDeliver().getMid();
    }

    @Test
    @Issue("DARIA-54416")
    @Description("Сохраняю черновик с ответом на письмо и проверяю, что он прочитан")
    public void shouldSaveDraftReplyAsSeen() throws Exception {
        String newSubj = "Re: "+subj;
        String draftMid = saveDraft()
                .withFromMailbox(authClient.account().email())
                .withTo(authClient.account().email())
                .withSubj(newSubj)
                .withSourceMid(mid)
                .withInreplyto(mid)
                .withReferences(messageId)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        assertThat("Черновик должен быть прочитанным ",
                authClient,
                hasMsgWithLidInFolder(draftMid, folderList.draftFID(), lidByTitle("seen_label")));
    }

    @Test
    @Description("Отправляем письмо с несколькими forwarded_mids и проверяем наличие метки forwarded_label")
    public void shouldMarkMessagesForwarded() throws Exception {
        String forwardedLid = lidByTitle("forwarded_label");
        String anotherSubj = Random.string();
        String resultSubj = Random.string();


        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(anotherSubj)
                .post(shouldBe(ok200()));


        String mid2 = waitWith.subj(anotherSubj).waitDeliver().getMid();


        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(resultSubj)
                .withForwardMids(mid)
                .withForwardMids(mid2)
                .post(shouldBe(ok200()));


        waitWith.subj(resultSubj).waitDeliver();


        assertThat("Нет метки на письме",
                authClient,
                hasMsgWithLidInFolder(mid, folderList.defaultFID(), forwardedLid));
        assertThat("Нет метки на письме",
                authClient,
                hasMsgWithLidInFolder(mid2, folderList.defaultFID(), forwardedLid));
    }

    @Test
    @Description("Отправляем письмо с in_reply_to и проверяем наличие метки answered_label")
    public void shouldMarkMessageReplied() throws Exception {
        String answeredLid = lidByTitle("answered_label");
        String anotherSubj = Random.string();


        String messageIdSent = sendMessage()
                .withTo(labelsTo.account().email())
                .withSubj(anotherSubj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();


        String midSent = waitWith.subj(anotherSubj).sent().waitDeliver().getMid();


        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(anotherSubj)
                .withInreplyto(messageIdSent)
                .withMarkAs("replied")
                .post(shouldBe(ok200()));


        waitWith.subj(anotherSubj).waitDeliver();


        assertThat("Нет метки на письме",
                authClient,
                hasMsgWithLidInFolder(midSent, folderList.sentFID(), answeredLid));
    }

    @Test
    @Description("Отправляем письмо и проверяем постановку пользовательской метки")
    public void shouldSendMessagesWithCustomLabels() throws Exception {
        String lid1 = createLabel(Random.string());
        String lid2 = createLabel(Random.string());
        String newSubj = Random.string();


        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(newSubj)
                .withLids(lid1)
                .withLids(lid2)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(newSubj).sent().waitDeliver().getMid();


        assertThat("Нет метки на письме",
                authClient,
                hasMsgWithLidInFolder(mid, folderList.sentFID(), lid1));
        assertThat("Нет метки на письме",
                authClient,
                hasMsgWithLidInFolder(mid, folderList.sentFID(), lid2));
    }
}
