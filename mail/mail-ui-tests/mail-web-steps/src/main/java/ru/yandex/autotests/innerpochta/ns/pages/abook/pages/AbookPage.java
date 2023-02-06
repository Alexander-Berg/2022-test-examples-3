package ru.yandex.autotests.innerpochta.ns.pages.abook.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.ToolbarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.contact.ContactBlock;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.dropdown.SelectConditionDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.dropdown.SelectGroupsDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.dropdown.SelectGroupsDropdownInPopup;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.group.GroupsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.popup.AddContactPopup;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.popup.ContactPopup;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.popup.CreateNewGroupPopup;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.Mail360HeaderBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 26.11.12
 * Time: 12:41
 */
public interface AbookPage extends MailPage {

    /*
     * Вкладка с контактами. Как Письма, только контакты!
     */
    @Name("Попап «Создать группу контактов»")
    @FindByCss(".mail-AbookPopup-NewGroup")
    CreateNewGroupPopup createNewGroupPopup();

    @Name("Попап добавления нового контакта")
    @FindByCss(".js-abook-person-popup")
    AddContactPopup addContactPopup();

    @Name("Попап существующего контакта")
    @FindByCss(".js-abook-person-popup")
    ContactPopup contactPopup();

    @Name("Блок c группами [слева]")
    @FindByCss(".ns-view-abook-left")
    GroupsBlock groupsBlock();

    @Name("Блок каждого контакта в адресной книге")
    @FindByCss(".ns-view-abook-contacts .js-abook-entry")
    ElementsCollection<ContactBlock> contacts();

    @Name("Тулбар - работа с контактами")
    @FindByCss(".mail-Toolbar_abook")
    ToolbarBlock toolbarBlock();

    @Name("Выпадающее меню c выбором групп")
    @FindByCss(".abook-groups-dropdown-popup")
    SelectGroupsDropdown groupSelectDropdown();

    @Name("Выпадающее меню c выбором условия для создания фильра")
    @FindByCss(".nb-select-dropdown .ui-autocomplete[style*='display: block;']")
    SelectConditionDropdown selectConditionDropdown();

    @Name("Попап")
    @FindByCss(".b-popup__box .b-popup__body")
    MailElement popup();

    @Name("Кнопка Сохранить для попапа")
    @FindByCss("button[data-dialog-action='dialog.submit']")
    MailElement popupSubmit();

    @Name("Попап меню кнопки еще")
    @FindByCss("._nb-popup-outer")
    MailElement popupMenu();

    @Name("Попап добавления контактов в группу")
    @FindByCss(".mail-AbookPopup-AddToGroup")
    MailElement addContactsToGroupPopup();

    @Name("Дропдаун кнопки «Еще»")
    @FindByCss("._nb-popup-link.ui-corner-all")
    ElementsCollection<MailElement> moreDropdown();

    @Name("Кнопка добавления контактов в группу")
    @FindByCss(".js-abook-popup-ok")
    MailElement addContactsToGroup();

    @Name("Кнопка «Еще контакты»")
    @FindByCss(".js-abook-load-more")
    MailElement moreContactsBtn();

    @Name("Выпадающее меню c выбором групп")
    @FindByCss(".ui-autocomplete")
    SelectGroupsDropdownInPopup selectGroupsDropdownInPopup();

    @Name("Шапка")
    @FindByCss(".mail-Header-Wrapper")
    Mail360HeaderBlock mail360HeaderBlock();

    @Name("Показать все контакты")
    @FindByCss(".js-abook-load-all")
    MailElement showAllContactsBtn();

    @Name("Плашка результатов поиска")
    @FindByCss(".mail-AbookHead-Title_content")
    MailElement searchResultsHeader();

    @Name("Выделенная группа контактов")
    @FindByCss(".js-abook-group.is-checked")
    MailElement checkedGroup();

    @Name("Кнопка «Написать»")
    @FindByCss(".qa-LeftColumn-ComposeButton")
    MailElement composeButton();

    @Name("Обновить")
    @FindByCss(".qa-LeftColumn-SyncButton")
    MailElement checkMailButton();

    @Name("Попап абука")
    @FindByCss(".ns-view-abook-popup")
    MailElement abookPopup();

}
