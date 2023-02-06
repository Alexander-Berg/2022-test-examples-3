package ru.yandex.autotests.innerpochta.touch.pages.searchblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by puffyfloof.
 */
public interface SearchHeader extends MailElement {

    @Name("Кнопка «Назад»")
    @FindByCss(".topBar-item_cancelSearch")
    MailElement back();

    @Name("Поле «Поиск писем»")
    @FindByCss(".search-inputText")
    MailElement input();

    @Name("Кнопка «Найти»")
    @FindByCss(".search-button")
    MailElement find();

    @Name("Крестик очистки поля «Поиск писем»")
    @FindByCss(".search-iconClear")
    MailElement clean();
}
