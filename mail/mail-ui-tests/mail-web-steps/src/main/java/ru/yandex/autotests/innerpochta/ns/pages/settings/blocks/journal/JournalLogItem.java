package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.journal;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface JournalLogItem extends MailElement {

    @Name("Описание записи в журнале посещений")
    @FindByCss(".b-account-activity-log__value_protocol")
    MailElement journalLogItemProtocol();

    @Name("Дата записи в журнале посещений")
    @FindByCss(".b-account-activity-log__value_dayname")
    MailElement journalLogItemDayname();

    @Name("Время записи в журнале посещений")
    @FindByCss(".b-account-activity-log__value_cell")
    MailElement journalLogItemCell();
}
