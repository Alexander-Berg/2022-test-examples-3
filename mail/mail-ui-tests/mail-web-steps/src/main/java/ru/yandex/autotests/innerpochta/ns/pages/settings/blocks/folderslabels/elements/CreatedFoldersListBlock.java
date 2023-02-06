package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.elements;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CreatedFoldersListBlock extends MailElement {

    @Name("Список папок")
    @FindByCss(".b-folders__folder")
    ElementsCollection<CustomFolderBlock> customFolders();

    @Name("Список раскрывающихся папок")
    @FindByCss(".b-folders__nesting:not(.b-folder-list_user)")
    ElementsCollection<CustomFoldersThreadBlock> customFoldersThreads();

    @Name("Закрытые переключатели (плюсы)")
    @FindByCss(".b-folders__nesting_closed .b-toggler")
    ElementsCollection<MailElement> closedTogglers();
}
