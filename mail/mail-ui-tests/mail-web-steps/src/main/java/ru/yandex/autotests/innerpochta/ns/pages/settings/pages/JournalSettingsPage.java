package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.journal.JournalBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface JournalSettingsPage extends MailPage {

    @Name("Журнал учёта посещений")
    @FindByCss(".b-account-activity")
    JournalBlock journalBlock();
}
