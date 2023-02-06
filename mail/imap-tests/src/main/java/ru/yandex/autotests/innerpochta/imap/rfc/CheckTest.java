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

import static ru.yandex.autotests.innerpochta.imap.requests.CheckRequest.check;

@Aqua.Test
@Title("Запрос CHECK")
@Description("http://tools.ietf.org/html/rfc3501#section-6.4.1")
@Features({"RFC"})
@Stories("6.4.1 CHECK")
public class CheckTest extends BaseTest {
    private static Class<?> currentClass = CheckTest.class;

    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Test
    @Title("Запрос CHECK должен вернуть OK")
    @ru.yandex.qatools.allure.annotations.TestCaseId("427")
    public void testCheckSucceeds() {
        imap.request(check()).shouldBeOk();
    }
}
