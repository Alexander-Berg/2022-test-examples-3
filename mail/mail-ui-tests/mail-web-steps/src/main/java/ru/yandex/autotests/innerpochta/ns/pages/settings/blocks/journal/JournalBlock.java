package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.journal;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface JournalBlock extends MailElement {

    @Name("Описание журнала")
    @FindByCss(".b-account-activity__description")
    MailElement journalDescription();

    @Name("Текущий IP-адрес")
    @FindByCss(".b-account-activity__current-ip")
    MailElement journalIp();

    @Name("Жарнал учёта посещений")
    @FindByCss(".b-account-activity-log")
    MailElement journalLog();

    @Name("Записи в журнале посещений")
    @FindByCss(".b-account-activity-log__line")
    ElementsCollection<JournalLogItem> journalLogItem();
}
