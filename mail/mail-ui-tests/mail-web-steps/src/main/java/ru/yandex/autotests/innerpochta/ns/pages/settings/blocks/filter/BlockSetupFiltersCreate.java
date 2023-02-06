package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter;


/**
 * User: lanwen
 * Date: 19.11.13
 * Time: 13:36
 */

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupFiltersCreate extends MailElement {

    @Name("Выбор писем для фильтра (Применять к...)")
    @FindByCss(".b-form-element_selects-as-text")
    FilterApplyConditionBlock blockApplyConditionFor();

    @Name("Блок условий для правила")
    @FindByCss(".b-form-layout__block:nth-of-type(2)")
    FilterCreateConditionsBlock blockCreateConditions();

    @Name("Выполнить действие...")
    @FindByCss("div.b-form-layout__block.b-form-layout__block_settings-filter:nth-of-type(3)")
    FilterActionsSelectBlock blockSelectAction();

    @Name("Выбор действия для правила (для сохранения требуется пароль)")
    @FindByCss("div.b-form-layout__block.b-form-layout__block_settings-filter:nth-of-type(4)")
    FilterPasswordProtectedActionsSelectBlock blockPasswordProtectedActions();

    @Name("Не применять остальные правила")
    @FindByCss("[name='stop']")
    MailElement dontApplyAnyOtherFilter();

    @Name("Также вы можете «указать название» правила")
    @FindByCss(".daria-action.b-link[action='filters.show-filter-name']")
    MailElement filterName();

    @Name("Поле ввода «Название» правила")
    @FindByCss("[name='name']")
    MailElement filterNameInput();

    @Name("Кнопка «Создать правило»")
    @FindByCss("button[type='submit']")
    MailElement submitFilterButton();

    @Name("Чекбокс «Применить к существующим письмам»")
    @FindByCss(".js-filter-apply-checkbox input")
    MailElement applyFilterCheckBox();

    @Name("Кнопка «Проверить»")
    @FindByCss(".js-preview")
    MailElement previewButton();

    @Name("Заголовок таблицы писем, попадающих под правило")
    @FindByCss(".js-preview-header:not(.g-hidden)")
    MailElement previewMessagesListHeader();

    @Name("Письма, попадающие под правило")
    @FindByCss(".ns-view-messages-item-preview")
    ElementsCollection<MailElement> previewMessagesList();

    @Name("Иконка аттачей у письм, попадающие под правило")
    @FindByCss(".js-show-attachments")
    ElementsCollection<MailElement> previewMessagesListWithAttach();

    @Name("Кнопка «Еще письма»")
    @FindByCss(".js-preview-more")
    MailElement moreMessagesButton();
}
