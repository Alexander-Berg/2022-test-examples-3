package ru.yandex.autotests.innerpochta.imap.select;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.sun.mail.imap.protocol.BASE64MailboxEncoder.encode;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.cyrillic;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.03.14
 * Time: 19:35
 */
@Aqua.Test
@Title("Команда SELECT. Выбор папки внутри второй сессии. Выбор уже удаленной папки")
@Features({ImapCmd.SELECT})
@Stories({MyStories.TWO_SESSION})
@Description("Работа с папками в двух сессиях параллельно")
public class SelectWithTwoSessionsTest extends BaseTest {
    private static Class<?> currentClass = SelectWithTwoSessionsTest.class;


    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient imap2 = newLoginedClient(currentClass);

    @Test
    @Stories({"Кириллические папки"})
    @Title("Селект для кириллической папки, созданной в другой сессии")
    @Description("[MAILPROTO-2121] Создаем кириллическую подпапку в одной сессии, селектим ее в другой, ожидая OK")
    @ru.yandex.qatools.allure.annotations.TestCaseId("584")
    public void shouldSelectCyrillicSubfolderInOtherSession() {
        String folder = newFolder(encode(cyrillic()), encode(cyrillic())).fullName();

        imap.request(create(folder)).shouldBeOk();
        imap2.request(select(folder)).repeatUntilOk(imap2).shouldBeOk();
        imap2.request(examine(folder)).shouldBeOk();
    }

    @Test
    @Stories({MyStories.ASYNC_OPER})
    @Title("Одновременно выполняем удаление и выбор папки\n" +
            "Ожидаемый результат: удаленная папка не заселектилась")
    @Description("Создаем кириллическую подпапку в одной сессии, удаляем, селектим ее в другой, ожидая No")
    @ru.yandex.qatools.allure.annotations.TestCaseId("585")
    public void shouldNotSelectAlreadyDeletedCyrillicSubfolderInOtherSession() {
        String folder = newFolder(encode(cyrillic()), encode(cyrillic())).fullName();

        imap.request(create(folder)).shouldBeOk();
        imap.request(select(folder)).repeatUntilOk(imap2);

        imap.request(delete(folder));
        imap2.request(select(folder)).shouldBeNo();
    }

}
