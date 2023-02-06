package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockLabels extends MailElement {

    @Name("Кнопка «Новая метка»")
    @FindByCss("[id = 'setup-labels-add']")
    MailElement newLabel();

    @Name("Кнопка «Изменить» метку")
    @FindByCss("[id = 'setup-labels-edit']")
    MailElement changeLabel();

    @Name("Кнопка «Удалить» метку")
    @FindByCss("[id = 'setup-labels-delete']")
    MailElement deleteLabel();

    @Name("Кнопка «Создать правило» для метки")
    @FindByCss("[id = 'setup-labels-filter']")
    MailElement createFilterForLabel();

    @Name("Дефолтные метки")
    @FindByCss(".b-folder-list_default .b-folder-list-item")
    ElementsCollection<MailElement> defaultLabels();

    @Name("Сортировать по имени")
    @FindByCss("[value='name']")
    MailElement sortByName();

    @Name("Сортировать по количеству писем")
    @FindByCss("[value='count']")
    MailElement sortByCount();

    @Name("Метки")
    @FindByCss(".b-label.b-label_rounded")
    ElementsCollection<MailElement> userLabelsList();
}
