package ru.yandex.autotests.innerpochta.ns.pages.folderblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface CustomFolderBlock extends MailElement {

    @Name("Имя пользовательской папки")
    @FindByCss(".qa-LeftColumn-FolderText")
    MailElement customFolderName();

    @Name("Счетчик пользовательской папки")
    @FindByCss(".qa-LeftColumn-Counters")
    MailElement folderCounter();
}
