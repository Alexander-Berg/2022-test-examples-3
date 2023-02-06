package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface Settings extends MailElement {

    @Name("Таб доступ")
    @FindByCss(".qa-EditLayer-TabAccess")
    MailElement tabAccess();

    @Name("Таб экспорт")
    @FindByCss(".qa-EditLayer-TabExport")
    MailElement tabExport();

    @Name("Экспорт в ical")
    @FindByCss(".qa-EditLayerCommon-ExportLink_iCal")
    MailElement icalExport();

    @Name("Экспорт в html")
    @FindByCss(".qa-EditLayerCommon-ExportLink_HTML")
    MailElement htmlExport();

    @Name("Экспорт в calDAV")
    @FindByCss(".qa-EditLayerCommon-ExportLink_CalDAV")
    MailElement caldavExport();

    @Name("Инпут для выдачи прав доступа")
    @FindByCss(".qa-LayerParticipants .textinput__control")
    MailElement inputContact();

    @Name("Попап")
    @FindByCss(".popup")
    MailElement popup();

    @Name("Саджест контактов")
    @FindByCss(".qa-Suggest-Items")
    MailElement suggest();
}
