package ru.yandex.autotests.innerpochta.imap.subscribe;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.SubscribeRequest.subscribe;
import static ru.yandex.autotests.innerpochta.imap.requests.UnsubscribeRequest.unsubscribe;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.rules.UnsubscribeRule.withUnsubscribeBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.04.14
 * Time: 20:12
 */
@Aqua.Test
@Title("Команда SUBSCRIBE. Подписываемся на системную папку")
@Features({ImapCmd.SUBSCRIBE})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Подписываемся на системные папки и подпапки системных папок\n"
        + "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class SubscribeSystemFolders extends BaseTest {
    private static Class<?> currentClass = SubscribeSystemFolders.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Parameterized.Parameter
    public String sysFolder;
    @Rule
    public ImapClient prodImap = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));

    @Parameterized.Parameters(name = "system_folder - {0}")
    public static Collection<Object[]> sysFolders() {
        return allSystemFolders();
    }

    @Test
    @Title("Просто подписываемся на системные папки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("635")
    public void simpleSubscribeOnSystemFolderShouldSeeOk() {
        imap.request(subscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);
        //todo: добавить проверку флагов
    }

    @Test
    @Title("Дважды подписываемся на системные папки. \n"
            + "Ожидаемый результат: OK, так как фича")
    @ru.yandex.qatools.allure.annotations.TestCaseId("636")
    public void doubleSubscribeOnSystemFolderShouldSeeOk() {
        imap.request(subscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);
        imap.request(subscribe(sysFolder)).shouldBeOk();
        //todo: добавить проверку флагов
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);
    }

    @Test
    @Title("Подписываемся на системную папку, отписываемся, потом опять подписываемся\n")
    @ru.yandex.qatools.allure.annotations.TestCaseId("637")
    public void subscribeSystemFolderAfterUnsubscribe() {
        imap.request(subscribe(sysFolder)).shouldBeOk();
        prodImap.request(unsubscribe(sysFolder));
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);
        imap.request(subscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);
    }
}
