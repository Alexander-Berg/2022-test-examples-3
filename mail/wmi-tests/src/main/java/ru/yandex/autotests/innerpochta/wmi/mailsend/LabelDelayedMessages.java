package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.After;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.Creator;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidInFolder;


@Aqua.Test
@Title("Отложенная отправка. Проверка метки")
@Description("Создает отложенное письмо, смотрит что оно пометилось специальной меткой")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "DelayedMessageTest")
public class LabelDelayedMessages extends BaseTest {

    private String storedMid;

    @Test
    @Description("Проверка что отложенное письмо помечается меткой \"delayed_message\"")
    public void testLabelDelayedMsg() throws Exception {
        MailSendMsgObj msg = new Creator(authClient.authHC(), authClient.acc()).getMailSendMsgObjFactory()
                .getDelayedMsg(100000)
                .setTtypeHtml();
        msg.setSend(msg.getSend() + " <img src='http://intrigan.com/uploads/images/6/0/6/9/12/cb0d41bdf5.jpg'></img>");

        storedMid = jsx(SendMessage.class).params(msg).post().via(hc).as(MailSend.class).statusOk().getStoremidValue();

        String lid = api(Labels.class).post().via(hc).lidBySymbol(LabelSymbol.DELAYED_MESSAGE);

        // Получаем фид исходящих
        String folderId = jsx(FolderList.class).post().via(hc).fidBySymbol(Symbol.OUTBOX);
        assertThat(String.format("Нет системной метки %s на письме", LabelSymbol.DELAYED_MESSAGE), hc,
                hasMsgWithLidInFolder(storedMid, folderId, lid));
    }

    // Чистка
    @After
    public void clear() throws Exception {
        logger.trace("------------------------Чистка-----------------------");
        jsx(MailboxOper.class)
                .params(MailboxOperObj.deleteOneMsg(storedMid))
                .post().via(hc);
    }
}
