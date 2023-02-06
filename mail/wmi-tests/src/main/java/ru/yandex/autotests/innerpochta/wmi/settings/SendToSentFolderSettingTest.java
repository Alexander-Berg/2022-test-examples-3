package ru.yandex.autotests.innerpochta.wmi.settings;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsSetupUpdateSomeObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetupUpdateSome;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.SettingsValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgsIn;

/**
 * DARIA-13289
 * Тестируем ручку Mail_send, которая раньше на свойство сохранять ли в отправленных не смотрела
 */
@Aqua.Test
@Title("Тестирование настроек. Тестирование галки 'сохранять в отправленных'")
@Description("Вырубаем галку и смотрим, что в отправленных не появились письма")
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Issue("DARIA-13289")
@Credentials(loginGroup = "SendToSent")
public class SendToSentFolderSettingTest extends BaseTest {

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).inbox().outbox().all();

    @Test
    @Title("Отправка через send_message c выключенной настройкой save_sent")
    @Description("Вырубаем галку сохранения в отправленных, отправляем через send_message\n" +
            "- Смотрим, что после доставки письма во входящие в отправленных оно не появилось")
    public void testSaveSentSettingIsOff() throws Exception {
        // Вырубаем галку
        SettingsSetupUpdateSomeObj setupUpdateSome = SettingsSetupUpdateSomeObj.getObjToTurnOFF("save_sent");
        jsx(SettingsSetupUpdateSome.class).params(setupUpdateSome).post().via(hc);

        assertThat(hc, withWaitFor(hasSetting("save_sent", equalTo("")), SECONDS.toMillis(5)));
        // Отправка письма
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg();
        Oper mailSendOper = jsx(SendMessage.class)
                .params(msg.setSend("Это сообщение не должно сохраниться в отправленных, т.к. галка выключена"));
        final String subject = msg.getSubj();
        mailSendOper.post().via(hc);

        // Тут ожидание письма
        waitWith.subj(subject).waitDeliver();

        assertThat("Отправленное письмо при выключенной галке 'сохранять в отправленных' " +
                "найдено в отправленных [WMI-640] [DARIA-13289]",
            hc, hasMsgsIn(subject, 0, folderList.sentFID()));
    }

    @Test
    @Title("Отправка через mail_send с параметром nosave=yes")
    @Description("Если в mail_send передан параметр nosave=yes, письмо не должно сохраняться в Отправленных")
    public void testSaveSentNoSave() throws IOException {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg();
        Oper mailSendOper = jsx(MailSend.class).params(msg
            .setSend("Это сообщение не должно сохраниться в отправленных, т.к. передан параметр nosave")
            .setNosave("yes"));
        final String subject = msg.getSubj();

        mailSendOper.post().via(hc);
        waitWith.subj(subject).waitDeliver();

        assertThat("Письмо, отправленное через mail_send с параметром nosave, не должно попасть в Отправленные",
            hc, hasMsgsIn(subject, 0, folderList.sentFID()));
    }

}
