package ru.yandex.autotests.innerpochta.imap.steps;

import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.autotests.innerpochta.imap.consts.flags.SystemFolderFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.structures.ListItem;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.matchers.FolderExistMatcher.hasFolder;
import static ru.yandex.autotests.innerpochta.imap.matchers.IsNotExtended.not;
import static ru.yandex.autotests.innerpochta.imap.matchers.WaitMatcher.withWaitFor;
import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 18.04.14
 * Time: 21:35
 */
public class ListSteps {

    private static final long TIMEOUT = 10;

    private final ImapClient client;

    private ListSteps(ImapClient imap) {
        this.client = imap;
    }

    public static ListSteps with(ImapClient imapClient) {
        return new ListSteps(imapClient);
    }

    @Step("В выводе LIST должны увидеть папку {0}")
    public void shouldSeeFolder(String folderName) {
        assertThat("Не обнаружили папку " + folderName, client, withWaitFor(hasFolder(folderName), TIMEOUT, SECONDS));
    }

    @Step("В выводе LIST НЕ должны увидеть папку {0}")
    public void shouldNotSeeFolder(String folderName) {
        assertThat("Не должны были обнаружить папку " + folderName,
                client, withWaitFor(not(hasFolder(folderName)), TIMEOUT, SECONDS));
    }

    @Step("В выводе LIST должны увидеть все папки из {0}")
    public void shouldSeeFolders(List<String> folders) {
        for (String folder : folders) {
            shouldSeeFolder(folder);
        }
    }

    @Step("Смотрим, что в list")
    public void show() {
        client.request(list("\"\"", "*")).shouldBeOk();
    }

    @Step("В выводе LIST НЕ должны увидеть ни одной папки из {0}")
    public void shouldNotSeeFolders(List<String> folders) {
        for (String folder : folders) {
            shouldNotSeeFolder(folder);
        }
    }

    @Step("В выводе LIST должны увидеть все системные папки")
    public void shouldSeeSystemFolders() {
        client.request(list("\"\"", "*")).shouldBeOk()
                .withItem(SystemFolderFlags.getINBOXItem())
                .withItem(SystemFolderFlags.getSentItem())
                .withItem(SystemFolderFlags.getTrashItem())
                .withItem(SystemFolderFlags.getSpamItem())
                .withItem(SystemFolderFlags.getDraftsItem())
                .withItem(SystemFolderFlags.getOutgoingItem());
    }

    @Step("В выводе LIST должны увидеть ТОЛЬКО системные папки (и больше никаких)")
    public void shouldSeeOnlySystemFoldersWithFlags() {
        client.request(list("\"\"", "*")).shouldBeOk()
                .withItems(SystemFolderFlags.getINBOXItem(),
                        SystemFolderFlags.getSentItem(),
                        SystemFolderFlags.getTrashItem(),
                        SystemFolderFlags.getSpamItem(),
                        SystemFolderFlags.getDraftsItem(),
                        SystemFolderFlags.getOutgoingItem());
    }

    @Step
    public void shouldSeeOnlySystemFoldersWithFlagsInWildcard() {
        client.request(list("\"\"", "%")).shouldBeOk()
                .withItems(SystemFolderFlags.getINBOXItem(), SystemFolderFlags.getSentItem(),
                        SystemFolderFlags.getTrashItem(), SystemFolderFlags.getSpamItem(),
                        SystemFolderFlags.getDraftsItem(), SystemFolderFlags.getOutgoingItem());
    }

    @Step
    public void shouldSeeInListWithFlags(Matcher<ListItem> list) {
        client.request(list("\"\"", "*")).shouldBeOk()
                .withItems(list, SystemFolderFlags.getINBOXItem(), SystemFolderFlags.getSentItem(),
                        SystemFolderFlags.getTrashItem(), SystemFolderFlags.getSpamItem(),
                        SystemFolderFlags.getDraftsItem(), SystemFolderFlags.getOutgoingItem());
    }

    @Step
    public void shouldSeeInListWithFlagsInWildcard(Matcher<ListItem> list) {
        client.request(list("\"\"", "%")).shouldBeOk()
                .withItems(list, SystemFolderFlags.getINBOXItem(), SystemFolderFlags.getSentItem(),
                        SystemFolderFlags.getTrashItem(), SystemFolderFlags.getSpamItem(),
                        SystemFolderFlags.getDraftsItem(), SystemFolderFlags.getOutgoingItem());
    }

    @Step
    public void shouldSeeInListWithFlags(Matcher<ListItem> list, Matcher<ListItem> list1) {
        client.request(list("\"\"", "*")).shouldBeOk()
                .withItems(list, list1, SystemFolderFlags.getINBOXItem(), SystemFolderFlags.getSentItem(),
                        SystemFolderFlags.getTrashItem(), SystemFolderFlags.getSpamItem(),
                        SystemFolderFlags.getDraftsItem(), SystemFolderFlags.getOutgoingItem());
    }

    @Step
    public void shouldSeeInListWithFlagsInWildcard(Matcher<ListItem> list, Matcher<ListItem> list1) {
        client.request(list("\"\"", "%")).shouldBeOk()
                .withItems(list, list1, SystemFolderFlags.getINBOXItem(), SystemFolderFlags.getSentItem(),
                        SystemFolderFlags.getTrashItem(), SystemFolderFlags.getSpamItem(),
                        SystemFolderFlags.getDraftsItem(), SystemFolderFlags.getOutgoingItem());
    }
}
