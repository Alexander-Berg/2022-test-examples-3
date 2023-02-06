package ru.yandex.mail.tests.sendbernar;

import io.restassured.response.Response;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.sendbernar.generated.CancelSendDelayedResponse;
import ru.yandex.mail.tests.sendbernar.generated.SendDelayedResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.tests.hound.MessagesByFolder.messagesByFolder;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLidInFolder;

@Aqua.Test
@Title("Отправка отложенного письма")
@Description("Отправка отложенного письма, ожидание его получения")
@Stories("mail send")
@Issues({@Issue("DARIA-48948"), @Issue("DARIA-48948"), @Issue("MAILPG-539"),
        @Issue("MAILPG-761"), @Issue("MAILPG-583"), @Issue("MAILADM-4558")})
public class DelayedMessageTest extends BaseDelayedMessageTest {
    @Override
    AccountWithScope mainUser() {
        return Accounts.delayedMailSend;
    }

    @Override
    String sendTime(long offset) {
        return String.valueOf((System.currentTimeMillis() + offset)/1000);
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
    void callback(String mid, Function<Response, Response> shouldBe) {
        sendDelayedMessage()
                .withMid(mid)
                .get(shouldBe);
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

    @Test
    @Title("Проверяем, что дата отправки в письме совпадает с ожидаемой")
    public void expectTheDateOfSentToBeEqualsWithThePassed() throws Exception {
        long timeout = HOURS.toMillis(10);
        String expectedRecievedDate = sendTime(timeout);
        String mid = mailSend(authClient.account().email(), subj, expectedRecievedDate, shouldBe(ok200()));

        String receivedDate = messagesByFolder(
                HoundApi.apiHound(HoundProperties.properties().houndUri(), props().getCurrentRequestId())
                        .messagesByFolder()
                        .withUid(authClient.account().uid())
                        .withFid(folderList.outgoingFID())
                        .withFirst("0")
                        .withCount("30")
                        .post(shouldBe(HoundResponses.ok200()))
        )
                .receivedDate(mid)
                .toString();

        assertEquals("Отображаемая дата отправки и фактическая не совпадают",
                receivedDate,
                expectedRecievedDate);
    }

    @Test
    @Title("Проверка меток на отложенном письме")
    public void shouldSetSystemLabelOnMessageInOutgoingAndTheMessageInSentShouldBeSeen() throws Exception {
        long timeout = MINUTES.toMillis(1);

        String mid = mailSend(authClient.account().email(), subj, sendTime(timeout), shouldBe(ok200()));


        for (String label : labelsToBeSet()) {
            assertThat("Нет системной метки " + label + " на письме",
                    authClient,
                    hasMsgWithLidInFolder(mid, folderList.outgoingFID(), lidByTitle(label)));
        }

        assertThat("Письмо непрочитано",
                authClient,
                hasMsgWithLidInFolder(mid, folderList.outgoingFID(), seenLid()));
    }
}
