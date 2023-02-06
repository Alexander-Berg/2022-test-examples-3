package ru.yandex.autotests.innerpochta.sendbernar;


import com.jayway.restassured.specification.ResponseSpecification;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.CaptchaResponse;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.senddelayed.ApiSendDelayed;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;


@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляем письма со спамом и вирусами и проверяем код ответа")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@RunWith(DataProviderRunner.class)
@Credentials(loginGroup = "VirusAndSpamSend")
@Issues({@Issue("DARIA-44719"), @Issue("DARIA-41017")})
public class SpamAndVirusTest extends BaseSendbernarClass {
    @DataProvider
    public static Object[][] cases() {
        return new Object[][] {
                {WmiConsts.VIRUS_CODE, SendbernarResponses.virus409()},
                {WmiConsts.LIGHT_SPAM, SendbernarResponses.lightSpam402()},
                {WmiConsts.STRONG_SPAM, SendbernarResponses.strongSpam409()}
        };
    }

    @Test
    @Description("Отправляем себе испорченные письма и проверяем ответы ручки")
    @UseDataProvider("cases")
    public void shouldReturnErrorOnSendingMessage(String messageContent, ResponseSpecification resultExpected) {
        sendMessage()
                .withText(messageContent)
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(resultExpected));
    }

    private static String sendTime(long offset) {
        return String.valueOf((System.currentTimeMillis() + offset)/1000);
    }

    @Test
    @Description("Отложенно отправляем себе испорченные письма и проверяем ответы ручки")
    @UseDataProvider("cases")
    public void shouldReturnErrorOnSendingDelayedMessage(String messageContent, ResponseSpecification resultExpected) {
        long timeout = HOURS.toMillis(10);
        sendDelayed()
                .withSendTime(sendTime(timeout))
                .withText(messageContent)
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(resultExpected));
    }

    @Test
    @Description("Проверяем, что на неправильную капчу снова возвращается капча")
    public void shouldReturnCaptchaOnWrongCaptchaResultWasEntered() {
        CaptchaResponse resp = sendMessage()
                .withText(WmiConsts.LIGHT_SPAM)
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(SendbernarResponses.lightSpam402()))
                .as(CaptchaResponse.class);


        String mid = sendMessage()
                .withSourceMid(resp.getStored().getMid())
                .withText(WmiConsts.LIGHT_SPAM)
                .withTo(authClient.acc().getSelfEmail())
                .withCaptchaKey(resp.getCaptcha().getKey())
                .withCaptchaEntered(Util.getRandomString())
                .post(shouldBe(SendbernarResponses.lightSpam402()))
                .as(CaptchaResponse.class)
                .getStored()
                .getMid();


        Message message = byMid(mid);


        assertThat("Нет хедера о плохой капче неверный",
                message.getHeader("X-Yandex-Captcha-Entered"),
                equalTo("bad"));
    }

    @Test
    @Issues({@Issue("MAILPG-4378"), @Issue("MAILPG-4705")})
    @Description("Игнорируем запрос на проверку капчи, если установлен флаг captcha_passed")
    public void shouldSkipCaptchaIfForcedTo() {
        sendMessage()
                .withText(WmiConsts.LIGHT_SPAM)
                .withTo(authClient.acc().getSelfEmail())
                .withCaptchaPassed(ApiSendMessage.CaptchaPassedParam.YES)
                .post(shouldBe(ok200()));

        assertThat("Письмо должно отправиться",
                waitWith.subj(WmiConsts.NO_SUBJECT_TITLE).waitDeliver().getMid(),
                notNullValue());

        long timeout = SECONDS.toMillis(10);
        sendDelayed()
                .withText(WmiConsts.LIGHT_SPAM)
                .withTo(authClient.acc().getSelfEmail())
                .withSendTime(sendTime(timeout))
                .withCaptchaPassed(ApiSendDelayed.CaptchaPassedParam.YES)
                .post(shouldBe(ok200()));

        assertThat("Письмо должно отправиться",
                waitWith.subj(WmiConsts.NO_SUBJECT_TITLE).waitDeliver().getMid(),
                notNullValue());
    }
}
