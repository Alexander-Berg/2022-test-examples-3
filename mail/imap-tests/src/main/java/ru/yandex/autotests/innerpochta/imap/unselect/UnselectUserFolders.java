package ru.yandex.autotests.innerpochta.imap.unselect;

import java.util.Collection;

import org.junit.Before;
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

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:18
 */
@Aqua.Test
@Title("Команда UNSELECT. Общие тесты")
@Features({ImapCmd.UNSELECT})
@Stories(MyStories.COMMON)
@Description("")
@RunWith(Parameterized.class)
public class UnselectUserFolders extends BaseTest {
    private static Class<?> currentClass = UnselectUserFolders.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String userFolder;


    public UnselectUserFolders(String userFolder) {
        this.userFolder = userFolder;
    }

    @Parameterized.Parameters(name = "user_folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Before
    public void prepareFolder() {
        prodImap.request(create(userFolder));
        imap.list().shouldSeeFolder(userFolder);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("648")
    public void simpleUnselectAfterSelectUserFolder() {
        imap.request(select(userFolder)).repeatUntilOk(imap);
        imap.request(unselect()).shouldBeOk();
        imap.request(unselect()).shouldBeBad();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("649")
    public void simpleUnselectAfterExamineUserFolder() {
        imap.request(examine(userFolder)).repeatUntilOk(imap);
        imap.request(unselect()).shouldBeOk();
        imap.request(unselect()).shouldBeBad();
    }

    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. Выполняем UNSELECT\n" +
            "Письмо должно остаться")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("650")
    public void unselectWithDeletedOneMessageAfterSelect() throws Exception {
        prodImap.append().appendRandomMessage(userFolder);
        prodImap.select().waitMsgs(userFolder, 1);

        imap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.noop().pullChanges();
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(unselect()).shouldBeOk();

        prodImap.status().numberOfMessagesShouldBe(userFolder, 1);
    }


}
