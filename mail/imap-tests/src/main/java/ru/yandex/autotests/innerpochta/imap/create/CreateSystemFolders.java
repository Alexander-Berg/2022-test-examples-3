package ru.yandex.autotests.innerpochta.imap.create;

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
import ru.yandex.autotests.innerpochta.imap.rules.CleanRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ch.lambdaj.collection.LambdaCollections.with;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.converters.ToObjectConverter.wrap;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.03.14
 * Time: 20:50
 * <p/>
 * MAILPROTO-2133
 * MAILPROTO-2138
 */
@Aqua.Test
@Title("Команда CREATE. Создаем папки, подпапки в системных папках")
@Features({ImapCmd.CREATE})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Создаем папки, подпапки в системных папках")
@RunWith(Parameterized.class)
public class CreateSystemFolders extends BaseTest {
    private static Class<?> currentClass = CreateSystemFolders.class;

    private static final int LEVEL_OF_HIERARCHY = 5;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = CleanRule.withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;

    public CreateSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return with(
                systemFolders().getSent(),
                systemFolders().getDeleted(),
                systemFolders().getDrafts(),
                systemFolders().getOutgoing(),
                systemFolders().getSpam()
        ).convert(wrap());
    }

    @Description("Создаем пользовательские папки с именами системных папок\n"
            + "Ожидаемый результат: NO")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("134")
    public void createSystemFolderShouldSeeNo() {
        imap.request(create(sysFolder)).shouldBeNo();
        imap.list().shouldSeeOnlySystemFoldersWithFlags();
    }
}
