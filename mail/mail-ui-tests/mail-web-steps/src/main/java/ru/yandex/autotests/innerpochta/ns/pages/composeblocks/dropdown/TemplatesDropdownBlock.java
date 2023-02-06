package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface TemplatesDropdownBlock extends MailElement {

    @Name("Кнопка «Сохранить шаблон»")
    @FindByCss(".js-save-template")
    MailElement saveTemplateButton();

    @Name("Кнопка «Создать шаблон»")
    @FindByCss(".js-create-template")
    MailElement сreateTemplateButton();

    @Name("Кнопка «Сохранить как шаблон»")
    @FindByCss(".ComposeTemplatesOptions-Action_save")
    MailElement saveAsTemplateBtn();

    @Name("Кнопка «Сохранить как новый шаблон»")
    @FindByCss(".ComposeTemplatesOptions-Action_save_as")
    MailElement saveAsNewTemplateBtn();

    @Name("Кнопка «Обновить текущий шаблон»")
    @FindByCss(".ComposeTemplatesOptions-Action_update")
    MailElement updateTemplateBtn();

    @Name("Список существующих Шаблонов")
    @FindByCss(".ComposeTemplatesList-Item")
    ElementsCollection<MailElement> templatesList();
}
