package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface GeneralSettings extends MailElement {

    @Name("Первый день недели")
    @FindByCss(".qa-SettingsWeekStartDay")
    MailElement weekStarts();

    @Name("Начало дня")
    @FindByCss(".qa-SettingsDayStartHour")
    MailElement dayStarts();

    @Name("Часовой пояс")
    @FindByCss(".qa-SettingsTimezone")
    MailElement timezone();

    @Name("Неактивная кнопка «Сохранить»")
    @FindByCss(".qa-Settings-Save.control_disabled_yes")
    MailElement disabledSaveButton();

    @Name("Активная кнопка «Сохранить»")
    @FindByCss(".qa-Settings-Save:not(.control_disabled_yes)")
    MailElement enabledSaveButton();

    @Name("Кнопка «Закрыть»")
    @FindByCss(".qa-SidebarsHeader-Closer")
    MailElement closeButton();

}
