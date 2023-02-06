package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.elements;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CustomFoldersThreadBlock extends MailElement {

    @Name("Список папок в треде")
    @FindByCss(".b-folders__folder")
    ElementsCollection<CustomFolderBlock> customFolders();

    @Name("Развернуть/свернуть тред")
    @FindByCss(".b-toggler")
    MailElement toggler();

    @Name("Имя треда")
    @FindByCss(".b-folders__folder__name")
    MailElement name();
}
