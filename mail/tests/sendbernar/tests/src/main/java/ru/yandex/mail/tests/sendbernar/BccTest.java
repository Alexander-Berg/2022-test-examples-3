package ru.yandex.mail.tests.sendbernar;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;

@Aqua.Test
@Title("Ручка send_message")
@Description("Смотрим что у адресата bcc не видно, а в отправленных видно")
@Stories("mail send")
@Issues(@Issue("MAILPG-771"))
public class BccTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.bcc;
    }

    private UserCredentials receiverTo = new UserCredentials(Accounts.bccReceiverTo);

    private UserCredentials receiverBcc = new UserCredentials(Accounts.bccReceiverBcc);

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(receiverTo).inbox().outbox();

    @Rule
    public CleanMessagesMopsRule cleanBcc = new CleanMessagesMopsRule(receiverBcc).inbox().outbox();

    @Test
    @Issues({@Issue("DARIA-22825"), @Issue("MPROTO-2775")})
    @Description("Отправляю письмо двум пользователям: одному в BCC, другому в TO")
    public void shouldSendEmailBcc() throws Exception {
        sendMessage()
                .withTo(receiverTo.account().email())
                .withBcc(receiverBcc.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid    = waitWith.subj(subj).sent().waitDeliver().getMid();
        String midTo  = waitWith.subj(subj).usingHttpClient(receiverTo).waitDeliver().getMid();
        String midBcc = waitWith.subj(subj).usingHttpClient(receiverBcc).waitDeliver().getMid();
        Message messageBcc = byMid(midBcc, receiverBcc);


        assertThat("В отправленных нет BCC [DARIA-22825]",
                byMid(mid).bccEmail(),
                is(receiverBcc.account().email()));
        assertThat("Получатель видит BCC",
                byMid(midTo, receiverTo).bccEmail(),
                is(""));
        assertThat("BCC видит BCC",
                messageBcc.bccEmail(),
                is(""));
        assertThat("BCC видит BCC вместо получателя",
                messageBcc.toEmail(),
                is(receiverTo.account().email()));
    }
}
