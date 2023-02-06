package ru.yandex.mail.tests.sendbernar;


import io.restassured.response.Response;
import org.junit.Test;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertTrue;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.things.matchers.IsThereMessagesMatcher.hasMsgIn;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLidInFolder;


@Features("sendbernar")
public abstract class BaseDelayedMessageTest extends BaseSendbernarClass {
    abstract String sendTime(long offset);

    abstract String mailSend(String to, String subj, String sendTime, Function<Response, Response> shouldBe);

    abstract String mailSend(String to, String subj, String sendTime, String messageId, Function<Response, Response> shouldBe);

    abstract void callback(String mid, Function<Response, Response> shouldBe);

    abstract boolean cancel(String mid, Function<Response, Response> shouldBe);

    abstract List<String> labelsToBeSet();

    @Test
    @Title("Отправка отложенного письма и ожидание вызова от ремайндеров")
    public void shouldSentDelayedMessageAndWaitForTheRemindersApiWillCallCallbackHandler() throws Exception {
        long timeout = SECONDS.toMillis(10);
        mailSend(authClient.account().email(), subj, sendTime(timeout), shouldBe(ok200()));


        String midSent = waitWith.subj(subj).sent()
                .errorMsg("Письмо в ОТПРАВЛЕННЫХ до истечения таймаута не появилось")
                .timeout(timeout + SECONDS.toMillis(30)).waitDeliver().getMid();

        assertThat("Отложенное письмо должно быть прочитано в папке \"Отправленные\"",
                authClient,
                hasMsgWithLidInFolder(midSent, folderList.sentFID(), seenLid()));

        waitWith.subj(subj)
                .errorMsg("Письмо во входящих до истечения таймаута не появилось").waitDeliver();
    }

    @Test
    @Title("Принудительная отправка отложенного письма самому себе")
    @Issue("MAILPG-761")
    public void shouldSentDelayedMessageAndCallCallbackHandler() throws Exception {
        long timeout = HOURS.toMillis(10);
        String mid = mailSend(authClient.account().email(), subj, sendTime(timeout), shouldBe(ok200()));

        callback(mid, shouldBe(ok200()));


        String midSent = waitWith.subj(subj).sent()
                .errorMsg("Письмо в ОТПРАВЛЕННЫХ до истечения таймаута не появилось").waitDeliver().getMid();


        assertThat("Отложенное письмо должно быть прочитано в папке \"Отправленные\"",
                authClient,
                hasMsgWithLidInFolder(midSent, folderList.sentFID(), seenLid()));


        waitWith.subj(subj)
                .errorMsg("Письмо во ВХОДЯЩИХ до истечения таймаута не появилось").waitDeliver();
    }

    @Test
    @Title("Вызов колбека с несуществующим мидом")
    @Description("Отвечаем 200 так как пользователь мог удалить письмо сам. "+
                 "Не отдаём ремайндерам ошибку чтоб они не ретраились")
    public void shouldNotReturnErrorOnWrongMid() throws Exception {
        callback(String.valueOf(Random.shortInt()), shouldBe(ok200()));
    }

    @Test
    @Title("Вызов колбека с мидом из входящих")
    public void shouldNotReturnErrorOnMessageNotFromOutgoing() throws Exception {
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        callback(mid, shouldBe(ok200()));
    }

    @Test
    @Title("Отправка отложенного письма с огромным ресипиентом самому себе")
    public void shouldSendDelayedMessageWithVeryLongRecipientHandler() throws Exception {
        StringBuilder to = new StringBuilder();
        for(int i = 0; i < 30; i ++ )  {
            to.append(Random.longString());
        }
        to.append(String.format(" <%s>", authClient.account().email()));

        long timeout = SECONDS.toMillis(20);

        mailSend(to.toString(), subj, sendTime(timeout), shouldBe(ok200()));


        String midSent = waitWith.subj(subj).sent()
                .errorMsg("Письмо в ОТПРАВЛЕННЫХ до истечения таймаута не появилось")
                .timeout(timeout+SECONDS.toMillis(30)).waitDeliver().getMid();


        assertThat("Отложенное письмо должно быть прочитано в папке \"Отправленные\"",
                authClient,
                hasMsgWithLidInFolder(midSent, folderList.sentFID(), seenLid()));


        waitWith.subj(subj)
                .errorMsg("Письмо во входящих до истечения таймаута не появилось").waitDeliver();

    }

    @Test
    @Title("Проверка удаления письма из исходящих после стандартной отправки")
    public void shouldDeleteMessageFromOutgoingAfterUsualSend() throws Exception {
        long timeout = HOURS.toMillis(10);
        String mid = mailSend(authClient.account().email(), subj, sendTime(timeout), shouldBe(ok200()));

        assertThat("Письма нет в исходящих",
                authClient,
                hasMsgIn(subj, folderList.outgoingFID()));


        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .withSourceMid(mid)
                .post(shouldBe(ok200()));


        assertThat("В исходящих письмо почему-то осталось",
                authClient,
                not(hasMsgIn(subj, folderList.outgoingFID())));


        waitWith.subj(subj).inbox().waitDeliver().getMid();
    }

    @Test
    @Title("Проверка удаления письма из исходящих после отправки колбеком")
    public void shouldDeleteMessageFromOutgoingAfterCallbackSending() throws Exception {
        long timeout = SECONDS.toMillis(10);

        mailSend(authClient.account().email(), subj, sendTime(timeout), shouldBe(ok200()));


        waitWith.subj(subj).inbox().waitDeliver();


        assertThat("Отложенное письмо осталось в папке \"Исходящие\"",
                authClient,
                not(hasMsgIn(subj, folderList.outgoingFID())));
    }

    @Test
    @Title("Проверяем, в случае отложенной отправки невалидным получателям, приходит письмо про это")
    public void shouldGetBounceLetterOnSendingWithWrongRecipient() throws Exception {
        long timeout = SECONDS.toMillis(10);
        mailSend(randomAddress(), subj, sendTime(timeout), shouldBe(ok200()));


        assertThat("Письма нет в исходящих",
                authClient,
                hasMsgIn(subj, folderList.outgoingFID()));


        waitWith.subj("Недоставленное сообщение").inbox()
                .errorMsg("Не получили письма о невозможности доставки")
                .waitDeliver();
        assertThat("Письма нет в отправленных",
                authClient,
                hasMsgIn(subj, folderList.sentFID()));
    }

    @Test
    @Title("Проверяем, сохраняем ли параметр message_id")
    public void shouldSaveMessageId() {
        String messageId = "<1121537786231@wmi5-qa.yandex.ru>";
        long timeout = HOURS.toMillis(10);


        String msgIdOfLetter = mailSend(authClient.account().email(), subj, sendTime(timeout), messageId, shouldBe(ok200()));
        String mid = waitWith.fid(folderList.outgoingFID()).subj(subj).waitDeliver().getMid();


        assertThat("message_id в ответе сендбернара не совпадает с переданным", msgIdOfLetter, equalTo(messageId));


        String messageIdHeader = byMid(mid).getHeader("message-id");
        assertThat("message_id в ответе mbody не совпадает с переданным в sendbernar", messageIdHeader, equalTo(messageId));
    }

    @Test
    @Title("Проверка отмены отправки письма")
    public void shouldCancelSendDelayedMessage() throws Exception {
        long timeout = SECONDS.toMillis(10);

        String mid = mailSend(authClient.account().email(), subj, sendTime(timeout), shouldBe(ok200()));

        boolean allDone = cancel(mid, shouldBe(ok200()));

        assertTrue("allDone должно содержать значение true", allDone);

        waitWith.subj(subj)
                .draft()
                .timeout(timeout + SECONDS.toMillis(30))
                .errorMsg("Письмо в папке Черновики до истечения таймаута не появилось")
                .waitDeliver();

        for (String label : labelsToBeSet()) {
            assertThat("На письме не должно быть системной метки " + label,
                    authClient,
                    not(hasMsgWithLidInFolder(mid, folderList.draftFID(), lidByTitle(label))));
        }

        assertThat("Письмо должно быть удалено из папки Отправленные",
                authClient,
                not(hasMsgIn(mid, folderList.outgoingFID())));

        assertThat("Письмо не должно быть принято в папку Входящие",
                authClient,
                not(hasMsgIn(mid, folderList.inboxFID())));

        String draftLid = lidByTitle("draft_label");

        assertThat("На письме должна быть системная метка " + draftLid,
                authClient,
                hasMsgWithLidInFolder(mid, folderList.draftFID(), draftLid));
    }
}
