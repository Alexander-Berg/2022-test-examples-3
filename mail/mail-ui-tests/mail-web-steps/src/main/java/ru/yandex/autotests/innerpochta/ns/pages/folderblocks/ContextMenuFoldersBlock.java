package ru.yandex.autotests.innerpochta.ns.pages.folderblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface ContextMenuFoldersBlock extends MailElement {

    @Name("Настроить папки")
    @FindByCss(".qa-LeftColumn-ContextMenu-Setup")
    MailElement setupFolders();

    @Name("Создать папку")
    @FindByCss(".qa-LeftColumn-ContextMenu-CreateNewItem")
    MailElement addNewFolder();
}
