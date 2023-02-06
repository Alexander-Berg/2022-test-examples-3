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
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateName;

@Aqua.Test
@Title("Запрос DELETE")
@Features({"RFC"})
@Stories("6.3.4 DELETE")
@Description("http://tools.ietf.org/html/rfc3501#section-6.3.4")
public class DeleteTest extends BaseTest {
    private static Class<?> currentClass = DeleteTest.class;

    @Rule
    public ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("434")
    public void simpleDelete() {
        String folderName = generateName();
        imap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);
        imap.request(delete(folderName)).shouldBeOk();
        imap.request(select(folderName)).repeatUntilNo(imap).shouldBeNo();
        imap.request(examine(folderName)).repeatUntilNo(imap).shouldBeNo();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("435")
    public void deleteInbox() {
        imap.request(delete("inbox")).shouldBeNo();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("436")
    public void deleteNotExisting() {
        imap.request(delete(generateName())).shouldBeNo();
    }
}
