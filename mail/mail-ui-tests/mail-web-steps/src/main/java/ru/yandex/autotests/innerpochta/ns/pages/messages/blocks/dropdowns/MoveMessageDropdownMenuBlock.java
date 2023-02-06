package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MoveMessageDropdownMenuBlock extends MailElement {

    @Name("Папка в списке папок")
    @FindByCss("[data-click-action='move'] .js-folder-name")
    ElementsCollection<MailElement> customFolders();

    @Name("Входящие")
    @FindByCss("[title='Входящие'] .js-folder-name")
    MailElement inboxFolder();

    @Name("Создать новую папку")
    @FindByCss(".b-mail-dropdown__item_new-label a")
    MailElement createNewFolder();

    @Name("Поле ввода")
    @FindByCss("input")
    MailElement input();
}


