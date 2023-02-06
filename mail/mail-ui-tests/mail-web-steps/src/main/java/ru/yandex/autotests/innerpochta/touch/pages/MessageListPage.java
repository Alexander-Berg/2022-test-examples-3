package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.ClearFolderPopup;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.FolderPopup;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.NotFoundPage;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.Popup;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.GroupOperationsToast;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.GroupOperationsToolbarHeader;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.GroupOperationsToolbarPhone;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.MessageBlock;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.MessageListHeaderBlock;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.PinnedLettersToolbar;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.WidgetElements;
import ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks.TabsOnboarding;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface MessageListPage extends MailPage {

    @Name("Шапка в списке писем")
    @FindByCss(".topBar_messageList")
    MessageListHeaderBlock headerBlock();

    @Name("Верхний тулбар групповых операций")
    @FindByCss(".topBar_messageSelection")
    GroupOperationsToolbarHeader groupOperationsToolbarHeader();

    @Name("Тост с доп. действиями в групповых операциях")
    @FindByCss(".bottomSheet")
    GroupOperationsToast groupOperationsToast();

    @Name("Тулбар групповых операций на телефонах")
    @FindByCss(".selectionOperations")
    GroupOperationsToolbarPhone groupOperationsToolbarPhone();

    @Name("Тулбар закрепленных писем")
    @FindByCss(".messagesHead-pins")
    PinnedLettersToolbar pinnedLettersToolbar();

    @Name("Блок одного письма в списке писем")
    @FindByCss(".js-messages-message")
    ElementsCollection<MessageBlock> messages();

    @Name("Попап проставления метки на письмо/Попап действий с письмом")
    @FindByCss(".is-active.popup")
    Popup popup();

    @Name("Виджеты")
    @FindByCss(".messagesMessage-inner")
    WidgetElements widgetElements();

    @Name("Блок одного письма в списке писем")
    @FindByCss(".js-messages-message")
    MessageBlock messageBlock();

    @Name("Попап выбора папки")
    @FindByCss(".aside")
    FolderPopup folderPopup();

    @Name("Страничка 404")
    @FindByCss(".pageNotFound")
    NotFoundPage notFoundPage();

    @Name("Статуслайн с ошибкой")
    @FindByCss("[class*='NotificationToast__error--'] [class*='NotificationToast__toast-content--']")
    MailElement statusLineError();

    @Name("Статуслайн с информацией о дейтствии/событии")
    @FindByCss("[class*='NotificationToast__info--'] [class*='NotificationToast__toast-content--']")
    MailElement statusLineInfo();

    @Name("Статуслайн о новом письме")
    @FindByCss("[class*='NotificationToast__message--'] [class*='NotificationToast__toast-content--']")
    MailElement statusLineNewMsg();

    @Name("Крестик закрытия статуслайна")
    @FindByCss("[class*='NotificationToast__toast-close--']")
    MailElement statuslineClose();

    @Name("Кнопка очистки папки в «Спаме» и «Удаленных»")
    @FindByCss(".clearFolderDialog .pseudo-button")
    MailElement clearFolderButton();

    @Name("Попап очистки папки")
    @FindByCss(".confirm-popup")
    ClearFolderPopup clearFolderPopup();

    @Name("Список тем писем")
    @FindByCss(".messagesMessage-subject")
    ElementsCollection<MailElement> subjectList();

    @Name("Заглушка в пустой папке")
    @FindByCss(".ico_empty-folder")
    MailElement emptyFolderImg();

    @Name("Блок рекламы")
    @FindByCss(".direct")
    ElementsCollection<MailElement> advertisement();

    @Name("Смарт баннер")
    @FindByCss(".smartBanner-container")
    MailElement smartBanner();

    @Name("Велкамскрин")
    @FindByCss(".welcome-screen")
    MailElement welcomeScreen();

    @Name("Кнопка «Перейти в мобильную версию» на велкамскрине")
    @FindByCss(".welcome-close")
    MailElement closeWelcomeScreen();

    @Name("Крестик на смарт баннере")
    @FindByCss(".ico_close-banner")
    MailElement closeBanner();

    @Name("Страница загрузки почты")
    @FindByCss(".boot")
    MailElement bootPage();

    @Name("Область ptr")
    @FindByCss(".ptr")
    MailElement ptr();

    @Name("Промо рассылок")
    @FindByCss(".subscriptionsBubble")
    MailElement unsubscribePromo();

    @Name("Крестик закрытия промо рассылок")
    @FindByCss(".js-subscriptions-bubble-close")
    MailElement unsubscribePromoCloseBtn();

    @Name("Промо календаря")
    @FindByCss(".calendarBubble")
    MailElement calendarPromo();

    @Name("Крестик закрытия промо календаря")
    @FindByCss(".js-calendar-bubble-close")
    MailElement calendarPromoCloseBtn();

    @Name("Попап рассылок")
    @FindByCss(".subscriptionsWrapper")
    MailElement unsubscribePopup();

    @Name("Разделитель в списке писем")
    @FindByCss(".messages-timeGroup-header")
    ElementsCollection<MailElement> dateGroup();

    @Name("Выделенное письмо")
    @FindByCss(".js-messages-message .is-checked ")
    MessageBlock checkedMessageBlock();

    @Name("Список фильтров по папке")
    @FindByCss(".filterMenu-item")
    ElementsCollection<MailElement> filterList();

    @Name("Кнопка «Показать все письма» в пустом фильтре")
    @FindByCss(".messagesEmpty .messagesPlaceholder-link")
    MailElement emptyFilterLink();

    @Name("Полоска опт-ина")
    @FindByCss("[class*='OptinSubscriptionsBubble__']")
    MailElement optInLine();

    @Name("Крестик полоски опт-ина")
    @FindByCss("[class*='OptinSubscriptionsBubble__close--']")
    MailElement optInLineClose();

    //---------------------------------------------------------
    // ALL ABOUT TABS
    //---------------------------------------------------------

    @Name("Плашка о новых письмах в табе Рассылки")
    @FindByCss(".tab-news")
    MailElement newsTabNotify();

    @Name("Плашка о новых письмах в табе Рассылки: непрочитано")
    @FindByCss(".tab-news.is-unread")
    MailElement newsTabNotifyUnread();

    @Name("Плашка о новых письмах в табе Социальные сети")
    @FindByCss(".tab-social")
    MailElement socialTabNotify();

    @Name("Плашка о новых письмах в табе Социальные сети: непрочитано")
    @FindByCss(".tab-social.is-unread")
    MailElement socialTabNotifyUnread();

    @Name("Блок плашки о новых письмах в табах")
    @FindByCss(".tabNotification")
    MailElement tabNotify();

    @Name("Онбординг табов")
    @FindByCss(".onboarding")
    TabsOnboarding tabsOnboarding();

    //---------------------------------------------------------
}
