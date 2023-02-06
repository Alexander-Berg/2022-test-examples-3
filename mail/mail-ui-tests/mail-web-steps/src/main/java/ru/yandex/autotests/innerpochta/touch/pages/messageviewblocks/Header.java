package ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface Header extends MailElement {

    @Name("Кнопка назад")
    @FindByCss(".topBar-item_back")
    MailElement backToListBtn();

    @Name("Стрелочка «К предыдущему письму»")
    @FindByCss(".head-item_prev")
    MailElement prevMsg();

    @Name("Переключатель «К следующему письму»")
    @FindByCss(".head-item_next")
    MailElement nextMsg();

    @Name("Задизэйбленная cтрелочка «К предыдущему письму»")
    @FindByCss(".is-disabled.head-item_prev")
    MailElement disabledPrevMsg();

    @Name("Задизэйбленная стрелочка «К следующему письму»")
    @FindByCss(".is-disabled.head-item_next")
    MailElement disabledNextMsg();
}
