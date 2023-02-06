package ru.yandex.autotests.innerpochta.touch.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface PlusPopup extends MailElement {

    @Name("Кнопка «Интересно» в промо Плюса")
    @FindByCss("[href^='https://plus.yandex.ru']")
    MailElement plusInterestingBtn();

    @Name("Кнопка «Не сейчас» в промо Плюса")
    @FindByCss("[type='button']")
    MailElement notNowPlusPromoBtn();
}
