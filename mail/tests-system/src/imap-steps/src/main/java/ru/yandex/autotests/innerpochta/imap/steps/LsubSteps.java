package ru.yandex.autotests.innerpochta.imap.steps;

import java.util.List;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.matchers.FolderSubscribedMatcher.hasSubscribedFolder;
import static ru.yandex.autotests.innerpochta.imap.matchers.IsNotExtended.not;
import static ru.yandex.autotests.innerpochta.imap.matchers.WaitMatcher.withWaitFor;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 18.04.14
 * Time: 21:35
 */
public class LsubSteps {

    private static final long TIMEOUT = 10;

    private final ImapClient client;

    private LsubSteps(ImapClient imap) {
        this.client = imap;
    }

    public static LsubSteps with(ImapClient imapClient) {
        return new LsubSteps(imapClient);
    }

    @Step
    public void shouldSeeSubscribedFolder(String folderName) {
        assertThat(client, withWaitFor(hasSubscribedFolder(folderName), TIMEOUT, SECONDS));
    }

    @Step
    public void shouldNotSeeSubscribedFolder(String folderName) {
        assertThat(client, withWaitFor(not(hasSubscribedFolder(folderName)), TIMEOUT, SECONDS));
    }

    @Step
    public void shouldSeeSubscribedFolders(List<String> folders) {
        for (String folder : folders) {
            assertThat(client, withWaitFor(hasSubscribedFolder(folder), TIMEOUT, SECONDS));
        }
    }

    @Step
    public void shouldNotSeeSubscribedFolders(List<String> folders) {
        for (String folder : folders) {
            assertThat(client, withWaitFor(not(hasSubscribedFolder(folder)), TIMEOUT, SECONDS));
        }
    }
}
