package ru.yandex.autotests.innerpochta.imap.status;

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

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.StatusRequest.status;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:17
 */
@Aqua.Test
@Title("Команда STATUS. Состояние папки")
@Features({ImapCmd.STATUS})
@Stories(MyStories.USER_FOLDERS)
@Description("Проверяем, что прешедшие/удаленные письма корректно отображаются")
@RunWith(Parameterized.class)
public class StatusUserFolders extends BaseTest {
    private static Class<?> currentClass = StatusUserFolders.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String folder;

    public StatusUserFolders(String folder) {
        this.folder = folder;
    }

    @Parameterized.Parameters(name = "folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Description("Выполняем статус без параметров")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("602")
    public void statusWihoutParamsShouldSeeBad() {
        prodImap.request(create(folder));
        imap.list().shouldSeeFolder(folder);
        imap.request(status(folder)).shouldBeBad();
    }

    @Description("Выполняем STATUS с несуществующей папкой")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("603")
    public void statusNonExistFolderShouldSeeNo() {
        imap.request(status(folder).messages().recent().unseen()).shouldBeNo();
    }

    @Description("Выполняем STATUS с пустой папкой")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("604")
    public void statusEmptyUserFolderShouldSeeYes() {
        prodImap.request(create(folder));
        imap.list().shouldSeeFolder(folder);
        imap.request(status(folder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(0)
                .numberOfRecentMessagesShouldBe(0)
                .numberOfUnseenMessagesShouldBe(0);

    }

    @Description("Дважды выполняем STATUS с пустой папкой")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("605")
    public void doubleStatusUserFolderShouldSeeYes() {
        prodImap.request(create(folder));
        imap.list().shouldSeeFolder(folder);
        imap.request(status(folder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(0)
                .numberOfRecentMessagesShouldBe(0)
                .numberOfUnseenMessagesShouldBe(0);
        imap.request(status(folder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(0)
                .numberOfRecentMessagesShouldBe(0)
                .numberOfUnseenMessagesShouldBe(0);
    }

    @Description("Отправляем себе пару писем, проверяем, что изменился recent")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("600")
    public void statusWithRecentMessages() throws Exception {
        prodImap.request(create(folder));
        prodImap.list().shouldSeeFolder(folder);

        prodImap.append().appendRandomMessage(folder);

        imap.list().shouldSeeFolder(folder);
        imap.request(status(folder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(1)
                .numberOfRecentMessagesShouldBe(1)
                .numberOfUnseenMessagesShouldBe(1);
    }

    @Description("Отправляем себе пару писем, проверяем, что изменился unseen")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("601")
    public void statusWithUnseenMessages() throws Exception {
        prodImap.request(create(folder));
        prodImap.list().shouldSeeFolder(folder);

        prodImap.append().appendRandomMessage(folder);

        imap.list().shouldSeeFolder(folder);

        //почечаем письмо прочитанным
        imap.request(status(folder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(1)
                .numberOfRecentMessagesShouldBe(1)
                .numberOfUnseenMessagesShouldBe(1);
    }

    @Description("Проверяем, что UID next такой же как на продакшене")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("608")
    public void statusWithUidNext() {
        prodImap.request(create(folder));
        imap.list().shouldSeeFolder(folder);
        imap.request(status(folder).uidNext().uidValidity()).shouldBeOk().
                uidNextShouldBe(prodImap.status().getUidNext(folder));
    }
}
