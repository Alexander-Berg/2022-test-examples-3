package ru.yandex.autotests.innerpochta.api;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgsIn;


@Aqua.Test
@Title("[API] Отправка письма самому себе")
@Description("Отправка простого письма через мобильный клиент")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = SendSimpleMail.LOGIN_GROUP)
public class SendSimpleMail extends BaseTest {

    public static final String LOGIN_GROUP = "ApiFunkTest";

    @Test
    @Description("Авторизируемся через токен, имитируя мобильного клиента.\n" +
            "Отсылаем самому себе письмо через API")
    public void sendSimpleMail() throws Exception {

        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend("Hello world from simple api send");

        api(MailSend.class).params(msg).post().via(hc);
        // ищем письмо с куками
        String mid = waitWith.subj(msg.getSubj()).waitDeliver().getMid();

        api(MailboxOper.class).params(MailboxOperObj.deleteOneMsg(mid)).post().via(hc);
        assertThat(hc, hasMsgsIn(msg.getSubj(), 0, folderList.defaultFID()));
    }

}
