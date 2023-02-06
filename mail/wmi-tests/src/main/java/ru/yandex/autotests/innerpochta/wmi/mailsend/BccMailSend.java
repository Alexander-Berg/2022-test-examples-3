package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj.getMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.Obj.XMLVERSION_DARIA2;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

@Aqua.Test
@Title("Отправка письма с BCC")
@Description("Смотрим что у адресата bcc не видно, а в отправленных видно [DARIA-22825]")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "Bccmailsend")
public class BccMailSend extends BaseTest {
    private MailSendMsgObj msg;

    @ClassRule
    public static HttpClientManagerRule recieverTo = auth().with("DotNamed");

    @ClassRule
    public static HttpClientManagerRule recieverBcc = auth().with("BccmailsendBcc");

    @Rule
    public CleanMessagesRule clean = with(authClient).outbox();

    @Rule
    public CleanMessagesRule cleanTo = with(recieverTo).inbox();

    @Rule
    public CleanMessagesRule cleanBcc = with(recieverBcc).inbox();


    @Before
    public void prepare() throws Exception {
        // Заготовки писем
        msg = msgFactory.getSimpleEmptySelfMsg().setTo(recieverTo.acc().getSelfEmail())
                .setBcc(recieverBcc.acc().getSelfEmail());

        clean.subject(msg.getSubj());
        cleanTo.subject(msg.getSubj());
        cleanBcc.subject(msg.getSubj());
    }

    @Test
    @Issues({@Issue("DARIA-22825"), @Issue("MPROTO-2775")})
    @Description("Отправка пустого письма самому себе\n" +
            "- Просто проверка что дошло")
    public void sendEmailBcc() throws Exception {
        logger.warn("Отправляем письмо с bcc [DARIA-22825]");
        api(MailSend.class).params(msg).post().via(hc);

        logger.info("У отправителя");
        String mid = waitWith.subj(msg.getSubj()).inFid(folderList.sentFID()).waitDeliver().getMid();
        logger.info("У получателя");
        String midTo = waitWith.subj(msg.getSubj()).usingHC(recieverTo.authHC()).waitDeliver().getMid();
        logger.info("У того кто в bcc");
        String midBcc = waitWith.subj(msg.getSubj()).usingHC(recieverBcc.authHC()).waitDeliver().getMid();

        //у получателя
        //сломали специально: MPROTO-2694
        assertThat("В отправленных нет BCC [DARIA-22825]", msgVia(mid, hc).bccEmail(),
                is(recieverBcc.acc().getSelfEmail()));
        assertThat("Получатель видит BCC", msgVia(midTo, recieverTo.authHC()).bccEmail(), is(""));

        //у получателя bcc
        Message messageBcc = msgVia(midBcc, recieverBcc.authHC());
        assertThat("BCC видит BCC", messageBcc.bccEmail(), is(""));
        assertThat("BCC видит BCC вместо получателя", messageBcc.toEmail(), is(recieverTo.acc().getSelfEmail()));

    }

    private Message msgVia(String mid, DefaultHttpClient hc) throws IOException {
        return api(Message.class).params(getMsg(mid).setXmlVersion(XMLVERSION_DARIA2)).post().via(hc);
    }

}
