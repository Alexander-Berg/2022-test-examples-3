package ru.yandex.autotests.innerpochta.cal.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.ConfirmPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.EditEventPopups;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.EventBigDialogPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.EventDialogPopup;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.LeftSideBar;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.TouchEventPage;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.TouchRoomCard;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.TouchScheduleViewPage;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.TouchTodo;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface TouchHomePage extends MailPage {

    @Name("Бургер")
    @FindByCss("[class*=TouchHeader__control]:first-child")
    MailElement burger();

    @Name("Сетка событий")
    @FindByCss("[class*=qa-TouchGrid]")
    MailElement grid();

    @Name("Полоски времени")
    @FindByCss("[class*=TouchGridDay__row]")
    ElementsCollection<MailElement> gridRows();

    @Name("Iframe отправки репорта в корпе")
    @FindByCss("[class*=TouchBugReporter__iframe]")
    MailElement reportIframe();

    @Name("Лоадер")
    @FindByCss(".spin2")
    MailElement loader();

    @Name("Левый сайдбар")
    @FindByCss("[class*=TouchAside__panel]")
    LeftSideBar sidebar();

    @Name("Дата в шапке")
    @FindByCss("[class*=TouchGridHeader__calendarToggler]")
    MailElement headerDate();

    @Name("События в сетке на экране")
    @FindByCss("[class*=TouchGrid__slide][style*='translate(0px, 0px)'] .qa-TouchGridEvent")
    ElementsCollection<MailElement> events();

    @Name("Экран события")
    @FindByCss("[class*=TouchEventPage__wrap]")
    TouchEventPage eventPage();

    @Name("Экран раписания")
    @FindByCss("[class*=TouchScheduleRange__list]")
    TouchScheduleViewPage shedulePage();

    @Name("События в расписании")
    @FindByCss("[class*=TouchScheduleEvent__wrap]")
    ElementsCollection<MailElement> eventsShedule();

    @Name("Попап отмены создания события")
    @FindByCss(".qa-EventCreatingCancelDialog")
    EventDialogPopup cancelCreatePopup();

    @Name("Попап отмены редактирования")
    @FindByCss(".qa-TouchEventEditingCancelDialog")
    EventDialogPopup cancelEditPopup();

    @Name("Попап выбора событий для редактирования")
    @FindByCss(".qa-TouchEventEditingModeDialog")
    EventDialogPopup eventsPeriodicityPopup();

    @Name("Попап отправления письма об изменении")
    @FindByCss(".qa-EventMailToAllDialog")
    EventDialogPopup mailToAllParticipantsPopup();

    @Name("Попап сохранения изменения участников")
    @FindByCss(".qa-TouchMembersPicker_unsavedConfirmationDialog")
    EventDialogPopup saveEditParticipantsPopup();

    @Name("Попап удаления события")
    @FindByCss(".qa-TouchEventDeleteDialog")
    EventDialogPopup deleteEvenPopup();

    @Name("Попап удаления одного или серии")
    @FindByCss(".qa-TouchEventDeleteDialog")
    EventDialogPopup deleteOneOrAllPopup();

    @Name("Попап подтверждения отказа от события")
    @FindByCss("[class*=TouchEventDecision__modalFooter]")
    EventDialogPopup simpleEventDecisionPopup();

    @Name("Попап подтверждения отказа от регулярного события")
    @FindByCss("[class*=TouchModal__wrap].modal_visible_yes")
    EventBigDialogPopup regularEventDecisionPopup();

    @Name("Кнопка добавления нового события")
    @FindByCss("[class*=TouchGridNewEvent]")
    MailElement addEventButton();

    @Name("Кнопка добавления нового события на странице расписания")
    @FindByCss("[class*=cheduleEmptyState__createButton]")
    MailElement addEventButtonShedule();

    @Name("Экран редактирования места")
    @FindByCss(".qa-TouchLocationPicker")
    EditEventPopups editPlacePage();

    @Name("Экран редактирования участников")
    @FindByCss(".qa-TouchMembersPicker")
    EditEventPopups editParticipantsPage();

    @Name("Экран изменения слоя")
    @FindByCss(".qa-TouchLayerPicker")
    EditEventPopups editLayerPage();

    @Name("Экран изменения занятости")
    @FindByCss(".qa-TouchAvailabilityPicker")
    EditEventPopups editAvailabilityPage();

    @Name("Экран изменения повтора события")
    @FindByCss("[class*=qa-TouchRepetitionSettings_presets]")
    EditEventPopups editRepetitionPage();

    @Name("Экран настройки повтора события")
    @FindByCss("[class*=qa-TouchRepetitionSettings_customSettings]")
    EditEventPopups customSettingsRepetitionPage();

    @Name("Экран настройки типа повторения")
    @FindByCss("[class*=qa-TouchRepetitionSettings_repetitionType]")
    EditEventPopups repetitionTypePage();

    @Name("Экран изменения переговорки")
    @FindByCss(".modal_visible_yes [class*=TouchResourcesPicker]")
    EditEventPopups editResourcesPage();

    @Name("Экран выбора офиса")
    @FindByCss(".modal_visible_yes [class*=qa-TouchResourcesPicker_officePicker]")
    EditEventPopups editOfficePage();

    @Name("Экран изменения организатора")
    @FindByCss("[class*=qa-TouchOrganizerPicker]")
    EditEventPopups editOrganizerPage();

    @Name("Блок событий на весь день")
    @FindByCss("[class*=TouchGridAllday__wrap]")
    MailElement allDayEventsBlock();

    @Name("«Ещё n» блока событий на весь день")
    @FindByCss("[class*=TouchGridAllday__more]")
    MailElement allDayEventsMoreButton();

    @Name("Стрелочка разворачивания/сворачивания всех событий блока событий на весь день")
    @FindByCss("[class*=TouchGridAllday__expandArrow]")
    MailElement allDayEventsMoreArrowControl();

    @Name("Кнопка «Сегодня»")
    @FindByCss("[class*=TouchFloatingButton__visible]")
    MailElement todayBtn();

    @Name("Полоска текущено времени")
    @FindByCss("[class*=TouchGridCurrentTime__wrap]")
    MailElement nowLine();

    @Name("Нотификация с ошибкой")
    @FindByCss(".notification-error")
    MailElement errorNotify();

    @Name("Нотификация о сохранении изменений")
    @FindByCss(".notification-success")
    MailElement successNotify();

    @Name("Крестик нотификации")
    @FindByCss(".notification-dismiss")
    MailElement notificationDismiss();

    @Name("Список дел")
    @FindByCss("[class*=TouchTodo__wrap]")
    TouchTodo todo();

    @Name("Карточка переговорки")
    @FindByCss(".modal_visible_yes [class*=TouchRoomCard__wrap]")
    TouchRoomCard roomCard();

    @Name("Попап подтверждения ограниченной брони переговорки")
    @FindByCss("[class*=qa-EventRepetitionLimitDialog]")
    ConfirmPopup confirmSavePopup();

    @Name("Уведомление")
    @FindByCss(".notification-message")
    MailElement notificationMsg();
}
