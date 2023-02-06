package ru.yandex.autotests.innerpochta.wmi.mailsend;

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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj.getMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.Obj.XMLVERSION_DARIA2;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 31.10.13
 * Time: 21:24
 */
@Aqua.Test
@Title("Отправка письма с CC")
@Description("Смотрим, что у адресата корректно отображается CC")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "Mailsendcc")
@Issue("DARIA-51811")
public class CCMailSend extends BaseTest {

    @ClassRule
    public static HttpClientManagerRule pddAuth = auth().with("Adminkapdd");

    @Rule
    public CleanMessagesRule clean = with(authClient).all().inbox().outbox();

    @Test
    @Issue("DARIA-29751")
    @Title("Должны отправлять письмо с email с русским доменом в cc")
    @Description("Первый раз возникало в DARIA-29751")
    public void sendWithRussianCc() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg().setCc(pddAuth.acc().getSelfEmail());
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        String mid = waitWith.subj(msg.getSubj()).waitDeliver().getMid();
        
        Message message = api(Message.class).params(getMsg(mid)
                .setXmlVersion(XMLVERSION_DARIA2)).post().via(hc);
        assertThat("Неверный СС", message.ccEmail(), equalTo(pddAuth.acc().getSelfEmail()));

        waitWith.reset().subj(msg.getSubj()).usingHC(pddAuth.authHC()).waitDeliver();
    }

    @Test
    @Title("СС должен быть пустым когда совпадает с адресатом")
    public void sendWithSelfEmailAsCC() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg().setCc(authClient.acc().getSelfEmail());
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        String mid = waitWith.subj(msg.getSubj()).waitDeliver().getMid();

        Message message = api(Message.class).params(getMsg(mid).setXmlVersion(XMLVERSION_DARIA2)).post().via(hc);
        assertThat("СС должен быть пустым когда совпадает с адресатом", message.ccEmail(), equalTo(""));
    }
}
