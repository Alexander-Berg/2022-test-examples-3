package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface LabelMessageDropdownMenuBlock extends MailElement {

    @Name("Отметить прочитанным")
    @FindByCss(".js-read")
    MailElement markAsRead();

    @Name("Отметить непрочитанным")
    @FindByCss(".js-unread")
    MailElement markAsUnread();

    @Name("Отметить важным")
    @FindByCss("[data-click-action='label'] .b-mail-icon.b-mail-icon_important")
    MailElement labelImportant();

    @Name("Снять метку Важное")
    @FindByCss("[data-click-action='unlabel']>img")
    MailElement unlabelImportant();

    @Name("Создать новую метку")
    @FindByCss(".b-mail-dropdown__item__content.js-label-new")
    MailElement createNewLabel();

    @Name("Поле поиска")
    @FindByCss(".js-search-input")
    MailElement searchField();

    @Name("Снять пользовательскую метку")
    @FindByCss(".b-mail-dropdown__item_simple:not([class*='g-hidden']) [data-click-action='unlabel'].js-action")
    ElementsCollection<MailElement> unlabelCustomMark();

    @Name("Пользовательские метки в меню установок меток для письма")
    @FindByCss(".b-mail-dropdown__item:not(.g-hidden) .js-action .b-mail-dropdown__item__content__wrapper .b-label__content")
    ElementsCollection<MailElement> customMarks();
}


