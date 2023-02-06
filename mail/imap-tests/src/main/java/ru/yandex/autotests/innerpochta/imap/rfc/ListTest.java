package ru.yandex.autotests.innerpochta.imap.rfc;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.structures.ListItem;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.matchers.ListItemMatcher.listItem;
import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

@Aqua.Test
@Title("Запрос LIST")
@Features({"RFC"})
@Stories("6.3.8 LIST")
@Description("http://tools.ietf.org/html/rfc3501#section-6.3.8")
public class ListTest extends BaseTest {
    private static Class<?> currentClass = ListTest.class;

    private static final Matcher<ListItem> INBOX = listItem("|", "INBOX", FolderFlags.UNMARKED.value(), FolderFlags.NO_INFERIORS.value());
    private static final Matcher<ListItem> OUTBOX = listItem("|", systemFolders().getOutgoing(), FolderFlags.UNMARKED.value(), FolderFlags.HAS_NO_CHILDREN.value());
    private static final Matcher<ListItem> SENT = listItem("|", systemFolders().getSent(), FolderFlags.UNMARKED.value(), FolderFlags.HAS_NO_CHILDREN.value(), FolderFlags.SENT.value());
    private static final Matcher<ListItem> JUNK = listItem("|", systemFolders().getSpam(), FolderFlags.UNMARKED.value(), FolderFlags.HAS_NO_CHILDREN.value(), FolderFlags.JUNK.value());
    private static final Matcher<ListItem> TRASH = listItem("|", systemFolders().getDeleted(), FolderFlags.UNMARKED.value(), FolderFlags.HAS_NO_CHILDREN.value(), FolderFlags.TRASH.value());
    private static final Matcher<ListItem> DRAFTS = listItem("|", systemFolders().getDrafts(), FolderFlags.UNMARKED.value(), FolderFlags.HAS_NO_CHILDREN.value(), FolderFlags.DRAFTS.value());

    @Rule
    public ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("445")
    public void listAll() {
        imap.request(list("\"\"", "*")).shouldBeOk().withItems(
                INBOX,
                OUTBOX,
                SENT,
                JUNK,
                TRASH,
                DRAFTS
        );
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("442")
    public void listAllPercent() {
        imap.request(list("\"\"", "%")).shouldBeOk().withItems(
                INBOX,
                OUTBOX,
                SENT,
                JUNK,
                TRASH,
                DRAFTS
        );
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("443")
    public void listInbox() {
        imap.request(list("INBOX", "\"\"")).shouldBeOk().withItems(
                listItem("|", "", FolderFlags.NO_SELECT.value())
        );
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("441")
    public void listSent() {
        imap.request(list("\"\"", systemFolders().getSent())).shouldBeOk().withItem(SENT);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("444")
    public void hierarchyDelimiterTest() {
        imap.request(list("\"\"", "\"\"")).shouldBeOk().withItems(
                listItem("|", "", FolderFlags.NO_SELECT.value())
        );
    }
}
