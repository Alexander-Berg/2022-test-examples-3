package ru.yandex.autotests.innerpochta.sendbernar;

import com.jayway.restassured.response.Response;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.CancelSendDelayedResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendDelayedResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.senddelayed.ApiSendDelayed;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder.messagesByFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.callbackRejectResponse;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FAKE_SEEN_LBL;


@Aqua.Test
@Title("Отправка отложенного письма")
@Description("Отправка отложенного письма, ожидание его получения")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Issues({@Issue("DARIA-48948"), @Issue("DARIA-48948"), @Issue("MAILPG-539"),
        @Issue("MAILPG-761"), @Issue("MAILPG-583"), @Issue("MAILADM-4558")})
@Credentials(loginGroup = "DelayedMailSend")
public class DelayedMessageTest extends BaseDelayedMessageTest {
    @Override
    String sendTime(long offset) {
        return String.valueOf((System.currentTimeMillis() + offset)/1000);
    }

    @Override
    String mailSendWithNotifyOnSend(String to, String subj, String sendTime, Function<Response, Response> shouldBe) {
        return sendDelayed()
                .withTo(to)
                .withSubj(subj)
                .withSendTime(sendTime)
                .withConfirmDelivery(ApiSendDelayed.ConfirmDeliveryParam.YES)
                .post(shouldBe)
                .as(SendDelayedResponse.class)
                .getStored()
                .getMid();
    }

    @Override
    String mailSend(String to, String subj, String sendTime, Function<Response, Response> shouldBe) {
        return sendDelayed()
                .withTo(to)
                .withSubj(subj)
                .withSendTime(sendTime)
                .post(shouldBe)
                .as(SendDelayedResponse.class)
                .getStored()
                .getMid();
    }

    @Override
    String mailSendWithSourceMid(String to, String subj, String sendTime, String sourceMid, Function<Response, Response> shouldBe) {
        return sendDelayed()
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
    String mailSend(String to, String subj, String sendTime, String messageId, Function<Response, Response> shouldBe) {
        return sendDelayed()
                .withTo(to)
                .withSubj(subj)
                .withSendTime(sendTime)
                .withMessageId(messageId)
                .post(shouldBe)
                .as(SendDelayedResponse.class)
                .getMessageId();
    }

    @Override
    void callback(String mid, String to, String subj, Function<Response, Response> shouldBe) {
        sendDelayedMessage()
                .withMid(mid)
                .withTo(to)
                .withNotifyOnSend("no")
                .withSourceMid(mid)
                .withMessageDate("0")
                .withNonemptySubject(subj)
                .withMessageId("msgId")
                .post(shouldBe);
    }

    @Override
    boolean cancel(String mid, Function<Response, Response> shouldBe) {
        return cancelSendDelayed()
                .withMid(mid)
                .post(shouldBe)
                .as(CancelSendDelayedResponse.class)
                .getAllDone();
    }

    @Override
    List<String> labelsToBeSet() {
        return Arrays.asList("delayed_message");
    }

    @Override
    Function<Response, Response> checkSendingSpamMessageWithCallback() {
        return shouldBe(callbackRejectResponse());
    }

    @Override
    void postCheckSendingMessage(String mid) {
        assertThat("Нет системной метки о неудачной отправки",
                authClient, hasMsgWithLid(mid, lidByTitle("sending_failed_label")));
    }

    @Test
    @Title("Двойная отправка отложенного письма")
    public void shouldAcceptDoubleSendingOfSingleMessage() {
        long timeout = HOURS.toMillis(10);
        String mid = mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));
        mailSendWithSourceMid(authClient.acc().getSelfEmail(), subj, sendTime(timeout), mid, shouldBe(ok200()));
    }

    @Test
    @Title("Проверяем, что дата отправки в письме совпадает с ожидаемой")
    public void expectTheDateOfSentToBeEqualsWithThePassed() throws Exception {
        long timeout = HOURS.toMillis(10);
        long shift = SECONDS.toSeconds(5);
        String expectedRecievedDate = sendTime(timeout);
        String mid = mailSend(authClient.acc().getSelfEmail(), subj, expectedRecievedDate, shouldBe(ok200()));


        String receivedDate = messagesByFolder(
                MessagesByFolderObj
                        .empty()
                        .setUid(authClient.account().uid())
                        .setFid(folderList.outgoingFID())
                        .setFirst("0")
                        .setCount("30")
        )
                .get()
                .via(authClient)
                .receivedDate(mid)
                .toString();

        assertEquals("Отображаемая дата отправки и фактическая не совпадают",
                receivedDate,
                Long.toString(Long.parseLong(expectedRecievedDate)+shift));
    }

    @Test
    @Title("Проверка меток на отложенном письме")
    public void shouldSetSystemLabelOnMessageInOutgoingAndTheMessageInSentShouldBeSeen() throws Exception {
        long timeout = MINUTES.toMillis(1);

        String mid = mailSend(authClient.acc().getSelfEmail(), subj, sendTime(timeout), shouldBe(ok200()));


        for (String label : labelsToBeSet()) {
            assertThat("Нет системной метки " + label + " на письме",
                    authClient,
                    hasMsgWithLidInFolder(mid, folderList.outgoingFID(), lidByTitle(label)));
        }

        assertThat("Письмо непрочитано",
                authClient,
                hasMsgWithLidInFolder(mid, folderList.outgoingFID(), FAKE_SEEN_LBL));
    }
}
