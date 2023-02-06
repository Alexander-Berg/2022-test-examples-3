package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MoveMessageDropdownMenuMiniBlock extends MailElement {

    @Name("Папка в списке папок")
    @FindByCss(".js-folder-name")
    ElementsCollection<MailElement> customFolders();

    @Name("Входящие")
    @FindByCss("[title='Входящие'] .js-folder-name")
    MailElement inboxFolder();
}
