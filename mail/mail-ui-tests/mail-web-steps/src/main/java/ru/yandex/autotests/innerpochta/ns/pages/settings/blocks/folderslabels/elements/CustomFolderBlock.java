package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.elements;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CustomFolderBlock extends MailElement {

    @Name("Информация о папке")
    @FindByCss(".b-folders__folder__info")
    MailElement info();

    @Name("Имя папки")
    @FindByCss(".b-folders__folder__name .b-folders__folder__link")
    MailElement name();
}
