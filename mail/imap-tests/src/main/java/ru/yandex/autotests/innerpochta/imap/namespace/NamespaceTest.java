package ru.yandex.autotests.innerpochta.imap.namespace;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Corp;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.NamespaceRequest.namespace;
import static ru.yandex.autotests.innerpochta.imap.responses.NamespaceResponse.NIL;
import static ru.yandex.autotests.innerpochta.imap.structures.NamespaceContainer.emptyNamespace;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.04.14
 * Time: 16:09
 * https://jira.yandex-team.ru/browse/MAILPROTO-2103
 */
@Aqua.Test
@Title("Команда NAMESPACE. Общие тесты")
@Features({ImapCmd.NAMESPACE})
@Stories(MyStories.COMMON)
@Description("Общие тесты на NAMESPACE. Проверяем выдачу")
@Corp
public class NamespaceTest extends BaseTest {
    private static Class<?> currentClass = NamespaceTest.class;


    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("317")
    public void namespaceShouldSeeBadBeforeLogin() {
        imap.request(namespace()).shouldBeBad();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("316")
    public void namespaceAfterLoginTest() {
        imap.request(login(currentClass.getSimpleName()));
        imap.request(namespace()).shouldBeOk()
                .rootNamespaceShouldBe(emptyNamespace().getNamespace()).sharedNamespaceShouldBe(NIL)
                .userNamespaceShouldBe(NIL);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("315")
    public void doubleNamespace() {
        imap.request(login(currentClass.getSimpleName()));
        imap.request(namespace()).shouldBeOk()
                .rootNamespaceShouldBe(emptyNamespace().getNamespace()).sharedNamespaceShouldBe(NIL)
                .userNamespaceShouldBe(NIL);
        imap.request(namespace()).shouldBeOk()
                .rootNamespaceShouldBe(emptyNamespace().getNamespace()).sharedNamespaceShouldBe(NIL)
                .userNamespaceShouldBe(NIL);
    }
}
