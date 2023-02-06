package ru.yandex.autotests.innerpochta.touch.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */
public interface Popup extends MailElement {

    @Name("Крестик")
    @FindByCss(".popup-close")
    MailElement closeBtn();

    @Name("Метки")
    @FindByCss(".popup-itemLabel")
    ElementsCollection<MailElement> labels();

    @Name("Готово")
    @FindByCss(".popup-button")
    MailElement done();

    @Name("Активное Готово")
    @FindByCss(".is-active.popup-button")
    MailElement activeDone();

    @Name("Галочка рядом с проставленной меткой")
    @FindByCss(".is-active.popup-itemLabel-tick")
    ElementsCollection<MailElement> tick();

    @Name("Галочка рядом с проставленной меткой")
    @FindByCss(".is-semiactive.popup-itemLabel-tick")
    ElementsCollection<MailElement> halfactiveTick();

    @Name("«Да» на попапе сохранения письма в черновики")
    @FindByCss(".popup-item_ok")
    MailElement yesBtn();

    @Name("«Нет» на попапе сохранения письма в черновики")
    @FindByCss(".popup-item:not(.popup-item_ok)")
    MailElement noBtn();

    @Name("Кнопки в выпадающем меню")
    @FindByCss(".popup-item")
    ElementsCollection<MailElement> btnsList();
}
