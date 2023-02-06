package ru.yandex.autotests.innerpochta.imap.check;

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

import static ru.yandex.autotests.innerpochta.imap.requests.CheckRequest.check;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.03.14
 * Time: 1:16
 */
@Aqua.Test
@Title("Команда CHECK. Общие тесты.")
@Features({ImapCmd.CHECK})
@Stories(MyStories.COMMON)
@Description("Проверяем выдачу до авторизации и после авторизации")
public class CheckCommonTest extends BaseTest {
    private static Class<?> currentClass = CheckCommonTest.class;


    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("59")
    public void testCheckWithoutConnectionShouldSeeOk() {
        imap.request(check()).shouldBeOk();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("58")
    public void testCheckWithConnectionShouldSeeOk() {
        imap.request(login(currentClass.getSimpleName()));
        imap.request(check()).shouldBeOk();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("60")
    public void doubleCheckTestShouldSeeOk() {
        imap.request(check()).shouldBeOk();
        imap.request(check()).shouldBeOk();
    }

}
