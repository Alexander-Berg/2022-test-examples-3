package ru.yandex.autotests.innerpochta.imap.rules;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.steps.ClearSteps;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 20.05.14
 * Time: 18:34
 */
public class CleanFlagsRule extends BeforeAfterOptionalRule {
    private final Logger log = LogManager.getLogger(this.getClass());
    private ClearSteps clearSteps;

    private CleanFlagsRule(ImapClient imap) {
        this.clearSteps = new ClearSteps(imap);
    }

    public static ImapClient withCleanFlagsBefore(ImapClient imap) {
        return imap.around(new CleanFlagsRule(imap).before(true));
    }

    public static ImapClient withCleanFlagsAfter(ImapClient imap) {
        return imap.around(new CleanFlagsRule(imap).after(true));
    }

    @Override
    public void call() {
        log.warn("----------->CLEAR FLAGS");
        clearSteps.clearFlags();
        log.warn("===========>CLEAR FLAGS");
    }
}
