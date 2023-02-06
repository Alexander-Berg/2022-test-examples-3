package ru.yandex.mail.tests.sendbernar;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.restassured.specification.ResponseSpecification;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.sendbernar.generated.CaptchaResponse;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;

@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляем письма со спамом и вирусами и проверяем код ответа")
@Stories("mail send")
@RunWith(DataProviderRunner.class)
@Issues({@Issue("DARIA-44719"), @Issue("DARIA-41017")})
public class SpamAndVirusTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.virusAndSpam;
    }

    private static final String VIRUS_CODE = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
    private static final String LIGHT_SPAM = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X";
    private static final String STRONG_SPAM = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STRONG-ANTI-UBE-TEST-EMAIL*C.34X";

    @DataProvider
    public static Object[][] cases() {
        return new Object[][] {
                {VIRUS_CODE, SendbernarResponses.virus409()},
                {LIGHT_SPAM, SendbernarResponses.lightSpam402()},
                {STRONG_SPAM, SendbernarResponses.strongSpam409()}
        };
    }

    @Test
    @Description("Отправляем себе испорченные письма и проверяем ответы ручки")
    @UseDataProvider("cases")
    public void shouldReturnErrorOnSendingMessage(String messageContent, ResponseSpecification resultExpected) {
        sendMessage()
                .withText(messageContent)
                .withTo(authClient.account().email())
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
                .withTo(authClient.account().email())
                .post(shouldBe(resultExpected));
    }

    @Test
    @Description("Проверяем, что на неправильную капчу снова возвращается капча")
    public void shouldReturnCaptchaOnWrongCaptchaResultWasEntered() {
        CaptchaResponse resp = sendMessage()
                .withText(LIGHT_SPAM)
                .withTo(authClient.account().email())
                .post(shouldBe(SendbernarResponses.lightSpam402()))
                .as(CaptchaResponse.class);


        String mid = sendMessage()
                .withSourceMid(resp.getStored().getMid())
                .withText(LIGHT_SPAM)
                .withTo(authClient.account().email())
                .withCaptchaKey(resp.getCaptcha().getKey())
                .withCaptchaEntered(Random.string())
                .post(shouldBe(SendbernarResponses.lightSpam402()))
                .as(CaptchaResponse.class)
                .getStored()
                .getMid();


        Message message = byMid(mid);


        assertThat("Нет хедера о плохой капче неверный",
                message.getHeader("X-Yandex-Captcha-Entered"),
                equalTo("bad"));
    }
}
