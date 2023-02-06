package ru.yandex.autotests.innerpochta.ns.pages.folderblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface ContextMenuCollectorsBlock extends MailElement {

    @Name("Настроить сборщики")
    @FindByCss(".qa-LeftColumn-ContextMenu-Setup")
    MailElement setupCollectors();

    @Name("Создать сборщик")
    @FindByCss(".qa-LeftColumn-ContextMenu-CreateNewItem")
    MailElement addNewCollector();
}
