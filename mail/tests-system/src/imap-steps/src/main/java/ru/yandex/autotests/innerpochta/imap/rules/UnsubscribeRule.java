package ru.yandex.autotests.innerpochta.imap.rules;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.steps.ClearSteps;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.04.14
 * Time: 20:28
 */
public class UnsubscribeRule extends BeforeAfterOptionalRule {
    private final Logger log = LogManager.getLogger(this.getClass());
    private ClearSteps clearSteps;

    private UnsubscribeRule(ImapClient imap) {
        this.clearSteps = new ClearSteps(imap);
    }

    public static ImapClient withUnsubscribeBefore(ImapClient imap) {
        return imap.around(new UnsubscribeRule(imap).before(true));
    }

    public static ImapClient withUnsubscribeAfter(ImapClient imap) {
        return imap.around(new UnsubscribeRule(imap).after(true));
    }

    @Override
    public void call() {
        log.warn("vvvvvvvvvvvv-->UNSUBSCRIBE ALL");
        clearSteps.unsubscribeFromAllFolders();
        log.warn("^^^^^^^^^^^^-->UNSUBSCRIBE ALL (end)");
    }
}
