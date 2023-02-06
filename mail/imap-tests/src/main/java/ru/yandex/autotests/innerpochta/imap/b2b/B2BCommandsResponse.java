package ru.yandex.autotests.innerpochta.imap.b2b;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.ImapRequest;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ch.lambdaj.collection.LambdaCollections.with;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.converters.ToObjectConverter.wrap;
import static ru.yandex.autotests.innerpochta.imap.requests.CapabilityRequest.capability;
import static ru.yandex.autotests.innerpochta.imap.requests.CheckRequest.check;
import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;
import static ru.yandex.autotests.innerpochta.imap.requests.LsubRequest.lsub;
import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;
import static ru.yandex.autotests.innerpochta.imap.requests.StatusRequest.status;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 15.04.14
 * Time: 20:29
 */
@Aqua.Test
@Title("B2B все комманды. Общий тест")
@Features({"B2B"})
@Stories(MyStories.B2B)
@Description("[MAILPROTO-2158] Сравнение с продакшеном ответа от комманд")
@Issues({@Issue("MAILPROTO-2158"), @Issue("MPROTO-1216")})
@RunWith(Parameterized.class)
public class B2BCommandsResponse extends BaseTest {
    private static Class<?> currentClass = B2BCommandsResponse.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @ClassRule
    public static ImapClient prodImap = newLoginedClient(currentClass);
    private ImapRequest command;

    public B2BCommandsResponse(ImapRequest command) {
        this.command = command;
    }

    @Parameterized.Parameters(name = "cmd - {0}")
    @SuppressWarnings("unchecked")
    public static Collection<Object[]> commandsForCompare() {
        return with(
                //status:
                status(systemFolders().getInbox()).uidValidity().build("."),
                status(systemFolders().getInbox()).uidValidity().uidNext().build("."),
                status(systemFolders().getInbox()).uidValidity().uidNext().messages().build("."),
                status(systemFolders().getInbox()).uidValidity().uidNext().messages().recent().build("."),
                status(systemFolders().getInbox()).uidValidity().uidNext().messages().recent().unseen().build("."),

                //list:
                list("\"\"", systemFolders(Folders.RU).getInbox()).build("."),
                list("\"\"", systemFolders(Folders.EN).getInbox()).build("."),
                list("\"\"", "*").build("."),
                list("\"\"", "%").build("."),
                list("\"\"", "\"\"").build("."),
                //lsub:
                lsub("\"\"", systemFolders(Folders.RU).getInbox()).build("."),
                lsub("\"\"", systemFolders(Folders.EN).getInbox()).build("."),
                lsub("\"\"", "*").build("."),
                lsub("\"\"", "%").build("."),
                lsub("\"\"", "\"\"").build("."),
                //capability:
                capability().build("."),
                //noop:
                noOp().build("."),
                //check:
                check().build(".")
        ).convert(wrap());
    }

    @Description("Запрашиваем только положительные запросы, сравниваем ответы с продом")
    @Test
    @SuppressWarnings("unchecked")
    @ru.yandex.qatools.allure.annotations.TestCaseId("54")
    public void commonB2BTest() {
        ImapResponse imapResponse = requestWith(imap, command, "BETA");
        ImapResponse imapProdResponse = requestWith(prodImap, command, "PRODUCTION");

        imapResponse.shouldBeEqualTo(imapProdResponse);
    }

    @Step("Выполняем команду на {2}-IMAP")
    private ImapResponse requestWith(ImapClient client, ImapRequest request, String comment) {
        return client.request(request).shouldBeOk();
    }

}
