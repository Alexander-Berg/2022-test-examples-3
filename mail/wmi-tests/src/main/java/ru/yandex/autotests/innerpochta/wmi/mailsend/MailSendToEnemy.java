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
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 19.01.15
 * Time: 18:12
 * AUTOTESTPERS-158
 */
@Aqua.Test
@Title("Отправка писем. Отправляем письмо на вражеский email")
@Description("Отправляем письма с различными на вражеский email.\n " +
        "Проверяем, что сохраняем в папку \"Отправленные\"")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "SendToEnemyMailTest")
@RunWith(Parameterized.class)
public class MailSendToEnemy extends BaseTest {

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient)
            .all().allfolders();

    //параметризуем по получателю
    @Parameterized.Parameters(name = "To <{0}>")
    public static Collection<Object[]> data() throws Exception {
        List<Object[]> toList = new ArrayList<>();
        toList.add(new Object[]{"antivagcom@gmail.com "});
        toList.add(new Object[]{"alena-test1@mail.ru"});
        toList.add(new Object[]{"vicdev@yahoo.com"});
        toList.add(new Object[]{"alena-test1@rambler.ru"});

        return toList;
    }

    private String to;

    public MailSendToEnemy(String to) {
        this.to = to;
    }

    @Test
    @Issues({@Issue("AUTOTESTPERS-158"), @Issue("MPROTO-1550")})
    @Description("Отсылаем письмо на вражеский адресс: проверяем, что оно сохраняетя в отправленных")
    public void sendMailToEnemyLogin() throws Exception {
        String subj = Util.getRandomString();
        jsx(MailSend.class).params(msgFactory.getEmptyObj().setTo(to)
                .setSubj(subj).setSend("Это тестовое письмо от yandex. " +
                        "Пожалуйста не отвечайте на него." + Util.getLongString())).post().via(hc).statusOk();
        waitWith.subj(subj).inFid(folderList.sentFID()).waitDeliver()
                .errorMsg("Не появилось письмо в папке \"Отправленные\" для " + to);

    }
}
