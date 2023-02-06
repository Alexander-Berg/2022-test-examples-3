package ru.yandex.autotests.innerpochta.imap.rules;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.steps.ClearSteps;
import ru.yandex.autotests.innerpochta.imap.steps.NoopSteps;

/**
 * User: lanwen
 * Date: 25.03.14
 * Time: 15:31
 */
public class CleanRule extends BeforeAfterOptionalRule {
    private final Logger log = LogManager.getLogger(this.getClass());
    private ClearSteps clearSteps;
    private NoopSteps noop;

    private CleanRule(ImapClient imap) {
        this.clearSteps = new ClearSteps(imap);
        this.noop = imap.noop();
    }

    public static ImapClient withCleanBefore(ImapClient imap) {
        return imap.around(new CleanRule(imap).before(true));
    }

    public static ImapClient withCleanAfter(ImapClient imap) {
        return imap.around(new CleanRule(imap).after(true));
    }

    @Override
    public void call() {
        log.warn("vvvvvvvvvvvv-->CLEAN ALL (start)");
        clearSteps.clearMailbox();
        noop.pullChanges();
        log.warn("^^^^^^^^^^^^-->CLEAN ALL (end)");
    }
}
