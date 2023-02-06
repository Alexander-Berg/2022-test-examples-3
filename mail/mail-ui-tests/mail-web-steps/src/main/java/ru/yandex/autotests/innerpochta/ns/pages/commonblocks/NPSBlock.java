package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author crafty
 */

public interface NPSBlock extends MailElement {

    @Name("Звезды в плашке NPS")
    @FindByCss(".promo-nps-star")
    ElementsCollection<MailElement> stars();

    @Name("Кнопка «Отмена» в плашке NPS")
    @FindByCss(".promo-nps__button[type='button']")
    MailElement cancelBtn();

    @Name("Кнопка «Отправить» в плашке NPS")
    @FindByCss(".promo-nps__button[type='submit']")
    MailElement submitBtn();

    @Name("Поле для комментария в плашке NPS")
    @FindByCss(".textarea__control")
    MailElement comment();

}