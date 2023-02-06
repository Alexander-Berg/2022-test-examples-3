package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface EditEventPopups extends MailElement {

    @Name("Инпут текста")
    @FindByCss("[class*=TouchSuggest__input]")
    MailElement input();

    @Name("Список в саджесте")
    @FindByCss("[class*=TouchSuggest__suggestionWrapper]")
    ElementsCollection<MailElement> suggested();

    @Name("Список в саджесте переговорок")
    @FindByCss("[class*=TouchResource__wrap]")
    ElementsCollection<RoomSuggestBlock> suggestedRooms();

    @Name("Список участников в саджесте")
    @FindByCss("[class*=TouchSuggest__suggestionWrapper]")
    ElementsCollection<TouchSuggestedList> suggestedList();

    @Name("Кнопка сохранения")
    @FindByCss("[class*=saveButton]")
    MailElement save();

    @Name("Список добавленных участников")
    @FindByCss("[class*=TouchMembersPicker__pickedMember]")
    ElementsCollection<MailElement> pickedMembers();

    @Name("Пункты меню")
    @FindByCss("[class*=TouchMenuItem__wrap]")
    ElementsCollection<MailElement> menuItems();

    @Name("Крестик стирания текста из инпута")
    @FindByCss("[class*=TouchSuggest__rightControl]")
    MailElement clearInputBtn();

    @Name("Крестики удаления добавленного варианта")
    @FindByCss("[class*=TouchMembersPicker__removeMemberButtonContainer]")
    ElementsCollection<MailElement> removeItem();

    @Name("Кнопка возврата к экрану события в шапке")
    @FindByCss("[class*=TouchHeader__control]:first-child")
    MailElement backToEditEvent();

    @Name("«Ничего не нашлось» в саджесте контактов")
    @FindByCss("[class*=TouchMembersPicker__emptyStateText]")
    MailElement nothingFoundSuggest();

    @Name("«Ничего не нашлось» в саджесте переговорок")
    @FindByCss("[class*=TouchResourcesSuggestionsList__emptyStateText]")
    MailElement nothingFoundSuggestRooms();

    @Name("«Сейчас выбрана переговорка» в саджесте переговорок")
    @FindByCss(".modal_visible_yes [class*=TouchResourcesPicker__suggestSectionTitle]")
    MailElement choosenSuggestRooms();

    @Name("Кнопка выбора офиса на странице переговорок")
    @FindByCss(".modal_visible_yes [class*=button2_pin_circle-circle]")
    MailElement chooseOfficeButton();

    @Name("Настройки на экране повторения события")
    @FindByCss("[class*=TouchEventRepetition__formRow]")
    ElementsCollection<MailElement> menuSettings();

    @Name("Кнопка «Повторять до»")
    @FindByCss("[class*=TouchEventRepetition__dateInput]")
    MailElement repeatTo();

    @Name("Текст кнопки «Повторять до»")
    @FindByCss("[class*=TouchEventRepetition__dueDateText]")
    MailElement repeatToText();

    @Name("Кнопка «Повторять каждый N день/неделю/месяц»")
    @FindByCss("[class*=TouchEventRepetition__menuItem] [class*=TouchNativeControl__input]")
    MailElement repeatSetting();

    @Name("Кнопка «Очистить дату»")
    @FindByCss("[class*=TouchEventRepetition__dateInputClear]")
    MailElement clearDate();

    @Name("Выбранный день недели")
    @FindByCss("[class*=TouchMenuItem__check]")
    MailElement choosenWeekDay();

    @Name("Занятый участник")
    @FindByCss("[class*=TouchMember__busy]")
    MailElement busyMember();
}
