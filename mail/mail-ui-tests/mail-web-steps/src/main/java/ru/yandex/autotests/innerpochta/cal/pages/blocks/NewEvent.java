package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface NewEvent extends MailElement {

    @Name("Инпут «Название»")
    @FindByCss(".qa-NameField input")
    MailElement nameInput();

    @Name("Добавить «Описание»")
    @FindByCss(".qa-DescriptionField button")
    MailElement descriptionAddBtn();

    @Name("Инпут «Описание»")
    @FindByCss(".qa-DescriptionField textarea")
    MailElement descriptionInput();

    @Name("Поле «Участники»")
    @FindByCss(".qa-MembersField .qa-Picker")
    MailElement membersField();

    @Name("Инпут «Участники»")
    @FindByCss(".qa-MembersField input")
    MailElement membersInput();

    @Name("Список строк переговорок")
    @FindByCss("[class*=EventResourcesFieldItem__wrap]")
    ElementsCollection<RoomBlock> roomsList();

    @Name("Список рекомендованных переговорок")
    @FindByCss("[class*=SuggestMeetingTimesItem]")
    ElementsCollection<MailElement> recomendedRoomList();

    @Name("Лоадер виджета переговорок")
    @FindByCss("[class*=WidgetSlider__loader]")
    MailElement roomsLoader();

    @Name("Кнопка «Ещё переговорки»")
    @FindByCss("[class*=SuggestMeetingTimesAnyRoom]")
    MailElement moreRoomsButton();

    @Name("Выбранный временной интервал для переговорок")
    @FindByCss("[class*=WidgetSlider__title]")
    MailElement selectedTimeForRoom();

    @Name("Стрелки в виджете переговорок")
    @FindByCss("[class*=WidgetSlider__arrow]")
    ElementsCollection<MailElement> roomWidgetArrows();

    @Name("Неактивная стрелка в виджете переговорок")
    @FindByCss("[class*=WidgetSlider__arrow_disabled]")
    MailElement roomWidgetArrowDisabled();

    @Name("Поле «Место»")
    @FindByCss(".qa-LocationField")
    MailElement locationfiled();

    @Name("Инпут «Место»")
    @FindByCss(".qa-LocationField input")
    MailElement locationInput();

    @Name("Блок уведомлений")
    @FindByCss(".qa-NotificationField")
    Notifications notifyField();

    @Name("«Календарь»")
    @FindByCss(".qa-LayerField")
    MailElement layerField();

    @Name("Селект для выбора календаря")
    @FindByCss(".qa-LayerField")
    MailElement layerFieldSelect();

    @Name("Поле «Время и дата»")
    @FindByCss(".qa-DatesField")
    MailElement timeDateField();

    @Name("Инпут Время события")
    @FindByCss(".qa-DatesField-Time input")
    ElementsCollection<MailElement> timeInputList();

    @Name("Инпут Время события. Начало")
    @FindByCss(".qa-DatesField_Start-TimePicker input")
    MailElement time();

    @Name("Инпут Дата события")
    @FindByCss(".qa-DatesField-Date input")
    ElementsCollection<MailElement> dateInputList();

    @Name("Инпуты Дата события")
    @FindByCss(".qa-DatesField-Date input")
    ElementsCollection<MailElement> dateList();

    @Name("Инпут Время события. Конец")
    @FindByCss(".qa-DatesField_End-TimePicker input")
    MailElement timeEnd();

    @Name("Чекбокс «Весь день»")
    @FindByCss(".qa-DatesField-AllDay input")
    MailElement allDayCheckBox();

    @Name("Крестик для закрытия попапа")
    @FindByCss(".qa-NewEventPopup-Close")
    MailElement closePopup();

    @Name("Кнопка «Создать» в попапе")
    @FindByCss(".qa-EventFormPopup-CreateButton")
    MailElement createFromPopupBtn();

    @Name("Кнопка «Создать» на странице")
    @FindByCss(".qa-EventForm-CreateButton")
    MailElement createFromPageBtn();

    @Name("Кнопка «Больше параметров»")
    @FindByCss(".qa-EventFormPopup-MoreParams")
    MailElement moreParamsBtn();

    @Name("Чекбокс «Повторять событие»")
    @FindByCss(".qa-DatesField-Repetition label")
    MailElement repeatEventCheckBox();

    @Name("Кнопка изменить для редактирования опций повторения событий")
    @FindByCss(".qa-DatesField-Repetition .Link")
    MailElement repeatEventChangeButton();

    @Name("Ссылка «Изменить» для попапа повторения событий")
    @FindByCss(".qa-DatesField-Repetition .Link_theme_normal")
    MailElement changeRepeatPopup();

    @Name("Чекбокс «Доступ - участники могут редактировать»")
    @FindByCss(".qa-AccessField-CanEdit label")
    MailElement accessCanEditCheckBox();

    @Name("Кнопка «Сохранить» на странице")
    @FindByCss(".qa-EventForm-SaveButton")
    MailElement saveChangesBtn();

    @Name("Кнопка «Удалить» на странице")
    @FindByCss(".qa-EventFormDelete-Button")
    MailElement deleteEventBtn();

    @Name("Сообщение «Вы редактируете одно/все событие серии»")
    @FindByCss(".qa-EventRepetitionWarning")
    MailElement eventWarning();

    @Name("Ссылка «Перейти к редактированию серии»")
    @FindByCss(".qa-EventRepetitionWarning .Link")
    MailElement eventWarningLink();

    @Name("Список участников встречи")
    @FindByCss(".qa-Picker-Item")
    ElementsCollection<MailElement> membersList();

    @Name("Кнопка удаления участника встречи")
    @FindByCss("[class*=Yabble__closeWrap]")
    ElementsCollection<MailElement> memberDeleteBtn();

    @Name("Выделенный участник")
    @FindByCss("[class*=Yabble__wrap_hovered]")
    MailElement hoveredMember();

    @Name("Занятый участник")
    @FindByCss("[class*=Yabble__wrap_busy]")
    MailElement busyMember();

    @Name("Занятая переговорка")
    @FindByCss(".qa-ResourcesField [class*=Yabble__wrap_busy]")
    MailElement busyRoom();

    @Name("Выбранная переговорка")
    @FindByCss(".qa-ResourcesField [class*=Yabble__name]")
    MailElement room();

    @Name("Кнопка «Отмена»")
    @FindByCss(".qa-EventForm-CancelButton")
    MailElement cancelButton();

    @Name("Выпадушка «Статус»")
    @FindByCss(".qa-AvailabilityField button")
    MailElement status();

    @Name("Поле «Статус»")
    @FindByCss(".qa-AvailabilityField [class*=EventFormField__field]")
    MailElement statusField();

    @Name("Видимость «Видят все»")
    @FindByCss("span.RadioButton_view_default > label")
    ElementsCollection<MailElement> visibility();

    @Name("Выбранный вариант видимости")
    @FindByCss("span.RadioButton_view_default > label.RadioButton-Radio_checked")
    ElementsCollection<MailElement> visibilityChecked();

    @Name("Выпадушка «Офис»")
    @FindByCss("[class*=EventResourcesFieldItem__office]")
    MailElement office();

    @Name("Поле ввода названия переговорки")
    @FindByCss("[class*=EventResourcesFieldItem__resource] input")
    MailElement roomNameInput();

    @Name("Сообщение о редактировании одного события в серии/всей серии")
    @FindByCss("[class*=EventRepetitionWarning__text]")
    MailElement editSeriesMsg();

    @Name("Кнопка перехода в редактирование серии/одного события")
    @FindByCss("[class*=EventRepetitionWarning__text] .Link")
    MailElement changeToSeriesOrSingle();

    @Name("Ошибка в инпуте поля «Участники»")
    @FindByCss(".qa-Picker-Error")
    MailElement membersErrorMessage();

    @Name("Поле ввода даты старта события")
    @FindByCss(".qa-DatesField_Start-DatePicker-Input")
    MailElement dateStartInput();

    @Name("Кнопка добавления ссылки на телемост")
    @FindByCss("[class*=EventTelemostField__addButton]")
    MailElement telemostBtn();

    @Name("Заголовок формы")
    @FindByCss("[class*=EventForm__title]")
    MailElement title();

    @Name("Заголовок попапа")
    @FindByCss("[class*=EventCreatePopup__title-]")
    MailElement popupTitle();
}
