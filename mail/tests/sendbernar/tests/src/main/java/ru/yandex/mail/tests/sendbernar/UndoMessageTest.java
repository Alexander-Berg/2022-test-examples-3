package ru.yandex.mail.tests.sendbernar;

import io.restassured.response.Response;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.tests.sendbernar.generated.CancelSendUndoResponse;
import ru.yandex.mail.tests.sendbernar.generated.SendUndoResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Aqua.Test
@Title("Отправка отложенного письма")
@Description("Отправка отложенного письма, ожидание его получения")
@Stories("mail send")
@Issues({@Issue("DARIA-48948"), @Issue("DARIA-48948"), @Issue("MAILPG-539"),
        @Issue("MAILPG-761"), @Issue("MAILPG-583"), @Issue("MAILADM-4558"), @Issue("MAILPG-2124")})
public class UndoMessageTest extends BaseDelayedMessageTest {
    @Override
    AccountWithScope mainUser() {
        return Accounts.undoMailSend;
    }

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
    void callback(String mid, Function<Response, Response> shouldBe) {
        sendUndoMessage()
                .withMid(mid)
                .get(shouldBe);
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
}

