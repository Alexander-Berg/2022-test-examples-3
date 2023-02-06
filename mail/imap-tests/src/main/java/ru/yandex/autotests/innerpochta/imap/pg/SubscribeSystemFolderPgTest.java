package ru.yandex.autotests.innerpochta.imap.pg;

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
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Arrays.asList;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.SubscribeRequest.subscribe;
import static ru.yandex.autotests.innerpochta.imap.requests.UnsubscribeRequest.unsubscribe;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.rules.UnsubscribeRule.withUnsubscribeBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;

@Aqua.Test
@Title("Команда UNSUBSCRIBE. Подписываемся на системные папки")
@Features({ImapCmd.SUBSCRIBE, "PG"})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Отписываемся от папок и подпапок системных папок\n"
        + "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class SubscribeSystemFolderPgTest extends BaseTest {
    private static Class<?> currentClass = SubscribeSystemFolderPgTest.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Parameterized.Parameter
    public String sysFolder;
    @Rule
    public ImapClient prodImap = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return asList(new Object[][]{
                {systemFolders().getSent()},
                {systemFolders().getDeleted()},
                {systemFolders().getDrafts()},
        });
    }

    @Test
    @Title("Просто подписываемся на системные папки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("382")
    public void simpleSubscribeOnSystemFolderShouldSeeOk() {
        imap.request(subscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);
        //todo: добавить проверку флагов
    }

    @Test
    @Title("Дважды подписываемся на системные папки. \n"
            + "Ожидаемый результат: OK, так как фича")
    @ru.yandex.qatools.allure.annotations.TestCaseId("383")
    public void doubleSubscribeOnSystemFolderShouldSeeOk() {
        imap.request(subscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);
        imap.request(subscribe(sysFolder)).shouldBeOk();
        //todo: добавить проверку флагов
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);
    }

    @Test
    @Title("Подписываемся на системную папку, отписываемся, потом опять подписываемся\n")
    @ru.yandex.qatools.allure.annotations.TestCaseId("384")
    public void subscribeSystemFolderAfterUnsubscribe() {
        imap.request(subscribe(sysFolder)).shouldBeOk();
        prodImap.request(unsubscribe(sysFolder));
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);
        imap.request(subscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);
    }

    @Test
    @Title("Подписываемся на подпапку системной папки. \n"
            + "При создании должны сразу подписаться на папку, поэтому отписываемся от нее")
    @ru.yandex.qatools.allure.annotations.TestCaseId("385")
    public void subscribeOnSubfolderSystemFolder() {
        FolderContainer folderContainer = newFolder(sysFolder, Util.getRandomString());
        prodImap.request(create(folderContainer.fullName()));
        prodImap.request(unsubscribe(folderContainer.fullName()));

        imap.list().shouldSeeFolder(folderContainer.fullName());
        imap.request(subscribe(folderContainer.fullName())).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(folderContainer.fullName());
    }

}
