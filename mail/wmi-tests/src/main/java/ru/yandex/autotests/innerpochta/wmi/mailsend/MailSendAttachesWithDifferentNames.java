package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Rule;
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
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.attachInMessageShouldBeSameAs;

@Aqua.Test
@Title("Отправка писем. Письма с аттачами с разным именем")
@Description("Генерирует небольшие аттачи с разными именами. Отправляет, получает, проверяет соответствие")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND, MyStories.ATTACH})
@RunWith(value = Parameterized.class)
@Credentials(loginGroup = MailSendAttachesWithDifferentNames.GROUP_NAME)
public class MailSendAttachesWithDifferentNames extends BaseTest {

    public static final String GROUP_NAME = "MailsendAttaDiffNames";


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws Exception {
        Object[][] data = new Object[][]
                {
                        new Object[]{"attach2.zip"},
                        new Object[]{Util.getRandomString()},
                        new Object[]{"100500"},
                        new Object[]{"test test test"},
                        new Object[]{"z x xc c"},
                        new Object[]{"z x ,xc c"},
                        new Object[]{"test.jpg"},
                        //new Object[]{"аттач.jpg"},  //Надо доработать способ отправки -
                        // иначе имя аттача не передается корректно
                        //new Object[]{"оттач с пробелами.jpg"},
                };
        return Arrays.asList(data);
    }

    private String attachName;

    public MailSendAttachesWithDifferentNames(String attachName) {
        this.attachName = attachName;
    }

    private MailSendMsgObj msg;

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox();

    @Test
    @Description("Отправляем письма с аттачами с различными именами\n" +
            "Проверяем, что отправленный аттачи совпадаеют с полученными.")
    public void sendAttachWithSpecifiedName() throws Exception {
        logger.warn("Attach name = " + attachName);

        File attach = Util.generateRandomShortFile(attachName, 64);

        msg = msgFactory.getSimpleEmptySelfMsg();
        clean.subject(msg.getSubj()); // в конце


        msg.addAtts("application", attach);
        msg.setSend("MailSendAttachesWithDifferentNames::sendAttachWithSpecifiedName()" + Util.getRandomString());
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();

        waitWith.subj(msg.getSubj()).waitDeliver();

        attachInMessageShouldBeSameAs(attach, msg.getSubj(), hc);
    }
}