package ru.yandex.autotests.innerpochta.touch.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface SaveToDisk extends MailElement {

    @Name("Кнопка «Сохранить»")
    @FindByCss(".is-active.popup-button")
    MailElement saveBtn();

    @Name("Кнопка «Закрыть»")
    @FindByCss(".popup-close")
    MailElement close();

    @Name("Папки")
    @FindByCss(".diskTree-item")
    ElementsCollection<MailElement> folders();

    @Name("Выделенная папка")
    @FindByCss(".is-checked.diskTree-item")
    MailElement activeFolder();
}
