package ru.yandex.autotests.innerpochta.pop3.other;

import java.io.IOException;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.base.Pop3Cmd;
import ru.yandex.autotests.innerpochta.imap.core.pop3.Pop3Client;
import ru.yandex.autotests.innerpochta.pop3.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.09.14
 * Time: 18:59
 * <p/>
 * AUTOTESTPERS-141
 * https://st.yandex-team.ru/MAILORA-314
 */
@Aqua.Test
@Title("POP3. Тесты на архив")
@Features({MyStories.POP3, "ARCHIVE"})
@Stories({Pop3Cmd.DELE, Pop3Cmd.PASS, Pop3Cmd.USER, Pop3Cmd.NOOP, Pop3Cmd.STAT, Pop3Cmd.QUIT})
@Web
@Description("Проверяем архив в POP3. Удаляем письмо.")
public class ArchiveTest extends BaseTest {

    @ClassRule
    public static HttpClientManagerRule authClient = auth().with(props().account(LOGIN_GROUP).getLogin(),
            props().account(LOGIN_GROUP).getPassword());
    public Pop3Client pop3 = new Pop3Client().pop3(LOGIN_GROUP);
    public SendUtils sendWith = new SendUtils(authClient);

    public WaitUtils wait = new WaitUtils(authClient);

    @Rule
    public CleanMessagesRule clean = with(authClient).all().allfolders();

    @Test
    @Stories(MyStories.STARTREK)
    @Description("Удаляем письмо через POP3.\n" +
            "Ожидаемый результат: оно исчезает в POP3, но не исчезает в вэбах")
    @ru.yandex.qatools.allure.annotations.TestCaseId("667")
    public void testArchive() throws Exception {
        String subj = sendWith.viaProd().waitDeliver().send().getSubj();
        Thread.sleep(3000);
        //важно коннектиться после того как письмо пришло, чтобы оно появилось в POP3
        pop3.connect();
        pop3.noop(true);
        assertThat("Ожидали одно сообщение в POP3", pop3.list().length, equalTo(1));
        pop3.dele(1, true);
        //подтверждаем удаление
        pop3.quit(true);

        Thread.sleep(3000);
        //проверяем, что письмо осталось
        wait.subj(subj).count(1).waitDeliver();
    }

    @After
    public void disc() throws IOException {
        pop3.disconnect();
    }

}
