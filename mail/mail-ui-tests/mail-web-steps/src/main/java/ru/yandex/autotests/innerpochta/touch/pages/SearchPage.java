package ru.yandex.autotests.innerpochta.touch.pages;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.GroupOperationsToolbarHeader;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.GroupOperationsToolbarPhone;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.MessageBlock;
import ru.yandex.autotests.innerpochta.touch.pages.searchblocks.SearchHeader;

/**
 * @author puffyfloof
 */

public interface SearchPage extends MailPage {

    @Name("Шапка в поиске")
    @FindByCss(".topBar_search")
    SearchHeader header();

    @Name("Блок одного письма в списке писем")
    @FindByCss(".js-messages-message")
    MessageBlock messageBlock();

    @Name("Блок найденного письма в списке писем")
    @FindByCss(".js-messages-message")
    ElementsCollection<MessageBlock> messages();

    @Name("Саджест предыдущих запросов")
    @FindByCss(".searchSuggest")
    MailElement searchedBefore();

    @Name("Непустой саджест")
    @FindByCss(".searchSuggest-list")
    MailElement suggest();

    @Name("Список вариантов саджеста")
    @FindByCss(".searchSuggest-item .searchSuggest-link")
    ElementsCollection<MailElement> searchSuggestItems();

    @Name("Карусель фильтров расширенного поиска")
    @FindByCss("[class*=AdvancedSearch__items-] [class*=AdvancedSearch__item--]")
    ElementsCollection<MailElement> advancedSearchFilters();

    @Name("[Планшеты] Кнопка расширенных фильтров")
    @FindByCss("[class*=AdvancedSearch__large-device--] [class*=AdvancedSearch__item--]")
    MailElement advancedSearchFiltersBtn();

    @Name("[Планшеты] Попап с фильтрами")
    @FindByCss("[class*=AdvancedSearch__buttons-popup--]")
    MailElement advancedSearchFiltersPopup();

    @Name("Выбранный фильтр в поиске")
    @FindByCss("[class*=AdvancedSearch__is-active--]")
    MailElement advancedSearchActiveFilter();

    @Name("Попап Ещё фильтров расширенного поиска")
    @FindByCss("[class*=Selector__more-selector--]")
    MailElement advancedSearchMorePopup();

    @Name("Список фильтров попапа Ещё фильтров поиска")
    @FindByCss("[class*=Selector__body--] [class*=Selector__item--]")
    ElementsCollection<MailElement> advancedSearchMorePopupItems();

    @Name("Крестик в попапе Ещё фильтров поиска")
    @FindByCss("[class*=Selector__back-button--]")
    MailElement advancedSearchPopupClose();

    @Name("Попап фильтра Кому/От кого расширенного поиска")
    @FindByCss("[class*=Selector__contacts-selector--]")
    MailElement advancedSearchContactsPopup();

    @Name("Попап фильтра Папка расширенного поиска")
    @FindByCss("[class*=Selector__folder-selector--]")
    MailElement advancedSearchFolderPopup();

    @Name("Выбранная папка в попапе фильтра Папок расширенного поиска")
    @FindByCss("[class*=Selector__is-selected--]")
    MailElement selectedFolderInFolderPopup();

    @Name("Шапка в фильтре Папок")
    @FindByCss("[class*=Selector__header--]")
    MailElement headerInFolderPopup();

    @Name("Инпут поиска в фильтре Папок")
    @FindByCss("[class*=Selector__header--] input")
    MailElement inputInFolderPopup();

    @Name("Список папок в фильтре папок")
    @FindByCss(".list [class*=Selector__item--]")
    ElementsCollection<MailElement> foldersInPopupFolder();

    @Name("Попап фильтра Дата расширенного поиска")
    @FindByCss("[class*=Selector__dates-selector--]")
    MailElement advancedSearchDatesPopup();

    @Name("Совпадения, подсвеченные в поиске")
    @FindByCss(".msearch-highlight")
    MailElement searchHighlights();

    @Name("Лоадер во время загрузки поисковой выдачи")
    @FindByCss(".ico_loader")
    MailElement loader();

    @Name("Заглушка в пустой папке")
    @FindByCss(".messagesEmpty-goto_search")
    MailElement emptySearchResultImg();

    @Name("Тулбар груповых операций с письмами на телефонах")
    @FindByCss(".selectionOperations")
    GroupOperationsToolbarPhone groupOperationsToolbarPhone();

    @Name("Верхний тулбар групповых операций с письмами")
    @FindByCss(".topBar_messageSelection")
    GroupOperationsToolbarHeader groupOperationsToolbarHeader();

    @Name("Заголовок в контекстном саджесте")
    @FindByCss(".searchSuggest-groupTitle")
    MailElement groupSuggestTitle();

    @Name("Аватарки в поисковом саджесте")
    @FindByCss(".searchSuggest-avatar")
    MailElement searchSuggestAvatar();
}
