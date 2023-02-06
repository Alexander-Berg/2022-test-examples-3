package ru.yandex.autotests.innerpochta.cal.pages;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.CalHeaderBlock;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.ContactsSuggest;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.DecisionEventList;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.DeleteEventPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.EditCalSidebar;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.EditEvent;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.EventDecisionPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.GeneralSettings;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.GridEvent;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.LeftPanel;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.MeetingsPage;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.MiniCalendar;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.NewCalSidebar;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.NewEvent;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.NewFeedSidebar;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.RepeatPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.RoomEvent;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.RoomWidget;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.SchedulePage;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.Settings;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.Todo;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.TodoItemPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.ViewEventPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.ViewSomeoneElseEvent;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.WarningPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.WelcomeWizard;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.UserMenuBlock;

/**
 * @author cosmopanda
 */

public interface HomePage extends MailPage {

    @Name("Колонки")
    @FindByCss(".qa-WeekGridColumn")
    ElementsCollection<MailElement> columnsList();

    @Name("Колонка текущий день")
    @FindByCss(".qa-WeekGridColumn-Current")
    MailElement currentColumn();

    @Name("Items")
    @FindByCss(".WeekGridColumn__items")
    MailElement currentColumnItems();

    @Name("Попап создания события")
    @FindByCss(".qa-NewEventPopup")
    NewEvent newEventPopup();

    @Name("Виджет доступных переговорок")
    @FindByCss("[class*=WidgetSlider__wrap]")
    ElementsCollection<RoomWidget> roomsWidget();

    @Name("Предупреждение о выходе из создания события")
    @FindByCss(".qa-Confirm")
    WarningPopup warningPopup();

    @Name("Мини календарь")
    @FindByCss(".qa-DatesField_Start-DatePicker")
    MiniCalendar miniCalendar();

    @Name("Слои в попапе создания события")
    @FindByCss(".qa-LayerField-Item")
    ElementsCollection<MailElement> layersList();

    @Name("События в сетке")
    @FindByCss(".qa-WeekGridColumn-Current .qa-GridEvent")
    ElementsCollection<GridEvent> eventsTodayList();

    @Name("Все события в сетке")
    @FindByCss(".qa-GridEvent")
    ElementsCollection<GridEvent> eventsAllList();

    @Name("Все события на весь день в сетке")
    @FindByCss(".qa-WeekGridAllday-Events")
    ElementsCollection<GridEvent> allDayEventsAllList();

    @Name("События на весь день")
    @FindByCss("[class*=GridEvent__content]")
    ElementsCollection<GridEvent> allDayEvents();

    @Name("Попап")
    @FindByCss(".popup")
    MailElement popup();

    @Name("Время в выпадушке")
    @FindByCss(".qa-DatesField_Start-TimePicker-Item")
    ElementsCollection<MailElement> timesList();

    @Name("Список видов в выпадушке")
    @FindByCss(".qa-AsideView-Item")
    ElementsCollection<MailElement> viewsList();

    @Name("Кнопка открытия тудушки в шапке")
    @FindByCss(".qa-Header-Todo")
    MailElement openTodoBtn();

    @Name("Тудушка")
    @FindByCss(".qa-Todo")
    Todo todo();

    @Name("Левая колонка")
    @FindByCss(".qa-Aside")
    LeftPanel leftPanel();

    @Name("Кнопки выбора вида")
    @FindByCss(".qa-AsideView-Item")
    ElementsCollection<MailElement> selectView();

    @Name("Развёрнутая левая колонка")
    @FindByCss(".Aside__expanded--1fv5j")
    LeftPanel expandedLeftPanel();

    @Name("Попап просмотра события")
    @FindByCss(".qa-EventFormPreview")
    ViewEventPopup viewEventPopup();

    @Name("Дело")
    @FindByCss(".qa-TodoDaily-Items")
    TodoItemPopup todoItemPopup();

    @Name("Дела в сетке")
    @FindByCss(".qa-GridTodoGroup")
    ElementsCollection<MailElement> todoItemsList();

    @Name("Страница создания события")
    @FindByCss(".qa-EventPage")
    NewEvent newEventPage();

    @Name("Страница редактирования события")
    @FindByCss(".qa-EventPage")
    EditEvent editEventPage();

    @Name("Страница просмотра чужого события")
    @FindByCss(".qa-EventPage")
    ViewSomeoneElseEvent viewSomeoneElseEventPage();

    //Contacts suggest
    @Name("Саджест контактов")
    @FindByCss(".qa-Suggest-Items")
    MailElement suggest();

    @Name("Элемент саджеста")
    @FindByCss(".qa-Suggest-Item")
    ElementsCollection<ContactsSuggest> suggestItem();

    @Name("Еще n событий")
    @FindByCss(".qa-GridMore")
    MailElement moreEventBtn();

    @Name("Дата в сетке на текущий месяц")
    @FindByCss(".qa-MonthGridDay-DayNumber")
    ElementsCollection<MailElement> dayInMonthGridBtn();

    @Name("Текущая дата в сетке на месяц")
    @FindByCss(".MonthGridDay__current--1kNBv")
    MailElement currentDayInMonthGrid();

    @Name("Текущая дата в сетке на неделю")
    @FindByCss(".WeekGridHeaderDay__wrap_current--3_33e > a")
    MailElement currentDayInWeekGridBtn();

    @Name("Текущая дата в сетке на неделю")
    @FindByCss(".WeekGridHeaderDay__day--opgg-")
    MailElement dayGridHeaderDay();

    @Name("Ссылка на 15 число текущего месяца")
    @FindByCss(".qa-MonthGridDay-DayNumber[href*='21']")
    MailElement dayFifteenInMonthGridBtn();

    @Name("Даты в сетке на неделю")
    @FindByCss(".WeekGridHeaderDay__day--opgg-")
    ElementsCollection<MailElement> weekGridHeaderDays();

    @Name("Элемент выпадающего меню")
    @FindByCss(".menu__item")
    ElementsCollection<MailElement> menuItems();

    @Name("Импорт календаря в «Новый календарь»")
    @FindByCss(".qa-LayerReference-Item")
    MailElement newCalImport();

    @Name("Опция «Только это событие»")
    @FindByCss("[data-key*='item-0']")
    MailElement editOneEvent();

    @Name("Опция «Все события»")
    @FindByCss("[data-key*='item-1']")
    MailElement editAllEvents();

    @Name("Сайдбар добавления календаря")
    @FindByCss(".qa-AddLayer")
    NewCalSidebar addCalSideBar();

    @Name("Сайдбар редактирования календаря")
    @FindByCss(".qa-EditLayer")
    EditCalSidebar editCalSideBar();

    @Name("Сайдбар добавления подписки")
    @FindByCss(".qa-AddFeed")
    NewFeedSidebar addFeedSideBar();

    @Name("Ссылка «Импортировать»")
    @FindByCss(".qa-AddLayer-TabImport")
    MailElement importLink();

    @Name("Приветственный визард")
    @FindByCss(".qa-WelcomeWizard")
    WelcomeWizard welcomeWizard();

    @Name("Неделя в сетке на месяц")
    @FindByCss(".qa-MonthGridRow")
    ElementsCollection<MailElement> weeksInMonthGrid();

    @Name("Сетка на месяц")
    @FindByCss(".MonthGrid__rows--2QyRf")
    MailElement monthGrid();

    @Name("Настройки левой колонки")
    @FindByCss(".qa-EditLayer")
    Settings settings();

    @Name("Список таймзон для изменения")
    @FindByCss(".qa-AsideTimezone-Item")
    ElementsCollection<MailElement> timezoneItems();

    @Name("Иконка шаренных календарей")
    @FindByCss(".qa-AsideLayers_Layers-LayerUnlock")
    MailElement unlockCalIcon();

    @Name("Иконка шаренных подписок")
    @FindByCss(".qa-AsideLayers_Subscriptions-LayerUnlock")
    MailElement unlockSubIcon();

    @Name("Попап удаления события")
    @FindByCss(".qa-EventFormDelete-Modal")
    DeleteEventPopup removeEventPopup();

    @Name("Шапка")
    @FindByCss(".PSHeader")
    CalHeaderBlock calHeaderBlock();

    @Name("Попап подтверждения действия")
    @FindByCss(".qa-EventDecision-Popup")
    EventDecisionPopup eventDecisionPopup();

    @Name("Кнопка «Не пойду» в выпадушке действий для события в поп-апе")
    @FindByCss(".qa-EventDecision-MenuItemNo")
    MailElement solutionsBtnNo();

    @Name("Кнопка «Возможно, пойду» в выпадушке действий для события в поп-апе")
    @FindByCss(".qa-EventDecision-MenuItemMaybe")
    MailElement solutionsBtnMaybe();

    @Name("Настройки")
    @FindByCss(".qa-Settings")
    GeneralSettings generalSettings();

    @Name("Дни недели в выпадушке")
    @FindByCss(".qa-SettingsWeekStartDay-Item")
    ElementsCollection<MailElement> weekStartsList();

    @Name("Часовые пояса в выпадушке")
    @FindByCss(".qa-SettingsTimezone-Item")
    ElementsCollection<MailElement> timezoneList();

    @Name("Начало дня в выпадушке")
    @FindByCss(".qa-SettingsDayStartHour-Item")
    ElementsCollection<MailElement> dayStartsList();

    @Name("Нотификация о сохранении изменений")
    @FindByCss(".notification-success")
    MailElement successNotify();

    @Name("Нотификация об ошибке")
    @FindByCss(".notification-error")
    MailElement errorNotify();

    @Name("Чекбокс «Показывать дела»")
    @FindByCss(".qa-SettingsMisc-ShowTodosInGrid .checkbox__control")
    MailElement showTodos();

    @Name("Чекбокс «Показывать номер недели»")
    @FindByCss(".qa-SettingsMisc-ShowWeekNumber .checkbox__control")
    MailElement showWeekNumber();

    @Name("Чекбокс «Показывать выходные»")
    @FindByCss(".qa-SettingsMisc-ShowWeekends .checkbox__control")
    MailElement showWeekends();

    @Name("Часовой промежуток сетки")
    @FindByCss(".WeekGridTimeRuler__cell--24Iia")
    ElementsCollection<MailElement> hourCell();

    @Name("Всплывающее уведомление")
    @FindByCss(".notifications-wrapper")
    MailElement notificationMessage();

    @Name("Попап «Повторять событие»")
    @FindByCss("[class*=EventRepetition__popup-]")
    RepeatPopup repeatPopup();

    @Name("Ссылка «Изменить» в попапе «Повторять событие»")
    @FindByCss(".qa-DatesField .Link_theme_normal")
    MailElement changeRepeatPopup();

    @Name("Информация о повторе события")
    @FindByCss(".qa-DatesField-Repetition")
    MailElement repeatInfo();

    @Name("Развернутый блок Весь день")
    @FindByCss(".WeekGridAllday__expander_expanded--1fQrU")
    MailElement allDayExpandedBlock();

    @Name("Залогиновое меню")
    @FindByCss(".legouser__popup")
    MailElement userMenu();

    @Name("Ссылка «Выйти» в залогиновом меню")
    @FindByCss(".legouser__menu-item_action_exit")
    MailElement logout();

    @Name("Выпадушка выбора языка")
    @FindByCss(".qa-AsideLang-Item")
    ElementsCollection<MailElement> langDropdownItem();

    @Name("Добавить пользователя")
    @FindByCss(".legouser__add-account")
    MailElement adduser();

    @Name("Список залогиненных пользователей")
    @FindByCss(".legouser__accounts .user-account_has-subname_yes")
    ElementsCollection<MailElement> userList();

    @Name("Кнопка «Удалить слой»")
    @FindByCss(".qa-EditLayerDelete-Button")
    MailElement deleteCal();

    @Name("Попап удаления календаря")
    @FindByCss(".qa-EditLayerDelete-Modal")
    MailElement deleteCalPopup();

    @Name("Чекбокс «Перенести события в другой календарь»")
    @FindByCss(".qa-EditLayerDelete-RelocateCheckbox .checkbox__control")
    MailElement deleteCalCheckbox();

    @Name("Кнопка «Удалить» в попапе удаления календаря")
    @FindByCss(".qa-EditLayerDelete-ModalButtonDelete")
    MailElement deleteCalButton();

    @Name("Задизейбленный чекбокс «Сделать основным»")
    @FindByCss(".qa-LayerExtraParams-IsDefault .checkbox_disabled_yes")
    MailElement disabledSetDefaultCalCheckbox();

    @Name("Попапы при выходе из редактирования события")
    @FindByCss("[class*=Confirm__content-]")
    ElementsCollection<WarningPopup> warPopups();

    @Name("Попап удаления календаря")
    @FindByCss(".EditLayerDelete__modalWrap--DdqRm")
    MailElement deleteLayerPopup();

    @Name("Инпут в попапе удаления")
    @FindByCss(".qa-EditLayerDelete-LayerNameInput .textinput__control")
    MailElement deleteLayerPopupInput();

    @Name("Список статусов занятости для события")
    @FindByCss(".Popup2_visible .Menu-Item_type_option")
    ElementsCollection<MailElement> statusList();

    //Repeat event popup
    @Name("Поле «Повторять до»")
    @FindByCss("[class*=EventRepetition__dueDateInput]")
    MailElement beforeDateField();

    @Name("Мини календарь для «Повторять до»")
    @FindByCss("[class*=EventRepetition__dueDateInput] .react-datepicker-popper")
    MiniCalendar beforeDateMiniCalendar();

    @Name("Попап выбора офиса")
    @FindByCss("[class*=EventResourcesFieldItem] .Menu-Item_type_option .Menu-Text")
    ElementsCollection<MailElement> officesList();

    @Name("Попап выбора офиса на странице переговорок")
    @FindByCss("[class*=OfficePicker__officesGroup] .menu__item_type_option")
    ElementsCollection<MailElement> officesListAtOfficePage();

    @Name("Карточка переговорки")
    @FindByCss("[class*=RoomCardInfo__wrap]")
    MailElement roomCard();

    @Name("Название переговорки в карточке")
    @FindByCss("[class*=RoomCardInfo__title]")
    MailElement roomName();

    @Name("Флажок у переговорки в карточке")
    @FindByCss("[class*=RoomCardInfo__locationIcon]")
    MailElement roomLocation();

    @Name("Ссылка расписания у переговорки в карточке")
    @FindByCss("[class*=RoomCardInfo__scheduleLink]")
    MailElement roomScheduleLink();

    @Name("Страница переговорок")
    @FindByCss("[class*=Invite__wrap]")
    MeetingsPage meetingsPage();

    @Name("Выпадушка «Офис»")
    @FindByCss("[class*=AsideResourcesFilter__section]")
    MailElement office();

    @Name("Имя встречи на странице переговорок")
    @FindByCss("[class*=TimelineIntervalCard__event]")
    RoomEvent roomEvent();

    @Name("Дни в сетке на месяц")
    @FindByCss("[class*=MonthGridDay__wrap]")
    ElementsCollection<MailElement> daysInMonthView();

    @Name("Список саджестов времени окончания события")
    //TODO: @FindByCss(".qa-DatesField_End-TimePicker-Item")
    @FindByCss(".Popup2_visible .Menu-Item_type_option")
    ElementsCollection<MailElement> timeEndVariants();

    @Name("Календарь для выбора дат")
    @FindByCss(".react-datepicker__month-container")
    MiniCalendar calendar();

    @Name("Страница расписания")
    @FindByCss("[class*=SchedulePage__wrap]")
    SchedulePage schedulePage();

    @Name("Список значений для установки стандартного уведомления")
    @FindByCss(".qa-NotificationsFieldItem_0-OffsetItem")
    ElementsCollection<MailElement> valueUntilList();

    @Name("Список типов единиц измерения для значений установки стандартного уведомления")
    @FindByCss(".qa-NotificationsFieldItem_1-UnitItem")
    ElementsCollection<MailElement> typeUntilList();

    @Name("Карточка событий")
    @FindByCss("[class*=TimelineIntervalCard__wrap]")
    MailElement eventsCard();

    @Name("Опции периода повторения")
    @FindByCss("[class*=EventRepetition__header] .menu__item_type_option")
    ElementsCollection<MailElement> repeatPeriodOption();

    @Name("Уведомление об ошибке")
    @FindByCss(".notifications-tr")
    MailElement notificationError();

    @Name("Ссылка на помощь в уведомлении об ошибке")
    @FindByCss(".notifications-tr .link")
    MailElement helpLink();

    @Name("Уведомление")
    @FindByCss(".notification-message")
    MailElement notificationMsg();

    @Name("Выпадушка решений о присутствии на встрече")
    @FindByCss("[class*=EventDecision__decisionDropDownMenu]")
    DecisionEventList decisionEventList();

    @Name("Старая шапка")
    @FindByCss("[class*=Header__wrap]")
    CalHeaderBlock oldCalHeaderBlock();

    @Name("Ссылка «Выйти» в залогиновом меню в старой шапке")
    @FindByCss(".qa-HeaderUserPopup_signOut")
    MailElement oldLogout();

    @Name("Добавить пользователя в старой шапке")
    @FindByCss(".qa-HeaderUserPopup_users_addUser")
    MailElement oldAddUser();

    @Name("Крестик информера на корпе")
    @FindByCss(".MessageBox-Close")
    MailElement closeWidget();

    @Name("Выпадающее меню пользователя")
    @FindByCss(".legouser__menu")
    UserMenuBlock userMenuDropdown();
}
