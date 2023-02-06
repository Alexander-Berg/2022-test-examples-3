package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;


@Aqua.Test
@Title("Ручка send_message")
@Description("Смотрим что у адресата bcc не видно, а в отправленных видно")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Issues(@Issue("MAILPG-771"))
@Credentials(loginGroup = "Bccmailsend")
public class BccTest extends BaseSendbernarClass {
    @ClassRule
    public static HttpClientManagerRule receiverTo = auth().with("DotNamed");

    @ClassRule
    public static HttpClientManagerRule receiverBcc = auth().with("BccmailsendBcc");

    @Rule
    public CleanMessagesMopsRule cleanTo = new CleanMessagesMopsRule(receiverTo).inbox().outbox();

    @Rule
    public CleanMessagesMopsRule cleanBcc = new CleanMessagesMopsRule(receiverBcc).inbox().outbox();

    @Test
    @Issues({@Issue("DARIA-22825"), @Issue("MPROTO-2775")})
    @Description("Отправляю письмо двум пользователям: одному в BCC, другому в TO")
    public void shouldSendEmailBcc() throws Exception {
        sendMessage()
                .withTo(receiverTo.acc().getSelfEmail())
                .withBcc(receiverBcc.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid    = waitWith.subj(subj).sent().waitDeliver().getMid();
        String midTo  = waitWith.subj(subj).usingHttpClient(receiverTo).waitDeliver().getMid();
        String midBcc = waitWith.subj(subj).usingHttpClient(receiverBcc).waitDeliver().getMid();
        Message messageBcc = byMid(midBcc, receiverBcc);


        assertThat("В отправленных нет BCC [DARIA-22825]",
                byMid(mid).bccEmail(),
                is(receiverBcc.acc().getSelfEmail()));
        assertThat("Получатель видит BCC",
                byMid(midTo, receiverTo).bccEmail(),
                is(""));
        assertThat("BCC видит BCC",
                messageBcc.bccEmail(),
                is(""));
        assertThat("BCC видит BCC вместо получателя",
                messageBcc.toEmail(),
                is(receiverTo.acc().getSelfEmail()));
    }

}
