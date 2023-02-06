package ru.yandex.autotests.innerpochta.imap.noop;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.steps.SmtpSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 04.04.14
 * Time: 16:49
 */
@Aqua.Test
@Title("Команда NOOP. Общие тесты")
@Features({ImapCmd.NOOP})
@Stories(MyStories.COMMON)
@Description("Общие тесты на NOOP")
public class NoopCommonTest extends BaseTest {
    private static Class<?> currentClass = NoopCommonTest.class;


    private final SmtpSteps smtp = new SmtpSteps(currentClass.getSimpleName());

    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("320")
    public void noopShouldSeeOkBeforeLogin() throws Exception {
        imap.request(noOp()).shouldBeOk().shouldBeEmpty();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("321")
    public void noopShouldSeeOkAfterLogin() {
        imap.request(login(currentClass.getSimpleName()));
        imap.request(noOp()).shouldBeOk().shouldBeEmpty();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("319")
    public void doubleNoopTest() {
        imap.request(noOp()).shouldBeOk().shouldBeEmpty();
        imap.request(noOp()).shouldBeOk().shouldBeEmpty();
    }
}
