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

    @Name("??????????????")
    @FindByCss(".qa-WeekGridColumn")
    ElementsCollection<MailElement> columnsList();

    @Name("?????????????? ?????????????? ????????")
    @FindByCss(".qa-WeekGridColumn-Current")
    MailElement currentColumn();

    @Name("Items")
    @FindByCss(".WeekGridColumn__items")
    MailElement currentColumnItems();

    @Name("?????????? ???????????????? ??????????????")
    @FindByCss(".qa-NewEventPopup")
    NewEvent newEventPopup();

    @Name("???????????? ?????????????????? ??????????????????????")
    @FindByCss("[class*=WidgetSlider__wrap]")
    ElementsCollection<RoomWidget> roomsWidget();

    @Name("???????????????????????????? ?? ???????????? ???? ???????????????? ??????????????")
    @FindByCss(".qa-Confirm")
    WarningPopup warningPopup();

    @Name("???????? ??????????????????")
    @FindByCss(".qa-DatesField_Start-DatePicker")
    MiniCalendar miniCalendar();

    @Name("???????? ?? ???????????? ???????????????? ??????????????")
    @FindByCss(".qa-LayerField-Item")
    ElementsCollection<MailElement> layersList();

    @Name("?????????????? ?? ??????????")
    @FindByCss(".qa-WeekGridColumn-Current .qa-GridEvent")
    ElementsCollection<GridEvent> eventsTodayList();

    @Name("?????? ?????????????? ?? ??????????")
    @FindByCss(".qa-GridEvent")
    ElementsCollection<GridEvent> eventsAllList();

    @Name("?????? ?????????????? ???? ???????? ???????? ?? ??????????")
    @FindByCss(".qa-WeekGridAllday-Events")
    ElementsCollection<GridEvent> allDayEventsAllList();

    @Name("?????????????? ???? ???????? ????????")
    @FindByCss("[class*=GridEvent__content]")
    ElementsCollection<GridEvent> allDayEvents();

    @Name("??????????")
    @FindByCss(".popup")
    MailElement popup();

    @Name("?????????? ?? ??????????????????")
    @FindByCss(".qa-DatesField_Start-TimePicker-Item")
    ElementsCollection<MailElement> timesList();

    @Name("???????????? ?????????? ?? ??????????????????")
    @FindByCss(".qa-AsideView-Item")
    ElementsCollection<MailElement> viewsList();

    @Name("???????????? ???????????????? ?????????????? ?? ??????????")
    @FindByCss(".qa-Header-Todo")
    MailElement openTodoBtn();

    @Name("??????????????")
    @FindByCss(".qa-Todo")
    Todo todo();

    @Name("?????????? ??????????????")
    @FindByCss(".qa-Aside")
    LeftPanel leftPanel();

    @Name("???????????? ???????????? ????????")
    @FindByCss(".qa-AsideView-Item")
    ElementsCollection<MailElement> selectView();

    @Name("?????????????????????? ?????????? ??????????????")
    @FindByCss(".Aside__expanded--1fv5j")
    LeftPanel expandedLeftPanel();

    @Name("?????????? ?????????????????? ??????????????")
    @FindByCss(".qa-EventFormPreview")
    ViewEventPopup viewEventPopup();

    @Name("????????")
    @FindByCss(".qa-TodoDaily-Items")
    TodoItemPopup todoItemPopup();

    @Name("???????? ?? ??????????")
    @FindByCss(".qa-GridTodoGroup")
    ElementsCollection<MailElement> todoItemsList();

    @Name("???????????????? ???????????????? ??????????????")
    @FindByCss(".qa-EventPage")
    NewEvent newEventPage();

    @Name("???????????????? ???????????????????????????? ??????????????")
    @FindByCss(".qa-EventPage")
    EditEvent editEventPage();

    @Name("???????????????? ?????????????????? ???????????? ??????????????")
    @FindByCss(".qa-EventPage")
    ViewSomeoneElseEvent viewSomeoneElseEventPage();

    //Contacts suggest
    @Name("?????????????? ??????????????????")
    @FindByCss(".qa-Suggest-Items")
    MailElement suggest();

    @Name("?????????????? ????????????????")
    @FindByCss(".qa-Suggest-Item")
    ElementsCollection<ContactsSuggest> suggestItem();

    @Name("?????? n ??????????????")
    @FindByCss(".qa-GridMore")
    MailElement moreEventBtn();

    @Name("???????? ?? ?????????? ???? ?????????????? ??????????")
    @FindByCss(".qa-MonthGridDay-DayNumber")
    ElementsCollection<MailElement> dayInMonthGridBtn();

    @Name("?????????????? ???????? ?? ?????????? ???? ??????????")
    @FindByCss(".MonthGridDay__current--1kNBv")
    MailElement currentDayInMonthGrid();

    @Name("?????????????? ???????? ?? ?????????? ???? ????????????")
    @FindByCss(".WeekGridHeaderDay__wrap_current--3_33e > a")
    MailElement currentDayInWeekGridBtn();

    @Name("?????????????? ???????? ?? ?????????? ???? ????????????")
    @FindByCss(".WeekGridHeaderDay__day--opgg-")
    MailElement dayGridHeaderDay();

    @Name("???????????? ???? 15 ?????????? ???????????????? ????????????")
    @FindByCss(".qa-MonthGridDay-DayNumber[href*='21']")
    MailElement dayFifteenInMonthGridBtn();

    @Name("???????? ?? ?????????? ???? ????????????")
    @FindByCss(".WeekGridHeaderDay__day--opgg-")
    ElementsCollection<MailElement> weekGridHeaderDays();

    @Name("?????????????? ?????????????????????? ????????")
    @FindByCss(".menu__item")
    ElementsCollection<MailElement> menuItems();

    @Name("???????????? ?????????????????? ?? ???????????? ????????????????????")
    @FindByCss(".qa-LayerReference-Item")
    MailElement newCalImport();

    @Name("?????????? ?????????????? ?????? ????????????????")
    @FindByCss("[data-key*='item-0']")
    MailElement editOneEvent();

    @Name("?????????? ???????? ????????????????")
    @FindByCss("[data-key*='item-1']")
    MailElement editAllEvents();

    @Name("?????????????? ???????????????????? ??????????????????")
    @FindByCss(".qa-AddLayer")
    NewCalSidebar addCalSideBar();

    @Name("?????????????? ???????????????????????????? ??????????????????")
    @FindByCss(".qa-EditLayer")
    EditCalSidebar editCalSideBar();

    @Name("?????????????? ???????????????????? ????????????????")
    @FindByCss(".qa-AddFeed")
    NewFeedSidebar addFeedSideBar();

    @Name("???????????? ??????????????????????????????")
    @FindByCss(".qa-AddLayer-TabImport")
    MailElement importLink();

    @Name("???????????????????????????? ????????????")
    @FindByCss(".qa-WelcomeWizard")
    WelcomeWizard welcomeWizard();

    @Name("???????????? ?? ?????????? ???? ??????????")
    @FindByCss(".qa-MonthGridRow")
    ElementsCollection<MailElement> weeksInMonthGrid();

    @Name("?????????? ???? ??????????")
    @FindByCss(".MonthGrid__rows--2QyRf")
    MailElement monthGrid();

    @Name("?????????????????? ?????????? ??????????????")
    @FindByCss(".qa-EditLayer")
    Settings settings();

    @Name("???????????? ?????????????? ?????? ??????????????????")
    @FindByCss(".qa-AsideTimezone-Item")
    ElementsCollection<MailElement> timezoneItems();

    @Name("???????????? ???????????????? ????????????????????")
    @FindByCss(".qa-AsideLayers_Layers-LayerUnlock")
    MailElement unlockCalIcon();

    @Name("???????????? ???????????????? ????????????????")
    @FindByCss(".qa-AsideLayers_Subscriptions-LayerUnlock")
    MailElement unlockSubIcon();

    @Name("?????????? ???????????????? ??????????????")
    @FindByCss(".qa-EventFormDelete-Modal")
    DeleteEventPopup removeEventPopup();

    @Name("??????????")
    @FindByCss(".PSHeader")
    CalHeaderBlock calHeaderBlock();

    @Name("?????????? ?????????????????????????? ????????????????")
    @FindByCss(".qa-EventDecision-Popup")
    EventDecisionPopup eventDecisionPopup();

    @Name("???????????? ?????? ???????????? ?? ?????????????????? ???????????????? ?????? ?????????????? ?? ??????-??????")
    @FindByCss(".qa-EventDecision-MenuItemNo")
    MailElement solutionsBtnNo();

    @Name("???????????? ??????????????????, ???????????? ?? ?????????????????? ???????????????? ?????? ?????????????? ?? ??????-??????")
    @FindByCss(".qa-EventDecision-MenuItemMaybe")
    MailElement solutionsBtnMaybe();

    @Name("??????????????????")
    @FindByCss(".qa-Settings")
    GeneralSettings generalSettings();

    @Name("?????? ???????????? ?? ??????????????????")
    @FindByCss(".qa-SettingsWeekStartDay-Item")
    ElementsCollection<MailElement> weekStartsList();

    @Name("?????????????? ?????????? ?? ??????????????????")
    @FindByCss(".qa-SettingsTimezone-Item")
    ElementsCollection<MailElement> timezoneList();

    @Name("???????????? ?????? ?? ??????????????????")
    @FindByCss(".qa-SettingsDayStartHour-Item")
    ElementsCollection<MailElement> dayStartsList();

    @Name("?????????????????????? ?? ???????????????????? ??????????????????")
    @FindByCss(".notification-success")
    MailElement successNotify();

    @Name("?????????????????????? ???? ????????????")
    @FindByCss(".notification-error")
    MailElement errorNotify();

    @Name("?????????????? ?????????????????????? ??????????")
    @FindByCss(".qa-SettingsMisc-ShowTodosInGrid .checkbox__control")
    MailElement showTodos();

    @Name("?????????????? ?????????????????????? ?????????? ??????????????")
    @FindByCss(".qa-SettingsMisc-ShowWeekNumber .checkbox__control")
    MailElement showWeekNumber();

    @Name("?????????????? ?????????????????????? ??????????????????")
    @FindByCss(".qa-SettingsMisc-ShowWeekends .checkbox__control")
    MailElement showWeekends();

    @Name("?????????????? ???????????????????? ??????????")
    @FindByCss(".WeekGridTimeRuler__cell--24Iia")
    ElementsCollection<MailElement> hourCell();

    @Name("?????????????????????? ??????????????????????")
    @FindByCss(".notifications-wrapper")
    MailElement notificationMessage();

    @Name("?????????? ???????????????????? ????????????????")
    @FindByCss("[class*=EventRepetition__popup-]")
    RepeatPopup repeatPopup();

    @Name("???????????? ???????????????????? ?? ???????????? ???????????????????? ????????????????")
    @FindByCss(".qa-DatesField .Link_theme_normal")
    MailElement changeRepeatPopup();

    @Name("???????????????????? ?? ?????????????? ??????????????")
    @FindByCss(".qa-DatesField-Repetition")
    MailElement repeatInfo();

    @Name("?????????????????????? ???????? ???????? ????????")
    @FindByCss(".WeekGridAllday__expander_expanded--1fQrU")
    MailElement allDayExpandedBlock();

    @Name("?????????????????????? ????????")
    @FindByCss(".legouser__popup")
    MailElement userMenu();

    @Name("???????????? ?????????????? ?? ?????????????????????? ????????")
    @FindByCss(".legouser__menu-item_action_exit")
    MailElement logout();

    @Name("?????????????????? ???????????? ??????????")
    @FindByCss(".qa-AsideLang-Item")
    ElementsCollection<MailElement> langDropdownItem();

    @Name("???????????????? ????????????????????????")
    @FindByCss(".legouser__add-account")
    MailElement adduser();

    @Name("???????????? ???????????????????????? ??????????????????????????")
    @FindByCss(".legouser__accounts .user-account_has-subname_yes")
    ElementsCollection<MailElement> userList();

    @Name("???????????? ???????????????? ??????????")
    @FindByCss(".qa-EditLayerDelete-Button")
    MailElement deleteCal();

    @Name("?????????? ???????????????? ??????????????????")
    @FindByCss(".qa-EditLayerDelete-Modal")
    MailElement deleteCalPopup();

    @Name("?????????????? ???????????????????? ?????????????? ?? ???????????? ????????????????????")
    @FindByCss(".qa-EditLayerDelete-RelocateCheckbox .checkbox__control")
    MailElement deleteCalCheckbox();

    @Name("???????????? ?????????????????? ?? ???????????? ???????????????? ??????????????????")
    @FindByCss(".qa-EditLayerDelete-ModalButtonDelete")
    MailElement deleteCalButton();

    @Name("???????????????????????????? ?????????????? ???????????????? ??????????????????")
    @FindByCss(".qa-LayerExtraParams-IsDefault .checkbox_disabled_yes")
    MailElement disabledSetDefaultCalCheckbox();

    @Name("???????????? ?????? ???????????? ???? ???????????????????????????? ??????????????")
    @FindByCss("[class*=Confirm__content-]")
    ElementsCollection<WarningPopup> warPopups();

    @Name("?????????? ???????????????? ??????????????????")
    @FindByCss(".EditLayerDelete__modalWrap--DdqRm")
    MailElement deleteLayerPopup();

    @Name("?????????? ?? ???????????? ????????????????")
    @FindByCss(".qa-EditLayerDelete-LayerNameInput .textinput__control")
    MailElement deleteLayerPopupInput();

    @Name("???????????? ???????????????? ?????????????????? ?????? ??????????????")
    @FindByCss(".Popup2_visible .Menu-Item_type_option")
    ElementsCollection<MailElement> statusList();

    //Repeat event popup
    @Name("???????? ???????????????????? ??????")
    @FindByCss("[class*=EventRepetition__dueDateInput]")
    MailElement beforeDateField();

    @Name("???????? ?????????????????? ?????? ???????????????????? ??????")
    @FindByCss("[class*=EventRepetition__dueDateInput] .react-datepicker-popper")
    MiniCalendar beforeDateMiniCalendar();

    @Name("?????????? ???????????? ??????????")
    @FindByCss("[class*=EventResourcesFieldItem] .Menu-Item_type_option .Menu-Text")
    ElementsCollection<MailElement> officesList();

    @Name("?????????? ???????????? ?????????? ???? ???????????????? ??????????????????????")
    @FindByCss("[class*=OfficePicker__officesGroup] .menu__item_type_option")
    ElementsCollection<MailElement> officesListAtOfficePage();

    @Name("???????????????? ??????????????????????")
    @FindByCss("[class*=RoomCardInfo__wrap]")
    MailElement roomCard();

    @Name("???????????????? ?????????????????????? ?? ????????????????")
    @FindByCss("[class*=RoomCardInfo__title]")
    MailElement roomName();

    @Name("???????????? ?? ?????????????????????? ?? ????????????????")
    @FindByCss("[class*=RoomCardInfo__locationIcon]")
    MailElement roomLocation();

    @Name("???????????? ???????????????????? ?? ?????????????????????? ?? ????????????????")
    @FindByCss("[class*=RoomCardInfo__scheduleLink]")
    MailElement roomScheduleLink();

    @Name("???????????????? ??????????????????????")
    @FindByCss("[class*=Invite__wrap]")
    MeetingsPage meetingsPage();

    @Name("?????????????????? ????????????")
    @FindByCss("[class*=AsideResourcesFilter__section]")
    MailElement office();

    @Name("?????? ?????????????? ???? ???????????????? ??????????????????????")
    @FindByCss("[class*=TimelineIntervalCard__event]")
    RoomEvent roomEvent();

    @Name("?????? ?? ?????????? ???? ??????????")
    @FindByCss("[class*=MonthGridDay__wrap]")
    ElementsCollection<MailElement> daysInMonthView();

    @Name("???????????? ?????????????????? ?????????????? ?????????????????? ??????????????")
    //TODO: @FindByCss(".qa-DatesField_End-TimePicker-Item")
    @FindByCss(".Popup2_visible .Menu-Item_type_option")
    ElementsCollection<MailElement> timeEndVariants();

    @Name("?????????????????? ?????? ???????????? ??????")
    @FindByCss(".react-datepicker__month-container")
    MiniCalendar calendar();

    @Name("???????????????? ????????????????????")
    @FindByCss("[class*=SchedulePage__wrap]")
    SchedulePage schedulePage();

    @Name("???????????? ???????????????? ?????? ?????????????????? ???????????????????????? ??????????????????????")
    @FindByCss(".qa-NotificationsFieldItem_0-OffsetItem")
    ElementsCollection<MailElement> valueUntilList();

    @Name("???????????? ?????????? ???????????? ?????????????????? ?????? ???????????????? ?????????????????? ???????????????????????? ??????????????????????")
    @FindByCss(".qa-NotificationsFieldItem_1-UnitItem")
    ElementsCollection<MailElement> typeUntilList();

    @Name("???????????????? ??????????????")
    @FindByCss("[class*=TimelineIntervalCard__wrap]")
    MailElement eventsCard();

    @Name("?????????? ?????????????? ????????????????????")
    @FindByCss("[class*=EventRepetition__header] .menu__item_type_option")
    ElementsCollection<MailElement> repeatPeriodOption();

    @Name("?????????????????????? ???? ????????????")
    @FindByCss(".notifications-tr")
    MailElement notificationError();

    @Name("???????????? ???? ???????????? ?? ?????????????????????? ???? ????????????")
    @FindByCss(".notifications-tr .link")
    MailElement helpLink();

    @Name("??????????????????????")
    @FindByCss(".notification-message")
    MailElement notificationMsg();

    @Name("?????????????????? ?????????????? ?? ?????????????????????? ???? ??????????????")
    @FindByCss("[class*=EventDecision__decisionDropDownMenu]")
    DecisionEventList decisionEventList();

    @Name("???????????? ??????????")
    @FindByCss("[class*=Header__wrap]")
    CalHeaderBlock oldCalHeaderBlock();

    @Name("???????????? ?????????????? ?? ?????????????????????? ???????? ?? ???????????? ??????????")
    @FindByCss(".qa-HeaderUserPopup_signOut")
    MailElement oldLogout();

    @Name("???????????????? ???????????????????????? ?? ???????????? ??????????")
    @FindByCss(".qa-HeaderUserPopup_users_addUser")
    MailElement oldAddUser();

    @Name("?????????????? ?????????????????? ???? ??????????")
    @FindByCss(".MessageBox-Close")
    MailElement closeWidget();

    @Name("???????????????????? ???????? ????????????????????????")
    @FindByCss(".legouser__menu")
    UserMenuBlock userMenuDropdown();
}
