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
import static ru.yandex.autotests.innerpochta.imap.requests.RenameRequest.rename;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateName;

@Aqua.Test
@Title("Запрос RENAME")
@Features({"RFC"})
@Stories("6.3.5 RENAME")
@Description("http://tools.ietf.org/html/rfc3501#section-6.3.5")
public class RenameTest extends BaseTest {
    private static Class<?> currentClass = RenameTest.class;

    @Rule
    public ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Title("Должны уметь переименовать папку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("450")
    public void simpleRename() {
        String folderName1 = generateName();
        String folderName2 = generateName();
        imap.request(create(folderName1)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName1);
        imap.request(select(folderName1)).shouldBeOk();

        imap.request(rename(folderName1, folderName2)).shouldBeOk();
        imap.request(select(folderName1)).shouldBeNo();
        imap.request(select(folderName2)).shouldBeOk();

        imap.request(delete(folderName2)).shouldBeOk();
        imap.request(select(folderName2)).shouldBeNo();
    }

    @Test
    @Title("RENAME должна возвращать BAD если папка с новым именем уже существует [MAILPROTO-2180]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("451")
    public void renameToExisting() {
        String folderName1 = generateName();
        String folderName2 = generateName();
        imap.request(create(folderName1)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName1);
        imap.request(create(folderName2)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName2);
        imap.request(rename(folderName1, folderName2)).shouldBeBad();
    }

    @Test
    @Title("Должны вернуть NO на попытку переименовать несуществующую папку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("452")
    public void renameNotExisting() {
        String folderName1 = generateName();
        String folderName2 = generateName();
        imap.request(rename(folderName1, folderName2)).shouldBeNo();
    }
}
