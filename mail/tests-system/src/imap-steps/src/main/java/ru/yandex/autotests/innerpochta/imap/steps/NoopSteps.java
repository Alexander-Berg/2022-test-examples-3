package ru.yandex.autotests.innerpochta.imap.steps;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.matchers.SomethingHappensMatcher.somethingHappens;
import static ru.yandex.autotests.innerpochta.imap.matchers.WaitMatcher.withWaitFor;
import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 20.05.14
 * Time: 15:40
 */
public class NoopSteps {

    private static final long TIMEOUT = 10;

    private final ImapClient client;

    private NoopSteps(ImapClient imap) {
        this.client = imap;
    }

    public static NoopSteps with(ImapClient imapClient) {
        return new NoopSteps(imapClient);
    }

    @Step
    public void messageShouldBeReceived() {
        assertThat(client, withWaitFor(somethingHappens(), TIMEOUT, SECONDS));
    }

    @Step("Обновить состояние")
    public void pullChanges() {
        client.request(noOp()).shouldBeOk();
    }
}
