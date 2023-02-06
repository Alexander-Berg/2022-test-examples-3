package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
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
import ru.yandex.autotests.innerpochta.wmi.core.oper.SendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj.getMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.Obj.XMLVERSION_DARIA2;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

@Aqua.Test
@Title("Сохранение в черновики отложенного письма")
@Description("Во время редактирования отложенного, сохраняем его в черновики")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "Lamertester")
public class DelayedSaveToDraft extends BaseTest {
    private MailSendMsgObj msg;
    private MailSendMsgObj msgDraft;

    @Rule
    public CleanMessagesRule clean = with(authClient).outbox();

    @Before
    public void prepare() throws Exception {
        // Заготовки писем
        msg = msgFactory.getDelayedMsg(SECONDS.toMillis(15));
        clean.subject(msg.getSubj());
    }

    @Test
    @Title("Сохраняем исходящие в черновик")
    @Description("Отправка пустого письма самому себе\n" +
            "- Просто проверка что дошло")
    public void simpleEmptyMail() throws Exception {
        logger.warn("Сохраняем исходящие в черновик");

        String mid = jsx(SendMessage.class).params(msg).post().via(hc).as(MailSend.class).getStoremidValue();
//        jsx(SendMessage.class).params(saveToDraft(mid, msg)).post().via(hc).withDebugPrint();

        msgVia(mid, hc).assertResponse(not(containsString("<error")));
    }

    private Message msgVia(String mid, DefaultHttpClient hc) throws IOException {
        return api(Message.class).params(getMsg(mid).setXmlVersion(XMLVERSION_DARIA2)).post().via(hc);
    }

}
