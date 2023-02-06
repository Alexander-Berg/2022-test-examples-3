package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.abook;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface AbookGroupsBlock extends MailElement {

    @Name("Кнопка «Создать группу»")
    @FindByCss(".js-setup-abook-create-group")
    MailElement createGroupButton();

    @Name("Кнопка «Переименовать» группу")
    @FindByCss(".js-setup-abook-rename-group")
    MailElement editGroupButton();

    @Name("Кнопка «Удалить» группу")
    @FindByCss(".js-setup-abook-remove-group")
    MailElement deleteGroupButton();

    @Name("Список созданных групп")
    @FindByCss(".js-setup-abook-group")
    ElementsCollection<AbookCustomUserGroupsBlock> createdGroups();
}