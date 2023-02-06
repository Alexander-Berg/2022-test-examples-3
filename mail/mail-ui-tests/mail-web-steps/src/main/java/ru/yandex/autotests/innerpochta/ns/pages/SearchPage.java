package ru.yandex.autotests.innerpochta.ns.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.AdvancedSearchBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.DropdownAdvancedSearch;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.Mail360HeaderBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines
 */
public interface SearchPage extends MailPage {

    @Name("Шапка")
    @FindByCss(".mail-Header-Wrapper")
    Mail360HeaderBlock mail360HeaderBlock();

    @Name("Саджест поиска")
    @FindByCss(".search__popup")
    MailElement searchSuggest();

    @Name("Иконкa саджеста - прошлый запрос")
    @FindByCss(".svgicon-mail--SearchSuggest-History_2")
    MailElement searchSuggestHistoryIcon();

    @Name("Иконкa саджеста - папка")
    @FindByCss(".svgicon-mail--SearchSuggest-Folder")
    MailElement searchSuggestFolderIcon();

    @Name("Иконкa саджеста - метка")
    @FindByCss(".svgicon-mail--SearchSuggest-Label")
    MailElement searchSuggestLabelIcon();

    @Name("Иконкa саджеста - тема")
    @FindByCss(".svgicon-mail--SearchSuggest-Mail")
    MailElement searchSuggestSubjectIcon();

    @Name("Иконкa саджеста - Непрочитанные")
    @FindByCss(".svgicon-mail--SearchSuggest-Unread")
    MailElement searchSuggestUnreadIcon();

    @Name("Иконкa саджеста - Важные")
    @FindByCss(".svgicon-mail--SearchSuggest-Important")
    MailElement searchSuggestFlaggedIcon();

    @Name("Иконкa саджеста - YQL")
    @FindByCss(".svgicon-mail--SearchSuggest-Ql")
    MailElement searchSuggestYqlIcon();

    @Name("Адрес контакта в саджесте")
    @FindByCss(".mail-SuggestItem-Rocks-Contact-Email")
    MailElement searchSuggestContactEmail();

    @Name("Письмо в саджесте - тема")
    @FindByCss(".mail-SuggestItem-Rocks-Mail-Subject")
    MailElement searchSuggestMailSubject();

    @Name("Письмо в саджесте")
    @FindByCss(".mail-SuggestItem-Rocks-Content")
    MailElement searchSuggestMailContent();

    @Name("Темы писем в саджесте")
    @FindByCss(".mail-SuggestItem-Rocks-Content")
    ElementsCollection<MailElement> searchSuggestMailThemes();

    @Name("Аттач в саджесте")
    @FindByCss(".mail-SuggestItem-Rocks-Attach-Files")
    MailElement searchSuggestMailAttach();

    @Name("Календарь для выбора дат")
    @FindByCss(".react-datepicker__month-container")
    MailElement calendar();

    @Name("Список прошлых запросов")
    @FindByCss(".search-result__item_type_history")
    ElementsCollection<MailElement> lastQueriesList();

    @Name("Результаты поиска")
    @FindByCss(".mail-MessagesSearchInfo_Summary")
    MailElement searchResultsHeader();

    @Name("Остальные результаты")
    @FindByCss(".mail-MessagesSearchInfo.js-messages-header")
    MailElement otherResultsHeader();

    @Name("Дропдаун выбора полей для поиска")
    @FindByCss(".menu")
    ElementsCollection<DropdownAdvancedSearch> dropdownAdvancedSearch();

    @Name("Попап расширенного поиска")
    @FindByCss(".mail-AdvancedSearch")
    AdvancedSearchBlock advancedSearchBlock();

    @Name("Дропдаун выбора папки")
    @FindByCss(".mail-AdvancedSearch-FolderSelector .menu__item")
    ElementsCollection<MailElement> folder();

    @Name("Дропдаун выбора дополнительных опций")
    @FindByCss(".mail-AdvancedSearch-MoreSelector .menu__item")
    ElementsCollection<MailElement> more();

    @Name("Инпуты для ввода диапазона дат")
    @FindByCss(".react-datepicker__input")
    ElementsCollection<MailElement> dataRangeInputs();

    @Name("Иконка контакта в саджесте")
    @FindByCss(".search__popup .mail-User-Avatar")
    MailElement searchUserAvatar();

    @Name("Дропдаун выбора даты")
    @FindByCss(".mail-AdvancedSearch-DateSelector__buttons-block .button2_theme_action")
    MailElement dateSearch();

    @Name("Попап выбора даты")
    @FindByCss(".mail-AdvancedSearch-DateSelector")
    MailElement dateSearchPopup();

    @Name("Кнопка «Написать»")
    @FindByCss(".qa-LeftColumn-ComposeButton")
    MailElement composeButton();

    @Name("Обновить")
    @FindByCss(".qa-LeftColumn-SyncButton")
    MailElement checkMailButton();

    @Name("Крестик для удаления запроса из истории")
    @FindByCss(".mail-SuggestItem-Rocks-History-Remove")
    MailElement searchSuggestRemove();
}
