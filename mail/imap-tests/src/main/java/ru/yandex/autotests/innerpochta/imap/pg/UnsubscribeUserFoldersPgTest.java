package ru.yandex.autotests.innerpochta.imap.pg;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.matchers.ListItemMatcher.listItem;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.LsubRequest.lsub;
import static ru.yandex.autotests.innerpochta.imap.requests.SubscribeRequest.subscribe;
import static ru.yandex.autotests.innerpochta.imap.requests.UnsubscribeRequest.unsubscribe;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.rules.UnsubscribeRule.withUnsubscribeBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

@Aqua.Test
@Title("Команда UNSUBSCRIBE. Пользовательские папки")
@Features({ImapCmd.SUBSCRIBE, "PG"})
@Stories(MyStories.USER_FOLDERS)
@Description("Отписываемся от пользовательских папок")
public class UnsubscribeUserFoldersPgTest extends BaseTest {
    private static Class<?> currentClass = UnsubscribeUserFoldersPgTest.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient imapClean = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));
    private String userFolder;

    @Before
    public void setUp() {
        userFolder = getRandomString();
    }

    @Test
    @Description("Просто отписываемся от созданной папки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("396")
    public void simpleUnsubscribeShouldSeeOk() {
        imap.request(create(userFolder));
        imap.lsub().shouldSeeSubscribedFolder(userFolder);

        imap.request(unsubscribe(userFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);

        //todo: добавить проверку флагов
    }

    @Test
    @Description("Дважды отписываемся от созданной папки\n"
            + "Ожидаемый результат: OK, так как фича")
    @ru.yandex.qatools.allure.annotations.TestCaseId("397")
    public void doubleUnsubscribeShouldSeeOk() {
        imap.request(create(userFolder));
        imap.lsub().shouldSeeSubscribedFolder(userFolder);

        imap.request(unsubscribe(userFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);

        imap.request(unsubscribe(userFolder)).shouldBeOk();
        //todo: добавить проверку флагов
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);
    }


    @Test
    @Stories(MyStories.JIRA)
    @Description("Отписываемся от подпапки [MAILPROTO-2083]\n"
            + "При создании должны сразу подписаться на папку, поэтому отписываемся от нее")
    @ru.yandex.qatools.allure.annotations.TestCaseId("398")
    public void unsubscribeOnSubfolderUserFolder() {
        FolderContainer folderContainer = newFolder(userFolder, getRandomString());
        imap.request(create(folderContainer.fullName()));
        imap.lsub().shouldSeeSubscribedFolders(folderContainer.foldersTreeAsList());

        imap.request(unsubscribe(folderContainer.fullName())).shouldBeOk();

        imap.lsub().shouldNotSeeSubscribedFolder(folderContainer.fullName());
        imap.lsub().shouldSeeSubscribedFolder(folderContainer.parent());
    }

    @Test
    @Stories(MyStories.JIRA)
    @Description("[MAILPROTO-2090] Подписываемся на папку, у которой есть подпапка\n"
            + "При создании должны сразу подписаться на папку, поэтому отписываемся от нее")
    @ru.yandex.qatools.allure.annotations.TestCaseId("399")
    public void unsubscribeParentOfSubfolderUserFolder() {
        FolderContainer folderContainer = newFolder(userFolder, getRandomString());
        imap.request(create(folderContainer.fullName()));
        imap.lsub().shouldSeeSubscribedFolders(folderContainer.foldersTreeAsList());

        imap.request(unsubscribe(folderContainer.parent())).shouldBeOk();

        imap.lsub().shouldSeeSubscribedFolder(folderContainer.fullName());

        imap.request(lsub("\"\"", folderContainer.parent())).shouldBeOk()
                .withItems(listItem("|", folderContainer.parent(), FolderFlags.HAS_CHILDREN.value(),
                        FolderFlags.NO_SELECT.value()));
    }

    @Test
    @Description("Отписываемся от папки, подписываемся, потом опять отписываемся\n")
    @ru.yandex.qatools.allure.annotations.TestCaseId("400")
    public void unsubscribeSystemFolderAfterUnsubscribe() {
        imap.request(create(userFolder));
        imap.lsub().shouldSeeSubscribedFolder(userFolder);

        imap.request(unsubscribe(userFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);

        imap.request(subscribe(userFolder));
        imap.lsub().shouldSeeSubscribedFolder(userFolder);

        imap.request(unsubscribe(userFolder)).shouldBeOk();
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);
    }
}
