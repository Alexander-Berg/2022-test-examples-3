package ru.yandex.autotests.innerpochta.imap.rfc;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateName;

@Aqua.Test
@Title("Запрос CREATE")
@Features({"RFC"})
@Stories("6.3.3 CREATE")
@Description("http://tools.ietf.org/html/rfc3501#section-6.3.3")
public class CreateTest extends BaseTest {
    private static Class<?> currentClass = CreateTest.class;

    @Rule
    public ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("432")
    public void simpleCreate() throws InterruptedException {
        String folderName = generateName();
        imap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);
        imap.request(select(folderName)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(examine(folderName)).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("433")
    public void createExisting() {
        String folderName = generateName();
        imap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);
        imap.request(create(folderName)).shouldBeNo();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("431")
    public void createHierarchy() {
        String name1 = generateName();
        String name2 = name1 + "|" + generateName();
        String name3 = name2 + "|" + generateName();

        imap.request(create(name3)).shouldBeOk();
        imap.list().shouldSeeSystemFolders();
    }
}
