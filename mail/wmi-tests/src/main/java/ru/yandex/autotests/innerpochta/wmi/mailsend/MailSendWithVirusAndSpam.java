package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgs;

@Aqua.Test
@Title("Отправка писем. Письмо со спамом и вирусами")
@Description("При отправке в результате должно быть указано что это спам или вирус")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@RunWith(value = Parameterized.class)
@Credentials(loginGroup = "VirusAndSpamSend")
@Issue("DARIA-44719")
public class MailSendWithVirusAndSpam extends BaseTest {

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() throws Exception {
        Object[][] data = new Object[][]{
                new Object[]{WmiConsts.VIRUS_CODE, "virus_found"},
                //DARIA-41017
                new Object[]{WmiConsts.LIGHT_SPAM, "captcha_request"},
                new Object[]{WmiConsts.STRONG_SPAM, "strongspam_found"},
        };
        return Arrays.asList(data);
    }

    @Parameterized.Parameter
    public String messageContent;
    @Parameterized.Parameter(1)
    public String resultExpected;


    @Test
    @Title("Письма с вирусом и спамом")
    @Description("Отправляем себе письма с вирусом, легким и тяжелым спамом.\n" +
            "Проверяем конкретный ответ от руки")
    public void sendMessage() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend(messageContent);

        MailSend msOper = api(MailSend.class).params(msg);
        assertEquals("Ожидался конкретный ответ от ручки", resultExpected, msOper.post().via(hc).getResultValue());
        Thread.sleep(5000);

        assertThat("Не ожидалось доставленных писем", hc, hasMsgs(msg.getSubj(), 0));
    }


    @Test
    @Title("Письма с вирусом и спамом")
    @Description("Отправляем себе письма с вирусом, легким и тяжелым спамом.\n" +
            "Проверяем конкретный ответ от руки")
    public void sendMessageViaJsx() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend(messageContent).setTtypePlain();

        MailSend msOper = jsx(MailSend.class).params(msg);
        assertEquals("Ожидался конкретный ответ от ручки", resultExpected, msOper.post().via(hc).getStatusValue());
        Thread.sleep(5000);

        assertThat("Не ожидалось доставленных писем", hc, hasMsgs(msg.getSubj(), 0));
    }
}
