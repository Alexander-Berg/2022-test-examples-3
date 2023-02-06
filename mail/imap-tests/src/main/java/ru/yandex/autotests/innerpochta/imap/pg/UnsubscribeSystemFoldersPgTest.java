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
@Title("Команда UNSUBSCRIBE. Отписываемся от системной папки")
@Features({ImapCmd.SUBSCRIBE, "PG"})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Отписываемся от папок и подпапок системных папок\n"
        + "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class UnsubscribeSystemFoldersPgTest extends BaseTest {
    private static Class<?> currentClass = UnsubscribeSystemFoldersPgTest.class;

    @ClassRule
    public static final ImapClient imap = newLoginedClient(currentClass);
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
    @Title("Сначала подписываемся на папки, затем отписываемся")
    @ru.yandex.qatools.allure.annotations.TestCaseId("392")
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
    @ru.yandex.qatools.allure.annotations.TestCaseId("393")
    public void doubleUnsubscribeOnSystemFolderShouldSeeOk() {
        prodImap.request(subscribe(sysFolder));
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);

        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);
    }

    @Test
    @Title("Отписываемся от системной папки, подписываемся, потом опять отписываемся\n")
    @ru.yandex.qatools.allure.annotations.TestCaseId("394")
    public void unsubscribeSystemFolderAfterSubscribe() {
        prodImap.request(subscribe(sysFolder));
        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);

        prodImap.request(subscribe(sysFolder));
        imap.lsub().shouldSeeSubscribedFolder(sysFolder);

        imap.request(unsubscribe(sysFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(sysFolder);
    }

    @Test
    @Title("Подписываемся на подпапку системной папки\n"
            + "При создании должны сразу подписаться на папку, поэтому отписываемся от нее")
    @ru.yandex.qatools.allure.annotations.TestCaseId("395")
    public void unsubscribeOnSubfolderSystemFolder() {
        FolderContainer folderContainer = newFolder(sysFolder, Util.getRandomString());
        prodImap.request(create(folderContainer.fullName()));
        imap.list().shouldSeeFolder(folderContainer.fullName());
        imap.lsub().shouldSeeSubscribedFolder(folderContainer.fullName());

        imap.request(unsubscribe(folderContainer.fullName())).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(folderContainer.fullName());
    }
}
