package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveDraftResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveTemplateResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendDelayedResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendMessageResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendUndoResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;


@Aqua.Test
@Title("Ручка send_message")
@Description("Отвечаем на письмо. Сохраняем ответ в черновики")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "MailSendReply")
public class LabelsTest extends BaseSendbernarClass {
    @ClassRule
    public static HttpClientManagerRule another = auth().with("Adminkapdd");

    @Rule
    public CleanMessagesMopsRule cleanAnother = new CleanMessagesMopsRule(another).inbox().outbox();

    private String mid;
    private String messageId;

    private String createLabel(String name) {
        return Mops.newLabelByName(authClient, name);
    }

    @Before
    public void prepareLetter() {
        messageId = sendMessage()
                .withTo(authClient.acc().getSelfEmail())
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
                .withFromMailbox(authClient.acc().getSelfEmail())
                .withTo(authClient.acc().getSelfEmail())
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
        String anotherSubj = getRandomString();
        String resultSubj = getRandomString();


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(anotherSubj)
                .post(shouldBe(ok200()));


        String mid2 = waitWith.subj(anotherSubj).waitDeliver().getMid();


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
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
        String anotherSubj = getRandomString();


        String messageIdSent = sendMessage()
                .withTo(another.acc().getSelfEmail())
                .withSubj(anotherSubj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();


        String midSent = waitWith.subj(anotherSubj).sent().waitDeliver().getMid();


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
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
    @Description("Отправляем и сохраняем письма, проверяем тип SystMetkaSO:people")
    public void shoultSetPeopleLabel() {
        String people = lidByNameAndType("4", "so");
        long offset = MINUTES.toMillis(10);


        String draftMid = saveDraft()
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("На письме нет метки", authClient, hasMsgWithLid(draftMid, people));


        String templateMid = saveTemplate()
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("На письме нет метки", authClient, hasMsgWithLid(templateMid, people));


        String delayedMid = sendDelayed()
                .withTo(authClient.acc().getSelfEmail())
                .withSendTime(String.valueOf((System.currentTimeMillis() + offset)/1000))
                .post(shouldBe(ok200()))
                .as(SendDelayedResponse.class)
                .getStored()
                .getMid();

        assertThat("На письме нет метки", authClient, hasMsgWithLid(delayedMid, people));


        String undoMid = sendUndo()
                .withTo(authClient.acc().getSelfEmail())
                .withSendTime(String.valueOf(offset/1000))
                .post(shouldBe(ok200()))
                .as(SendUndoResponse.class)
                .getStored()
                .getMid();

        assertThat("На письме нет метки", authClient, hasMsgWithLid(undoMid, people));


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(ok200()));

        String sendMid = waitWith.subj(subj).inbox().waitDeliver().getMid();
        String sentMid = waitWith.subj(subj).sent().waitDeliver().getMid();

        assertThat("На письме нет метки", authClient, hasMsgWithLid(sendMid, people));
        assertThat("На письме нет метки", authClient, hasMsgWithLid(sentMid, people));
    }

    @Test
    @Description("Отправляем письмо и проверяем постановку пользовательской метки")
    public void shouldSendMessagesWithCustomLabels() throws Exception {
        String lid1 = createLabel(getRandomString());
        String lid2 = createLabel(getRandomString());
        String newSubj = getRandomString();


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
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
