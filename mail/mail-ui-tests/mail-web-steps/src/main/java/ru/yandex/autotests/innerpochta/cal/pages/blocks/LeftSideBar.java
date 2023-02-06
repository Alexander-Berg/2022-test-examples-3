package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface LeftSideBar extends MailElement {

    @Name("Крестик в сайдбаре")
    @FindByCss("[class*=TouchAside__close]")
    MailElement close();

    @Name("Пункты меню сайдбара")
    @FindByCss("[class*=TouchMenuItem__withBorder]")
    ElementsCollection<MailElement> menuItems();

    @Name("Ссылка на полную версию календаря")
    @FindByCss("[class*=TouchAsideFooter__fullVersion]")
    MailElement fullView();

    @Name("Кнопка «Выйти из аккаунта»")
    @FindByCss("[class*=TouchAsideFooter__logout]")
    MailElement logOut();

    @Name("Кнопки смены вида")
    @FindByCss(".radio-button__text")
    ElementsCollection<MailElement> viewButtons();
}
