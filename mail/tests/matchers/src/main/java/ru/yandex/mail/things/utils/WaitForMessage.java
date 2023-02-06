package ru.yandex.mail.things.utils;

import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.hound.Folders;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.generated.FolderSymbol;
import ru.yandex.mail.things.matchers.IsThereMessagesMatcher;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.things.matchers.IsThereMessagesMatcher.hasMsgsIn;
import static ru.yandex.mail.things.matchers.IsThereMessagesMatcher.hasMsgsStrictSubjectIn;
import static ru.yandex.mail.things.matchers.WithWaitFor.withWaitFor;

public class WaitForMessage {
    private static String NO_SUBJECT_TITLE = "No subject";

    private UserCredentials authClient;
    private String subj;
    private boolean strictSubjectChecking = false;
    private String errorMsg;
    private long timeout;
    private int count;
    private String fid;

    private void reset() {
        this.subj = NO_SUBJECT_TITLE;
        this.errorMsg = "";
        this.timeout = SECONDS.toMillis(60);
        this.count = 1;
        this.fid = null;
    }

    private WaitForMessage(WaitForMessage other, UserCredentials authClient) {
        this.authClient = authClient;
        this.subj = other.subj;
        this.errorMsg = other.errorMsg;
        this.timeout = other.timeout;
        this.count = other.count;
        this.fid = other.fid;
    }

    private String fid(FolderSymbol sybmol) {
        return Folders.folders(
                HoundApi.apiHound(
                        HoundProperties.properties()
                                .houndUri(),
                        props().getCurrentRequestId()
                )
                        .folders()
                        .withUid(authClient.account().uid())
                        .post(shouldBe(HoundResponses.ok200()))
        ).fid(sybmol);
    }

    public WaitForMessage(UserCredentials authClient) {
        this.authClient = authClient;
        reset();
    }

    public WaitForMessage subj(String subj) {
        this.subj = subj;
        return this;
    }

    public WaitForMessage strict() {
        strictSubjectChecking = true;
        return this;
    }

    public WaitForMessage inbox() {
        this.fid = fid(FolderSymbol.INBOX);
        return this;
    }

    public WaitForMessage sent() {
        this.fid = fid(FolderSymbol.SENT);
        return this;
    }

    public WaitForMessage spam() {
        this.fid = fid(FolderSymbol.SPAM);
        return this;
    }

    public WaitForMessage draft() {
        this.fid = fid(FolderSymbol.DRAFT);
        return this;
    }

    public WaitForMessage fid(String fid) {
        this.fid = fid;
        return this;
    }

    public WaitForMessage template() {
        this.fid = fid(FolderSymbol.TEMPLATE);
        return this;
    }

    public WaitForMessage usingHttpClient(UserCredentials client) {
        return new WaitForMessage(this, client);
    }

    public WaitForMessage errorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    public WaitForMessage timeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public WaitForMessage count(int count) {
        this.count = count;
        return this;
    }

    public WaitedMailbox waitDeliver() {
        if (fid == null) {
            inbox();
        }

        if (null == subj) {
            waitMessagesDeliver(count, fid, timeout, errorMsg);
        } else {
            waitMessagesDeliver(subj, count, fid, timeout, errorMsg);
        }

        WaitedMailbox mailbox = new WaitedMailbox(subj, fid, authClient);
        reset();

        return mailbox;
    }

    @Step("[WAIT]: Ждем письма ({1}) с темой \"{0}\" в папке \"{3}\" <{2}>")
    private void waitMessagesDeliver(String subj, Integer count, String fid,
                                     long timeout, String failMsg) throws AssertionError {
        String msg = isEmpty(failMsg)
                ? "Не все письма с темой '" + subj + "' были найдены в папке " + fid + "  ."
                : failMsg;

        IsThereMessagesMatcher matcher = strictSubjectChecking
                ? hasMsgsStrictSubjectIn(subj, count, fid)
                : hasMsgsIn(subj, count, fid);

        assertThat(msg,
                authClient,
                withWaitFor(matcher, timeout));
    }

    @Step("[WAIT]: Ждем письма ({0}) в папке \"{2}\" <{3}>")
    private void waitMessagesDeliver(Integer count, String fid,
                                     long timeout, String failMsg) {
            String msg = isEmpty(failMsg)
                    ? "Количество писем в папке '" + fid + "' не совпадает с ожидаемым."
                    : failMsg;

            assertThat(msg,
                    authClient,
                    withWaitFor(hasMsgsIn(count, fid), timeout));
    }
}
