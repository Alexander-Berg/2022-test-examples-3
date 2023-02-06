package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.mailinterface;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.09.12
 * Time: 17:08
 */

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupInterface extends MailElement {

    @Name("Вариант «На отдельной странице»")
    @FindByCss(".js-2pane .b-mail-button__inner")
    MailElement defaultInterface();

    @Name("Вариант «Справа от списка писем»")
    @FindByCss(".js-3pane-vertical .b-mail-button__text")
    MailElement vertical3Pane();

    @Name("Варианты интерфейсов")
    @FindByCss(".b-mail-button_group-row .b-mail-button__inner")
    ElementsCollection<MailElement> interfaceOptions();

    @Name("Изображение 3pane интерфейса")
    @FindByCss(".b-setup__layout:not([class*='g-hidden']) a img")
    MailElement picture3Pane();

    @Name("Ссылка «Изменения сохранены. Перейти во входящие.» (после смены интерфейса на 3pane)")
    @FindByCss(".b-setup__layout_saved:not([class*='g-hidden']) a")
    MailElement inboxLinkAfterInterfaceSwitch();

    @Name("Стрелка «Еще» справа (следующая группа интерфейсов)")
    @FindByCss(".b-color-scheme__control.b-color-scheme__control_next")
    MailElement nextInterfaces();

    @Name("Стрелка «Еще» слева (предыдущая группа интерфейсов)")
    @FindByCss(".b-color-scheme__control.b-color-scheme__control_prev div[class='b-color-scheme__control__arrow']")
    MailElement prevInterfaces();
}
