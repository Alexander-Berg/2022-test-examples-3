package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils.*;

/**
 * /api/mail_send?compose_check=aa158234ad02f9d0985ca79d6c118c32&
 * ttype=plain&
 * to=udav.dev@yandex.ru&
 * subj=dublicate&
 * overwrite=123456789
 * <p/>
 * Начинаем писать письмо. Сохраняем в черновики. В другой вкладке удаляем черновик (в том числе и из "Удаленные").
 * Возвращаемся в первую вкладку и отправляем письмо
 * Ошибочно: в отправленных 2 письма,
 * Должно быть: одно письмо
 * Fix in: WMI-333
 * Подробности: DARIA-11700
 *
 * @author lanwen
 */
@Aqua.Test
@Title("Отправка писем. Письмо с невалидным параметром overwrite")
@Description("Связано с багом при асинхронной отправке и 2х письмах в отправленных. Fix in: [WMI-333]")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "NotValidOverwriteMailSend")
public class NotValidOverwriteMailSend extends BaseTest {

    @Before
    public void prepare() throws Exception {
        // Подготовка письма
        msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend("testNotValidOverwriteMailSend")
                .setOverwrite("123456789")
                .setTtypePlain();
        clean.subject(msg.getSubj());
    }

    //TODO тест на удаление 1 письма

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox();

    private MailSendMsgObj msg;

    @Test
    @Description("Отправка письма с невалидным mid в overwrite\n" +
            "В отправленных должно появиться 1 письмо, а не 2")
    public void wrongOverwriteSend() throws Exception {
        logger.warn("Отправка письма с невалидным overwrite. Fix in: [WMI-333], More info: [DARIA-11700]");
        MailBoxList mblResp = jsx(MailBoxList.class)
                .params(MailBoxListObj.inFid(folderList.sentFID()));
        int beforeCount = mblResp.post().via(hc).countMessagesInFolderWithSubj(msg.getSubj());
        jsx(MailSend.class).params(msg).post().via(hc);

        waitWith.subj(msg.getSubj()).inFid(folderList.sentFID()).waitDeliver();

        waitWith.subj(msg.getSubj()).waitDeliver();

        int afterCount = mblResp.post().via(hc).countMessagesInFolderWithSubj(msg.getSubj());
        int diff = afterCount - beforeCount;
        assertThat("Expected 1 msg in OUTBOX", diff, is(1));
    }

}
