package ru.yandex.autotests.innerpochta.ns.pages.wizard;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;


public interface NewWizardThemeBlock extends MailElement {

    @Name("Текущая тема")
    @FindByCss(".radiobox__radio_checked_yes")
    MailElement selectedTheme();

    @Name("Список всех тем")
    @FindByCss("label")
    ElementsCollection<MailElement> themes();
}
