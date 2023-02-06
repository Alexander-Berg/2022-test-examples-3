package ru.yandex.autotests.innerpochta.sendbernar;

import com.jayway.restassured.response.Response;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.CancelSendUndoResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendDelayedResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendUndoResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendundo.ApiSendUndo;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgIn;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;


@Aqua.Test
@Title("Отправка отложенного письма")
@Description("Отправка отложенного письма, ожидание его получения")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Issues({@Issue("DARIA-48948"), @Issue("DARIA-48948"), @Issue("MAILPG-539"),
        @Issue("MAILPG-761"), @Issue("MAILPG-583"), @Issue("MAILADM-4558"), @Issue("MAILPG-2124")})
@Credentials(loginGroup = "UndoMailSend")
public class UndoMessageTest extends BaseDelayedMessageTest {
    @Override
    String mailSend(String to, String subj, String sendTime, Function<Response, Response> shouldBe) {
        return sendUndo()
                .withTo(to)
                .withSubj(subj)
                .withSendTime(sendTime)
                .post(shouldBe)
                .as(SendUndoResponse.class)
                .getStored()
                .getMid();
    }

    @Override
    String mailSend(String to, String subj, String sendTime, String messageId, Function<Response, Response> shouldBe) {
        return sendUndo()
                .withTo(to)
                .withSubj(subj)
                .withSendTime(sendTime)
                .withMessageId(messageId)
                .post(shouldBe)
                .as(SendUndoResponse.class)
                .getMessageId();
    }

    @Override
    String mailSendWithSourceMid(String to, String subj, String sendTime, String sourceMid, Function<Response, Response> shouldBe) {
        return sendUndo()
                .withTo(to)
                .withSubj(subj)
                .withSourceMid(sourceMid)
                .withSendTime(sendTime)
                .post(shouldBe)
                .as(SendDelayedResponse.class)
                .getStored()
                .getMid();
    }

    @Override
    String mailSendWithNotifyOnSend(String to, String subj, String sendTime, Function<Response, Response> shouldBe) {
        return sendUndo()
                .withTo(to)
                .withSubj(subj)
                .withSendTime(sendTime)
                .withConfirmDelivery(ApiSendUndo.ConfirmDeliveryParam.YES)
                .post(shouldBe)
                .as(SendDelayedResponse.class)
                .getStored()
                .getMid();
    }

    @Override
    void callback(String mid, String to, String subj, Function<Response, Response> shouldBe) {
        sendUndoMessage()
                .withMid(mid)
                .withTo(to)
                .withMessageDate("0")
                .withSourceMid(mid)
                .withNotifyOnSend("no")
                .withNonemptySubject(subj)
                .withMessageId("msgId")
                .post(shouldBe);
    }

    @Override
    boolean cancel(String mid, Function<Response, Response> shouldBe) {
        return cancelSendUndo()
                .withMid(mid)
                .post(shouldBe)
                .as(CancelSendUndoResponse.class)
                .getAllDone();
    }

    @Override
    String sendTime(long offset) {
        return String.valueOf(offset/1000);
    }

    @Override
    List<String> labelsToBeSet() {
        return Arrays.asList("delayed_message", "undo_message");
    }

    @Override
    Function<Response, Response> checkSendingSpamMessageWithCallback() {
        return shouldBe(ok200());
    }

    @Override
    void postCheckSendingMessage(String mid) {
        assertThat("Почему-то осталось письмо",
                authClient,
                not(hasMsgIn(subj, folderList.outgoingFID())));
    }
}
