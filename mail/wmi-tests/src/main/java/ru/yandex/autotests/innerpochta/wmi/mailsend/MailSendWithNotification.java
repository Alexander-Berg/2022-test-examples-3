package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * @author jetliner
 */
@Aqua.Test
@Title("Отправка писем. Письмо с уведомлением о доставке")
@Description("Отправляем письмо и смотрим, что пришло уведомление о доставке")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "MailWithNotify")
@Issue("MPROTO-2804")
public class MailSendWithNotification extends BaseTest {
    public static final String NOTIFICATION_MAIL_SUBJECT = "Письмо успешно доставлено";


    private MailSendMsgObj msg;
    private String whatWeSend = "NotifyOnSend";

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox();

    @Rule
    public CleanMessagesRule cleanNotify = with(authClient).inbox().subject(NOTIFICATION_MAIL_SUBJECT);

    @Test
    @Title("Уведомление о доставке при отправке самому себе")
    @Description("Отправляем письмо с уведомляшкой самому себе\n" +
            "- Смотрим что уведомление не оказывает воздействия на само письмо\n" +
            "- Ищем уведомление")
    public void testSelfNotify() throws Exception {
        logger.warn("Уведомление о доставке при отправке самому себе");
        // Отправляем письмо и ждем доставки
        sendMailWithNotify(authClient.acc().getSelfEmail());
        waitWith.subj(msg.getSubj()).waitDeliver();
        // Проверяем что галочка об уведомлении никак не изменила письмо
        checkSendedMailEqualsRecieved();
        // Ищем уведомление
        searchNotificationMail();
    }

    @Test
    @Issue("WMI-378")
    @Title("Уведомление о доставке при отправке на другой адрес")
    @Description("WMI-378\n" +
            "До wmi.472 существовал баг, что уведомление об успешной доставке было не только адресату,\n" +
            "но и отправителю в отправленные\n" +
            "т.е. в итоге 2 уведомления вместо одного\n" +
            "Отправка письма с уведомлением стороннему адресу\n" +
            "В данном случае это notifymailsendtest@yandex.ru : testqa\n" +
            "(там настроен фильтр на складывание писем, содержащих notifyOnSend в корзину)")
    public void testOtherAdress() throws Exception {
        logger.warn("Уведомление о доставке при отправке на другой адрес. [WMI-378]");
        // Отправляем письмо и ждем доставки
        sendMailWithNotify("notifymailsendtest@yandex.ru");
        waitWith.subj(msg.getSubj()).inFid(folderList.sentFID()).waitDeliver();
        // Ищем уведомление
        searchNotificationMail();
    }

    /**
     * Подготовка и отправка письма с уведомлением указанному адресату
     *
     * @param to - адрес, куда отправляем
     */
    public void sendMailWithNotify(String to) throws IOException {
        msg = msgFactory.getSimpleEmptySelfMsg()
                .setTo(to)
                .setNotifyOnSend("yes")
                .setSend(whatWeSend);
        clean.subject(msg.getSubj());
        jsx(MailSend.class).params(msg).post().via(hc);
    }

    /**
     * - Проверка что письмо дошло таким каким отправили
     * : testqa
     *
     * @throws Exception *
     */
    public void checkSendedMailEqualsRecieved() throws Exception {
        String mid = jsx(MailBoxList.class).post().via(hc).getMidOfMessage(msg.getSubj());
        Message mrsp = api(Message.class)
                .params(MessageObj.getMsg(mid)).post().via(hc);

        // Первая строка
        String firstLine = mrsp.getFirstlineText();
        assertThat("Первая строка не соответствует ожиданиям.", firstLine, equalTo(whatWeSend));

        // Содержимое
        String messageContent = mrsp.getTextValueOfContentTag();
        assertThat("Содержимое не соответствует ожиданиям.", messageContent, equalTo(whatWeSend + "\n"));
    }


    /**
     * Поиск письма-уведомления
     *
     * @throws Exception *
     */
    @Step("Поиск письма уведомления")
    public void searchNotificationMail() throws Exception {
        logger.warn("Поиск нотификационного сообщения об успешной доставке");
        waitWith.subj(NOTIFICATION_MAIL_SUBJECT).errorMsg("Notification mail wasn't found").waitDeliver();
        int countNotify = jsx(MailBoxList.class)
                .post().via(hc).countMessagesInFolderWithSubj(NOTIFICATION_MAIL_SUBJECT);
        assertThat("Уведомлений о доставке нет", countNotify, greaterThan(0));
        assertEquals("Уведомлений о доставке больше положенного", 1, countNotify);
    }

}

