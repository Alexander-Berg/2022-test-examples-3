package ru.yandex.autotests.innerpochta.imap.rfc;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CustomRequest.custom;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

@Aqua.Test
@Title("Неправильные запросы")
@Features({"RFC"})
@Stories("Негативные тесты")
public class BadRequestsTest extends BaseTest {
    private static Class<?> currentClass = BadRequestsTest.class;

    public static final String BAD_COMMAND = "someweirdcommand";
    public static final String BAD_COMMAND_WITH_PARAMS = "someweirdcommand with parameters";
    @Rule
    public ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Title("Невалидная команда должна вернуть BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("424")
    public void badCommand() {
        imap.request(custom(BAD_COMMAND)).shouldBeBad();
    }

    @Test
    @Title("Невалидная команда с параметрами должна вернуть BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("425")
    public void badCommandWithParameters() {
        imap.request(custom(BAD_COMMAND_WITH_PARAMS)).shouldBeBad();
    }
}
