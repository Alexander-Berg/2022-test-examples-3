package ru.yandex.autotests.innerpochta.touch.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */
public interface NotFoundPage extends MailElement {

    @Name("Картинка 404")
    @FindByCss(".pageNotFound-img")
    MailElement notFoundImg();

    @Name("Кнопка «Во входящие»")
    @FindByCss(".pageNotFound-link")
    MailElement notFoundButton();
}

