package ru.yandex.autotests.innerpochta.imap.unsubscribe;

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
@Title("Команда UNSUBSCRIBE. Отписываемся от системной папки")
@Features({ImapCmd.UNSUBSCRIBE})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Отписываемся от папок и подпапок системных папок\n"
        + "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class UnsubscribeSystemFolders extends BaseTest {
    private static Class<?> currentClass = UnsubscribeSystemFolders.class;

    @ClassRule
    public static final ImapClient imap = newLoginedClient(currentClass);
    @Parameterized.Parameter
    public String sysFolder;
    @Rule
    public ImapClient prodImap = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));

    @Parameterized.Parameters(name = "system_folder - {0}")
    public static Collection<Object[]> sysFolders() {
        return allSystemFolders();
    }

    @Test
    @Title("Сначала подписываемся на папки, затем отписываемся")
    @ru.yandex.qatools.allure.annotations.TestCaseId("653")
    public void simpleUnsubscribeOnSystemFolderShouldSeeOk() {
        prodImap.request(subscribe(sysFolder));
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);

        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);
        //todo: добавить проверку флагов
    }

    @Test
    @Title("Дважды отписываемся от системных папок\n"
            + "Ожидаемый результат: OK, так как фича")
    @ru.yandex.qatools.allure.annotations.TestCaseId("654")
    public void doubleUnsubscribeOnSystemFolderShouldSeeOk() {
        prodImap.request(subscribe(sysFolder));
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);

        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);
    }

    @Test
    @Title("Отписываемся от системной папки, подписываемся, потом опять отписываемся\n")
    @ru.yandex.qatools.allure.annotations.TestCaseId("655")
    public void unsubscribeSystemFolderAfterSubscribe() {
        prodImap.request(subscribe(sysFolder));
        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);

        prodImap.request(subscribe(sysFolder));
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);

        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);
    }
}
