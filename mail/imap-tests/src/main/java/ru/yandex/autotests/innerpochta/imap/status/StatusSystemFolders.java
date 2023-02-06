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

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.StatusRequest.status;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 18.04.14
 * Time: 15:08
 */
@Aqua.Test
@Title("Команда STATUS. Состояние системной папки")
@Features({ImapCmd.STATUS})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Проверяем, что прешедшие/удаленные письма в системных папках корректно отображаются")
@RunWith(Parameterized.class)
public class StatusSystemFolders extends BaseTest {
    private static Class<?> currentClass = StatusSystemFolders.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;

    public StatusSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "system folder - {0}")
    public static Collection<Object[]> systemFolders() {
        return allSystemFolders();
    }

    @Description("Выполняем STATUS с пустой папкой")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("597")
    public void statusEmptyUserFolderShouldSeeOk() {
        imap.request(status(sysFolder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(0)
                .numberOfRecentMessagesShouldBe(0)
                .numberOfUnseenMessagesShouldBe(0);

    }

    @Description("Дважды выполняем STATUS с пустой папкой")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("598")
    public void doubleStatusUserFolderShouldSeeOk() {
        imap.request(status(sysFolder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(0)
                .numberOfRecentMessagesShouldBe(0)
                .numberOfUnseenMessagesShouldBe(0);
        imap.request(status(sysFolder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(0)
                .numberOfRecentMessagesShouldBe(0)
                .numberOfUnseenMessagesShouldBe(0);
    }

    @Description("[MAILPROTO-2117] Отправляем себе пару писем, проверяем, что изменился recent")
    @Stories({MyStories.JIRA, "RECENT"})
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("595")
    public void statusWithRecentMessages() throws Exception {
        prodImap.append().appendRandomMessage(sysFolder);

        imap.list().shouldSeeFolder(sysFolder);
        imap.request(status(sysFolder).messages().recent().unseen()).shouldBeOk()
                .numberOfMessagesShouldBe(1)
                .numberOfRecentMessagesShouldBe(1)
                .numberOfUnseenMessagesShouldBe(1);
    }

    @Description("Проверяем, что UID validity такой же как на продакшене")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("599")
    public void shouldUidValiditySameAsProd() {
        imap.request(status(sysFolder).uidNext().uidValidity()).shouldBeOk()
                .uidValidityShouldBe(prodImap.status().getUidValidity(sysFolder));
    }

    @Description("Проверяем, что UID next такой же как на продакшене")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("596")
    public void shouldUidNextSameAsProd() {
        imap.request(status(sysFolder).uidNext().uidValidity()).shouldBeOk()
                .uidNextShouldBe(prodImap.status().getUidNext(sysFolder));
    }
}
