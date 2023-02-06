package ru.yandex.autotests.innerpochta.sendbernar;

import com.jayway.restassured.response.Response;
import org.junit.Test;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveDraftResponse;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.Source;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgIn;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FAKE_SEEN_LBL;


public abstract class BaseDelayedMessageTest extends BaseSendbernarClass {
    abstract String sendTime(long offset);

    abstract String mailSend(String to, String subj, String sendTime, Function<Response, Response> shouldBe);

    abstract String mailSend(String to, String subj, String sendTime, String messageId, Function<Response, Response> shouldBe);

    abstract String mailSendWithSourceMid(String to, String subj, String sendTime, String sourceMid, Function<Response, Response> shouldBe);

    abstract String mailSendWithNotifyOnSend(String to, String subj, String sendTime, Function<Response, Response> shouldBe);

    abstract void callback(String mid, String to, String subj, Function<Response, Response> shouldBe);

    abstract boolean cancel(String mid, Function<Response, Response> shouldBe);

    abstract List<String> labelsToBeSet();

    abstract Function<Response, Response> checkSendingSpamMessageWithCallback();

    abstract void postCheckSendingMessage(String mid);

    @Test
    public void shouldRemoveOldReminderAndSetNew() {
        long timeout = SECONDS.toMillis(10);


        String mid = mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));


        mailSendWithSourceMid(authClient.acc().getSelfEmail(), subj+subj, sendTime(timeout), mid, shouldBe(ok200()));
    }

    @Test
    @Description("Отправляем письмо и ждём письмо со статусом доставки")
    public void shouldGetDsnMessage() {
        long timeout = SECONDS.toMillis(5);
        mailSendWithNotifyOnSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));


        waitWith.subj(subj).waitDeliver();
        waitWith.subj("Письмо успешно доставлено").waitDeliver();
    }

    @Test
    @Title("Не должны удалять письма из входящих, которые попали туда через отложенную или отменяемую отправки")
    @Issue("MAILPG-2419")
    public void shouldNotRemoveMessageFromUnusualFolders() {
        long timeout = SECONDS.toMillis(10);

        String sentToSelfSubject = subj + "inbox";
        String savedToDraftSubject = subj + "draft";
        String savedToOutgoingSubject = subj + "outgoing";


        mailSend(authClient.acc().getSelfEmail(), sentToSelfSubject, sendTime(timeout), shouldBe(ok200()));

        String midOutbox = mailSend(authClient.acc().getSelfEmail(), savedToOutgoingSubject,
                sendTime(MINUTES.toMillis(10)), shouldBe(ok200()));

        String midInbox = waitWith.subj(sentToSelfSubject).inbox()
                .errorMsg("Письмо во ВХОДЯЩИХ до истечения таймаута не появилось")
                .timeout(timeout + SECONDS.toMillis(30)).waitDeliver().getMid();

        String midSent = waitWith.subj(sentToSelfSubject).sent()
                .errorMsg("Письмо во ВХОДЯЩИХ до истечения таймаута не появилось")
                .timeout(timeout + SECONDS.toMillis(30)).waitDeliver().getMid();

        String midDraft = saveDraft()
                .withSubj(savedToDraftSubject)
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        String newSubj = getRandomString();
        mailSendWithSourceMid(authClient.acc().getSelfEmail(), newSubj, sendTime(timeout), midOutbox, shouldBe(ok200()));
        mailSendWithSourceMid(authClient.acc().getSelfEmail(), newSubj, sendTime(timeout), midInbox, shouldBe(ok200()));
        mailSendWithSourceMid(authClient.acc().getSelfEmail(), newSubj, sendTime(timeout), midSent, shouldBe(ok200()));
        mailSendWithSourceMid(authClient.acc().getSelfEmail(), newSubj, sendTime(timeout), midDraft, shouldBe(ok200()));


        assertThat("Письмо должно быть в папке \"Входящие\"",
                authClient,
                hasMsgIn(sentToSelfSubject, folderList.inboxFID()));

        assertThat("Письмо должно быть в папке \"Отправленные\"",
                authClient,
                hasMsgIn(sentToSelfSubject, folderList.sentFID()));

        assertThat("Письмо не должно быть в папке \"Черновики\"",
                authClient,
                not(hasMsgIn(savedToDraftSubject, folderList.draftFID())));

        assertThat("Письмо не должно быть в папке \"Исходящие\"",
                authClient,
                not(hasMsgIn(savedToOutgoingSubject, folderList.outgoingFID())));
    }

    @Test
    @Title("Отправка отложенного письма и ожидание вызова от ремайндеров")
    public void shouldSentDelayedMessageAndWaitForTheRemindersApiWillCallCallbackHandler() throws Exception {
        long timeout = SECONDS.toMillis(10);
        mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));


        String midSent = waitWith.subj(subj).sent()
                .errorMsg("Письмо в ОТПРАВЛЕННЫХ до истечения таймаута не появилось")
                .timeout(timeout + SECONDS.toMillis(30)).waitDeliver().getMid();

        assertThat("Отложенное письмо должно быть прочитано в папке \"Отправленные\"",
                authClient,
                hasMsgWithLidInFolder(midSent, folderList.sentFID(), FAKE_SEEN_LBL));

        waitWith.subj(subj)
                .errorMsg("Письмо во входящих до истечения таймаута не появилось").waitDeliver();
    }

    @Test
    @Title("Принудительная отправка отложенного письма самому себе")
    @Issue("MAILPG-761")
    public void shouldSentDelayedMessageAndCallCallbackHandler() throws Exception {
        long timeout = HOURS.toMillis(10);
        String mid = mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));

        callback(mid, authClient.acc().getSelfEmail(), subj, shouldBe(ok200()));


        String midSent = waitWith.subj(subj).sent()
                .errorMsg("Письмо в ОТПРАВЛЕННЫХ до истечения таймаута не появилось").waitDeliver().getMid();


        assertThat("Отложенное письмо должно быть прочитано в папке \"Отправленные\"",
                authClient,
                hasMsgWithLidInFolder(midSent, folderList.sentFID(), FAKE_SEEN_LBL));


        waitWith.subj(subj)
                .errorMsg("Письмо во ВХОДЯЩИХ до истечения таймаута не появилось").waitDeliver();
    }

    @Test
    @Title("Вызов колбека с несуществующим мидом")
    @Description("Отвечаем 200 так как пользователь мог удалить письмо сам. "+
                 "Не отдаём ремайндерам ошибку чтоб они не ретраились")
    public void shouldNotReturnErrorOnWrongMid() throws Exception {
        callback(String.valueOf(Util.getRandomShortInt()), "", "", shouldBe(ok200()));
    }

    @Test
    @Title("Вызов колбека с мидом из входящих")
    public void shouldNotReturnErrorOnMessageNotFromOutgoing() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        callback(mid, authClient.acc().getSelfEmail(), subj, shouldBe(ok200()));
    }

    @Test
    @Title("Отправка отложенного письма с огромным ресипиентом самому себе")
    public void shouldSendDelayedMessageWithVeryLongRecipientHandler() throws Exception {
        StringBuilder to = new StringBuilder();
        for(int i = 0; i < 30; i ++ )  {
            to.append(Util.getLongString());
        }
        to.append(String.format(" <%s>", authClient.acc().getSelfEmail()));

        long timeout = SECONDS.toMillis(20);

        mailSend(to.toString(), subj, sendTime(timeout), shouldBe(ok200()));


        String midSent = waitWith.subj(subj).sent()
                .errorMsg("Письмо в ОТПРАВЛЕННЫХ до истечения таймаута не появилось")
                .timeout(timeout+SECONDS.toMillis(30)).waitDeliver().getMid();


        assertThat("Отложенное письмо должно быть прочитано в папке \"Отправленные\"",
                authClient,
                hasMsgWithLidInFolder(midSent, folderList.sentFID(), FAKE_SEEN_LBL));


        waitWith.subj(subj)
                .errorMsg("Письмо во входящих до истечения таймаута не появилось").waitDeliver();

    }

    @Test
    @Title("Проверка удаления письма из исходящих после стандартной отправки")
    public void shouldDeleteMessageFromOutgoingAfterUsualSend() throws Exception {
        long timeout = HOURS.toMillis(10);
        String mid = mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));

        assertThat("Письма нет в исходящих",
                authClient,
                hasMsgIn(subj, folderList.outgoingFID()));


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
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

        mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));


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


        String msgIdOfLetter = mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), messageId, shouldBe(ok200()));
        String mid = waitWith.fid(folderList.outgoingFID()).subj(subj).waitDeliver().getMid();


        assertThat("message_id в ответе сендбернара не совпадает с переданным", msgIdOfLetter, equalTo(messageId));


        String messageIdHeader = byMid(mid).getHeader("message-id");
        assertThat("message_id в ответе mbody не совпадает с переданным в sendbernar", messageIdHeader, equalTo(messageId));
    }

    @Test
    @Title("Отправляем спамовое письмо колбеком и проверяем результат")
    public void sendSpamMessageWithCallback() throws Exception {
        String to = authClient.acc().getSelfEmail();


        String mid = saveDraft()
                .withTo(to)
                .withText(WmiConsts.STRONG_SPAM)
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        Source source = new MidsSource(mid);


        Mops.complexMove(authClient, folderList.outgoingFID(), source)
                .post(shouldBe(okSync()));

        Mops.label(authClient, source, labelsToBeSet()
                .stream()
                .map(this::lidByTitle)
                .collect(Collectors.toList()))
                .post(shouldBe(okSync()));


        callback(mid, to, subj, checkSendingSpamMessageWithCallback());

        postCheckSendingMessage(mid);
    }

    @Test
    @Title("Проверка отмены отправки письма")
    public void shouldCancelSendDelayedMessage() throws Exception {
        long timeout = SECONDS.toMillis(10);
        String mid = mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));

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
