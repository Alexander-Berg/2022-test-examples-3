package ru.yandex.autotests.innerpochta.touch.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface Viewer extends MailElement {

    @Name("Область с иконкой или изображением во вьюере")
    @FindByCss(".lightbox-body")
    MailElement viewerBody();

    @Name("Троббер во вьюере")
    @FindByCss(".lightbox-loader")
    MailElement viewerLoader();

    @Name("Кнопка «Закрыть» во вьюере")
    @FindByCss(".lightbox-back")
    MailElement viewerClose();

    @Name("Кнопка «Сохранить на диск»")
    @FindByCss(".lightbox-disk-save")
    MailElement saveToDisk();

    @Name("Кнопка «Открыть файл»")
    @FindByCss(".js-open")
    ElementsCollection<MailElement> openFileBtn();

    @Name("Счётчик аттачей")
    @FindByCss(".lightbox-position")
    MailElement attachCounter();

    @Name("Область аттача")
    @FindByCss(".lightbox-attachment")
    ElementsCollection<MailElement> attach();
}
