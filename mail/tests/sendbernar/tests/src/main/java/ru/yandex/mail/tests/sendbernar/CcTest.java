package ru.yandex.mail.tests.sendbernar;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;

@Aqua.Test
@Title("Ручка send_message")
@Description("Смотрим, что у адресата корректно отображается CC")
@Stories("mail send")
@Issue("DARIA-51811")
public class CcTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.cc;
    }

    private UserCredentials recieverTo = new UserCredentials(Accounts.ccReceiverTo);

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(recieverTo).inbox().outbox();

    @Test
    @Issue("DARIA-29751")
    @Title("Должны отправлять письмо с email с русским доменом в cc")
    @Description("Первый раз возникало в DARIA-29751")
    public void shouldSendWithRussianCc() throws Exception {
        sendMessage()
                .withTo(authClient.account().email())
                .withCc(recieverTo.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        assertThat("Неверный СС",
                byMid(mid).ccEmail(),
                equalTo(recieverTo.account().email()));
    }

    @Test
    @Title("СС должен быть пустым когда совпадает с адресатом")
    public void shouldSendWithSelfEmailAsCC() throws Exception {
        sendMessage()
                .withTo(authClient.account().email())
                .withCc(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        assertThat("СС должен быть пустым когда совпадает с адресатом",
                byMid(mid).ccEmail(),
                isEmptyString());
    }
}