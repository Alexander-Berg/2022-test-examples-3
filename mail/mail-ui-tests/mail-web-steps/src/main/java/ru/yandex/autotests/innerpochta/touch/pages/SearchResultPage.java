package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.MessageBlock;
import ru.yandex.autotests.innerpochta.touch.pages.searchblocks.SearchFilterToolbar;
import ru.yandex.autotests.innerpochta.touch.pages.searchblocks.SearchHeader;
import ru.yandex.autotests.innerpochta.touch.pages.searchblocks.SearchOptions;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */
public interface SearchResultPage extends MailPage {

    @Name("Шапка в поиске")
    @FindByCss(".topBar_search")
    SearchHeader header();

    @Name("Тулбар поиска с папками")
    @FindByCss(".ns-view-search-filter")
    SearchFilterToolbar filterToolbar();

    @Name("Фильтры расширенного поиска")
    @FindByCss(".searchOptions")
    SearchOptions options();

    @Name("Блок одного письма в списке писем")
    @FindByCss(".js-messages-message")
    ElementsCollection<MessageBlock> messages();

    @Name("Кнопка расширенных фильтров")
    @FindByCss(".searchSwitch-filterToggle")
    MailElement moreOption();
}
