package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface WidgetBlock extends MailElement {

    @Name("Цветной корешок")
    @FindByCss(".mail-MessageSnippet-WidgetDecoration")
    MailElement widgetDecoration();

    @Name("Кнопки-ссылки в виджете")
    @FindByCss(".js-widget-click-button")
    ElementsCollection<MailElement> widgetClickBtns();

    @Name("Кнопка для печати")
    @FindByCss(".js-print-widget-button")
    MailElement printButton();

    @Name("Кнопка «Исправить»")
    @FindByCss(".js-compose-widget-button")
    MailElement composeButton();

    @Name("Кнопка «Удалить»")
    @FindByCss(".js-delete-widget-button")
    MailElement deleteButton();

    @Name("Любая виджетная кнопка")
    @FindByCss(".nb-button")
    ElementsCollection<MailElement> widgetBtns();
}
