package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.config.BlockCollectorServerSetup;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupCollector extends MailElement {

    @Name("Чекбокс «присваивать собранным письмам метку»")
    @FindByCss("[name = 'cliker_label']")
    MailElement putLabel();

    @Name("Выпадушка - выбор метки")
    @FindByCss(".js-move-label")
    MailElement selectLabelDropdown();

    @Name("Выпадушка - выбор папки")
    @FindByCss(".js-move-folder")
    MailElement selectFolderDropDown();

    @Name("Кнопка «Сохранить изменения»")
    @FindByCss("[type='submit']")
    MailElement save();

    @Name("Удалить сборщик")
    @FindByCss(".js-collector-remove")
    ElementsCollection<MailElement> delete();

    @Name("Показать настройки соединения с сервером")
    @FindByCss(".js-toggle-extra-info")
    ElementsCollection<MailElement> toggleServerSettings();

    @Name("Блок «Настройка соединения с сервером»")
    @FindByCss(".b-form-layout__block_server-settings")
    BlockCollectorServerSetup blockServerSetup();

    @Name("Поле ввода «Пароль»")
    @FindByCss("[name = 'password']")
    MailElement password();
}
