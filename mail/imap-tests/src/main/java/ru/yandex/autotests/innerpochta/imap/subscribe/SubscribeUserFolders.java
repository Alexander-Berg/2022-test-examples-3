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
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.HAS_CHILDREN;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.NO_SELECT;
import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.matchers.ListItemMatcher.listItem;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.LsubRequest.lsub;
import static ru.yandex.autotests.innerpochta.imap.requests.SubscribeRequest.subscribe;
import static ru.yandex.autotests.innerpochta.imap.requests.UnsubscribeRequest.unsubscribe;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.rules.UnsubscribeRule.withUnsubscribeBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 13.05.14
 * Time: 12:18
 */
@Aqua.Test
@Title("Команда SUBSCRIBE. Подписываемся на пользовательсую папку")
@Features({ImapCmd.SUBSCRIBE})
@Stories(MyStories.USER_FOLDERS)
@Description("Подписываемся на пользовательские папки, подпапки\n"
        + "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class SubscribeUserFolders extends BaseTest {
    private static Class<?> currentClass = SubscribeUserFolders.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));
    private String userFolder;

    public SubscribeUserFolders(String userFolder) {
        this.userFolder = userFolder;
    }

    @Parameterized.Parameters(name = "user_folder - {0}")
    public static Collection<Object[]> foldersForSubscribe() {
        return allKindsOfFolders();
    }

    @Description("Просто подписываемся на созданную папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("640")
    public void simpleSubscribeShouldSeeOk() {
        prodImap.request(create(userFolder));
        prodImap.request(unsubscribe(userFolder));
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);

        imap.request(subscribe(userFolder)).shouldBeOk();
        //todo: добавить проверку флагов
        imap.lsub().shouldSeeSubscribedFolder(userFolder);
    }

    @Description("Дважды подписываемся на созданную папку\n"
            + "Ожидаемый результат: OK, так как фича")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("641")
    public void doubleSubscribeShouldSeeOk() {
        prodImap.request(create(userFolder));
        prodImap.request(unsubscribe(userFolder));
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);

        imap.request(subscribe(userFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(userFolder);
        imap.request(subscribe(userFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(userFolder);
    }


    @Description("Подписываемся на подпапку [MAILPROTO-2083]\n"
            + "При создании должны сразу подписаться на папку, поэтому отписываемся от нее\n"
            + "У родительской папки должны увидеть /noselect")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("642")
    public void subscribeOnSubfolderUserFolder() {
        FolderContainer folderContainer = newFolder(userFolder, Util.getRandomString());
        prodImap.request(create(folderContainer.fullName()));
        prodImap.request(unsubscribe(folderContainer.parent()));
        prodImap.request(unsubscribe(folderContainer.fullName()));

        imap.lsub().shouldNotSeeSubscribedFolders(folderContainer.foldersTreeAsList());

        imap.request(subscribe(folderContainer.fullName())).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(folderContainer.fullName());

        imap.request(lsub("\"\"", folderContainer.parent())).shouldBeOk()
                .withItems(listItem("|", folderContainer.parent(), HAS_CHILDREN.value(), NO_SELECT.value()));
    }

    @Description("Подписываемся на папку, у которой есть подпапка\n"
            + "При создании должны сразу подписаться на папку, поэтому отписываемся от нее")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("643")
    public void subscribeParentOfSubfolderUserFolder() {
        FolderContainer folderContainer = newFolder(userFolder, Util.getRandomString());
        prodImap.request(create(folderContainer.fullName()));
        prodImap.request(unsubscribe(folderContainer.parent()));
        prodImap.request(unsubscribe(folderContainer.fullName()));

        imap.lsub().shouldNotSeeSubscribedFolders(folderContainer.foldersTreeAsList());

        imap.request(subscribe(folderContainer.parent())).shouldBeOk();

        imap.lsub().shouldSeeSubscribedFolder(folderContainer.parent());
        imap.lsub().shouldNotSeeSubscribedFolder(folderContainer.fullName());
    }

    @Description("Подписываемся на папку, отписываемся, потом опять подписываемся\n")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("639")
    public void subscribeSystemFolderAfterUnsubscribe() {
        prodImap.request(create(userFolder));
        prodImap.request(unsubscribe(userFolder));
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);

        imap.request(subscribe(userFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(userFolder);

        prodImap.request(unsubscribe(userFolder));
        imap.lsub().shouldNotSeeSubscribedFolder(userFolder);

        imap.request(subscribe(userFolder)).shouldBeOk();
        imap.lsub().shouldSeeSubscribedFolder(userFolder);
    }
}
