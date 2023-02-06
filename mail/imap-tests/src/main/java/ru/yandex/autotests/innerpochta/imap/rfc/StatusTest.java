package ru.yandex.autotests.innerpochta.imap.rfc;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.StatusRequest.status;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

@Aqua.Test
@Title("Запрос STATUS")
@Features({"RFC"})
@Stories("6.3.10 STATUS")
@Description("http://tools.ietf.org/html/rfc3501#section-6.3.10")
public class StatusTest extends BaseTest {
    private static Class<?> currentClass = StatusTest.class;


    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(imap);

    @Test
    @Title("Статус должен вернуть точное количество сообщений")
    @ru.yandex.qatools.allure.annotations.TestCaseId("455")
    public void testStatus() {
        imap.request(status(Folders.INBOX).messages().recent().unseen())
                .shouldBeOk()
                .numberOfMessagesShouldBe(0)
                .numberOfRecentMessagesShouldBe(0)
                .numberOfUnseenMessagesShouldBe(0);
    }
}
