package ru.yandex.autotests.innerpochta.barbet;

import org.junit.Rule;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.archive.discard.ApiArchiveDiscard;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.archive.restore.ApiArchiveRestore;
import ru.yandex.autotests.innerpochta.wmi.core.rules.barbet.RecoveryUserRule;

import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiBarbet;


public class ArchivationBaseTest extends BarbetBaseTest {
    @Rule
    public RecoveryUserRule recoveryUser = new RecoveryUserRule(authClient).before(true).after(true);

    ApiArchiveRestore restore() {
        return apiBarbet(getUserTicket()).archive().restore()
                .withUid(getUid());
    }

    ApiArchiveDiscard discard() {
        return apiBarbet(getUserTicket()).archive().discard()
                .withUid(getUid());
    }
}
