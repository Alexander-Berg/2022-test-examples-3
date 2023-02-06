package ru.yandex.autotests.innerpochta.touch.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */
public interface FolderPopup extends MailElement {

    @Name("Папка")
    @FindByCss(".leftPanelItem-text")
    ElementsCollection<MailElement> folders();

    @Name("Закрыть")
    @FindByCss(".aside-close")
    MailElement closePopup();

    @Name("Стрелочка, разворачивающая подпапки")
    @FindByCss(".ico_arrow-mini")
    MailElement toggler();

    @Name("Развернётые подпапки")
    @FindByCss(".is-expanded.leftPanel_folder")
    MailElement expandedFolders();
}
