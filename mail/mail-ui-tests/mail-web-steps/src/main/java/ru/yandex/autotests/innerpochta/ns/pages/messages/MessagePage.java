package ru.yandex.autotests.innerpochta.ns.pages.messages;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.ChangeThemeBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.FooterLineBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.MobilePromoBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.NPSBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.NotificationEventBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.StatusLineBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.TabsPromoBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.TimelineBlock;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.UserMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.CollectorsNavigationBlock;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.ContextMenuCollectorsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.ContextMenuFoldersBlock;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.FoldersNavigationBlock;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.LabelsNavigationBlock;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.UnreadLabelBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.Mail360HeaderBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.NewAllServicesPopup;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.OptInLine;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.SettingsPopupBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoItemEditBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoItemsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoListSentBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoListsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.WidgetsSidebarCollapsed;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.WidgetsSidebarExpanded;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.AdInfoBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.CalendarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.DisplayedMessagesBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.EmptyFolderBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.InboxMsgInfoline;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageContextMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MsgFilterBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.Toolbar3PanelBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.ToolbarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.AttachmentsWidget;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.LabelMessageDropdownMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.LayoutSwitchDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.MoreActionsOnMessage3PaneDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.MoveMessageDropdownMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.MoveMessageDropdownMenuMiniBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.ServicesListDropdownMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.popup.MarkAsReadPopup;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.popup.MyFiltersPopup;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.popup.OptInPromo;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.popup.PromoTooltip;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks.ClearFolderPopUp;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks.MessagePageAttachmentsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks.MessagePageDoneBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks.MessagePageInboxPagerBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks.MessagePageInboxStickyPagerBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks.MessagePageNotificationBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks.MessagePageStickyToolBarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.popup.MedalMoreInfoPopup;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.NewFolderPopUp;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.NewLabelPopUp;

public interface MessagePage extends MailPage {

    @Name("Тулбар над списком писем. Кнопки для работы с письмами")
    @FindByCss(".ns-view-toolbar-box")
    ToolbarBlock toolbar();

    @Name("Неактивный тулбар над списком писем")
    @FindByCss(".ns-view-toolbar-box:not(.is-active)")
    MailElement disabledToolbar();

    @Name("Активный тулбар над списком писем")
    @FindByCss(".ns-view-toolbar-box.is-active")
    MailElement enabledToolbar();

    @Name("Дропменю «Переложить в папку»")
    @FindByCss(".ns-view-folders-actions")
    MoveMessageDropdownMenuBlock moveMessageDropdownMenu();

    @Name("Дропменю «Переложить в папку» 3pane")
    @FindByCss("body>.b-mail-dropdown__box")
    MoveMessageDropdownMenuMiniBlock moveMessageDropdownMenuMini();

    @Name("Дропменю «Поставить метку»")
    @FindByCss(".mail-FilterListPopup")
    LabelMessageDropdownMenuBlock labelsDropdownMenu();

    @Name("Дропменю «Дополнительные действия над письмом» (3pane)")
    @FindByCss("body > .nb-popup-outer_message-toolbar")
    MoreActionsOnMessage3PaneDropdown moreActionsOnMessage3PaneDropdown();

    @Name("Блок навигации по меткам")
    @FindByCss(".qa-LeftColumn-Labels")
    LabelsNavigationBlock labelsNavigation();

    @Name("Пэйджер")
    @FindByCss(".b-mail-pager__clip")
    MessagePageInboxStickyPagerBlock inboxStickyPager();

    @Name("Блок аттачей к письму")
    @FindByCss(".ns-view-attachments-widget-popup")
    MessagePageAttachmentsBlock messagePageAttachmentsBlock();

    @Name("Блок «Не нашлось ни одного письма»")
    @FindByCss(".ns-view-messages-empty")
    EmptyFolderBlock emptyFolder();

    @Name("Блок с пэйджером и кнопкой 'Ещё письма' под списком писем")
    @FindByCss(".ns-view-messages-pager-date")
    MessagePageInboxPagerBlock inboxPager();

    @Name("Пользовательские метки в меню установок меток для письма")
    @FindByCss(".b-done.b-done_promo")
    MessagePageDoneBlock messagePageDoneBlock();

    @Name("Метка Важные в композе")
    @FindByCss(".toggles-svgicon-on-active")
    MailElement importantLabel();

    @Name("Блок информации под тулбаром")
    @FindByCss(".ns-view-messages-notification-box")
    MessagePageNotificationBlock notificationBlock();

    @Name("Папки с письмами")
    @FindByCss(".qa-LeftColumn-Folders")
    FoldersNavigationBlock foldersNavigation();

    @Name("Блок сборщика писем на странице (слева, рядом с метками)")
    @FindByCss(".qa-LeftColumn-Collector")
    CollectorsNavigationBlock collectorsNavigation();

    @Name("Блок фильтра «Непрочитанные»")
    @FindByCss("a[href='#unread']")
    UnreadLabelBlock unreadLabelBlock();

    @Name("Выбранная метка «Непрочитанные»")
    @FindByCss("a[href='#unread'].is-checked")
    UnreadLabelBlock checkedUnreadLabel();

    @Name("Строка уведомления")
    @FindByCss(".mail-Statusline")
    StatusLineBlock statusLineBlock();

    @Name("Таймлайн")
    @FindByCss(".mail-Timeline")
    TimelineBlock timelineBlock();

    @Name("Несколько блоков писем на странице")
    @FindByCss(".ns-view-messages-list")
    ElementsCollection<DisplayedMessagesBlock> allDisplayedMessagesBlocks();

    @Name("Один блок писем")
    @FindByCss(".ns-view-messages-list")
    DisplayedMessagesBlock displayedMessages();

    @Name("Все аватарки не объединенные с чекбоксом")
    @FindByCss(".mail-User-Avatar[href*='search']")
    ElementsCollection<MailElement> avatarImgList();

    @Name("Тулбар для работы с письмами для 3х панельного интерфейса")
    @FindByCss(".ns-view-message .b-message-toolbar__i")
    Toolbar3PanelBlock toolbar3Panel();

    @Name("Залипший (висит вверху при скроле страницы) тулбар для работы с письмами")
    @FindByCss(".ns-view-toolbar.is-fixed .js-toolbar-content")
    MessagePageStickyToolBarBlock stickyToolBar();

    @Name("Поле результата отправки письма")
    @FindByCss(".js-title-info")
    MailElement doneTitle();

    @Name("«Перейти во Входящие» на Done")
    @FindByCss(".mail-Done-Redirect-Link")
    MailElement goToInbox();

    @Name("Заголовок на странице писем в папке писем коллектора")
    @FindByCss(".mail-MessagesSearchInfo_Summary")
    MailElement collectorFldSubj();

    @Name("Саджест")
    @FindByCss(".b-mail-suggest:not([style*='display: none'])")
    MailElement sudgest();

    @Name("Список элементов саджеста")
    @FindByCss(".b-mail-suggest:not([style*='display: none']) .b-grid-item")
    ElementsCollection<MailElement> suggestList();

    @Name("Контекстное меню")
    @FindByCss(".qa-LeftColumn-ContextMenu")
    ElementsCollection<MessageContextMenuBlock> allMenuList();

    @Name("Контекстное меню в списке писем")
    @FindByCss(".js-context-menu-items")
    ElementsCollection<MessageContextMenuBlock> allMenuListInMsgList();

    @Name("Выпадушка “Вид“ для выбора отображения почты")
    @FindByCss(".ns-view-layout-switch-popup")
    LayoutSwitchDropdown layoutSwitchDropdown();

    @Name("Попап “Очистка папки“")
    @FindByCss(".qa-LeftColumn-ConfirmPopup")
    ClearFolderPopUp clearFolderPopUp();

    @Name("Попап “Очистка папки“")
    @FindByCss(".b-popup")
    ClearFolderPopUp clearFolderPopUpOld();

    @Name("Создание новой папки")
    @FindByCss(".b-popup:not(.g-hidden)")
    NewFolderPopUp createFolderPopup();

    @Name("Окно создания новой метки")
    @FindByCss(".b-popup:not(.g-hidden)")
    NewLabelPopUp createLabelPopup();

    @Name("Кнопка «Еще письма»")
    @FindByCss(".js-message-load-more")
    MailElement loadMoreMessagesButton();

    @Name("Лоадер под списокм писем")
    @FindByCss("mail-MessagesPager-loader")
    MailElement moreMessagesLoader();

    @Name("Попап “Мои фильтры“")
    @FindByCss(".nb-popup ._nb-popup-i")
    MyFiltersPopup filtersPopup();

    @Name("Шапка Почта 360")
    @FindByCss(".mail-Header-Wrapper")
    Mail360HeaderBlock mail360HeaderBlock();

    @Name("Старый попап всех сервисов")
    @FindByCss(".mail-HeaderServicesPopup")
    NewAllServicesPopup oldAllServicesPopup();

    @Name("Попап всех сервисов Почта 360")
    @FindByCss(".PSHeader-MorePopup-content")
    NewAllServicesPopup allServices360Popup();

    @Name("Сервисы в попапе «Ещё»")
    @FindByCss(".PSHeader-MorePopup-services > .PSHeaderService")
    ElementsCollection<MailElement> moreItem();

    @Name("Блок с фильтрами в левой колонке")
    @FindByCss(".qa-LeftColumn-QuickFilters")
    MsgFilterBlock msgFiltersBlock();

    @Name("Информер о выделенных письмах")
    @FindByCss(".qa-StatuslineRoot")
    InboxMsgInfoline inboxMsgInfoline();

    @Name("Попап - “Пометить все письма в папке как прочитанные?“")
    @FindByCss(".qa-LeftColumn-ConfirmMarkRead")
    MarkAsReadPopup markAsReadPopup();

    @Name("Блок новостей в левой колонке")
    @FindByCss(".mail-News-Item")
    MailElement newsBlock();

    @Name("Список новостей в левой колонке")
    @FindByCss(".ns-view-informer-news .mail-News-Item")
    ElementsCollection<MailElement> singleNews();

    @Name("Блок ссылок в подвале инбокса")
    @FindByCss(".mail-App-Footer")
    FooterLineBlock footerLineBlock();

    @Name("Блок ссылок в подвале инбокса")
    @FindByCss(".qa-LeftColumn-Footer")
    FooterLineBlock leftPanelFooterLineBlock();

    @Name("Выпадающее меню пользователя")
    @FindByCss(".legouser__menu")
    UserMenuBlock userMenuDropdown();

    @Name("Пункты в попапе настроек")
    @FindByCss("[class*=SettingsItem__root--]")
    ElementsCollection<MailElement> settingsLink();

    @Name("Нотификация о встрече или сообщении")
    @FindByCss(".ns-view-notifications-item")
    NotificationEventBlock notificationEventBlock();

    @Name("Кнопка «Управление рассылками» в попапе настроек")
    @FindByCss(".svgicon-mail--Settings_setup-unsubscribe-filters")
    MailElement subscriptionsLink();

    @Name("Кнопка «Сбор почты с других ящиков» в попапе настроек")
    @FindByCss("[class*=SettingsItem__root][href='#setup/collectors']")
    MailElement collectorsLink();

    @Name("Выпадушка тем")
    @FindByCss(".js-themes-content")
    ChangeThemeBlock changeThemeBlock();

    @Name("Паранджа выпадушки тем")
    @FindByCss(".js-themes-overlay")
    MailElement themesOverlay();

    @Name("Крестик закрытия выпадушки тем")
    @FindByCss(".mail-ThemeOverlay-Close")
    ChangeThemeBlock closeThemeBlock();

    @Name("Компактная левая колонка")
    @FindByCss(".mail-Layout-Aside_compact")
    MailElement compactLeftPanel();

    @Name("Развернутая левая колонка")
    @FindByCss(".mail-Layout-Aside_maximum")
    MailElement maximisedLeftPanel();

    @Name("Кнопка “Сервисы“ в компактном режиме")
    @FindByCss(".ns-view-more-services")
    MailElement moreServices();

    @Name("Промо сборщиков в левой колонке")
    @FindByCss(".qa-LeftColumn-CollectorsPlaceholder")
    MailElement collectorsPromo();

    @Name("Дропдаун с кнопками меню")
    @FindByCss(".js-main-toolbar-more-menu")
    MailElement toolbarMoreMenu();

    @Name("Дропдаун выбора языка")
    @FindByCss(".mail-Footer-LangsDropdown")
    MailElement langsDropdown();

    @Name("Попап всех настроек")
    @FindByCss("[class*=SettingsButton__popup]")
    SettingsPopupBlock mainSettingsPopup();

    @Name("Попап пометки прочитанным папки")
    @FindByCss(".mail-FoldedList-Popup")
    MailElement mailFoldedListPopup();

    @Name("Кнопка «Создать папку»")
    @FindByCss(".qa-LeftColumn-AddFolderButton")
    MailElement createFolderBtn();

    @Name("Кнопка «Создать метку»")
    @FindByCss(".qa-LeftColumn-LabelsPlaceholder")
    MailElement createLabelBtn();

    @Name("Кнопка - плюс «Создать метку»")
    @FindByCss(".qa-LeftColumn-LabelAdd")
    MailElement createLabelPlusBtn();

    @Name("Контекстное меню сборщика")
    @FindByCss(".qa-LeftColumn-CollectorContextMenu")
    ContextMenuCollectorsBlock contextMenuCollectors();

    @Name("Контекстное меню папки")
    @FindByCss(".qa-LeftColumn-FolderContextMenu")
    ContextMenuFoldersBlock contextMenuFolder();

    @Name("Кнопка Очистить у папки")
    @FindByCss(".qa-LeftColumn-ClearControl")
    MailElement clearTrashButton();

    @Name("Шестеренка у создания папки")
    @FindByCss(".mail-FolderList-Setup_link[href*='#setup/folders']")
    MailElement createFolderGear();

    @Name("Шестеренка у создания метки")
    @FindByCss(".mail-LabelList-Setup_link[href*='#setup/folders']")
    MailElement createLabelGear();

    @Name("Кнопка «Ставить метку автоматически»")
    @FindByCss("[class*=messages-empty__button]")
    MailElement putMarkAutomaticallyButton();

    @Name("Кнопка “Добавить дело“ внизу страницы")
    @FindByCss(".js-todo-placeholder-wrap")
    MailElement toDoWindow();

    @Name("Развернутый блок Тудушки со списками дел")
    @FindByCss(".ns-view-todo-lists-box")
    TodoListsBlock todoListBlock();

    @Name("Развернутый блок Тудушки со списками дел")
    @FindByCss(".ns-view-todo-items-box")
    TodoItemsBlock todoItemsBlock();

    @Name("Блок редактирования Дела")
    @FindByCss(".ns-view-todo-item-edit-box")
    TodoItemEditBlock todoItemEditBlock();

    @Name("Блок отправки Списка дел по email")
    @FindByCss(".ns-view-todo-email-box")
    TodoListSentBlock todoListSentBlock();

    @Name("Линк «Показать все...»")
    @FindByCss(".js-hidden-labels-toggler")
    MailElement showLabels();

    @Name("Ссылка «Настроить» папки")
    @FindByCss(".mail-NestedList-Setup-Settings[href*='#setup/folders']")
    MailElement settingsFoldersLink();

    @Name("Ссылка «Настроить» сборщик")
    @FindByCss(".mail-NestedList-Setup-Settings[href*='#setup/collectors']")
    MailElement settingsCollectorsLink();

    @Name("Кнопка «Добавить ящик»")
    @FindByCss(".qa-LeftColumn-CollectorAdd")
    MailElement addCollectorBtn();

    @Name("Попап Карты")
    @FindByCss(".mail-Message-Map_mail-message-map-outer-node")
    MailElement mapPopup();

    @Name("Тултип события таймлайна")
    @FindByCss(".mail-Timeline-Events-Tooltip")
    MailElement timelineEventTooltip();

    @Name("Попап промо пушей")
    @FindByCss(".js-push-notifications-promo")
    MailElement pushPromo();

    @Name("Промо табов")
    @FindByCss(".js-promo-tabs")
    TabsPromoBlock tabsPromo();

    @Name("Промо мобильных")
    @FindByCss(".js-promo-mobile-app")
    MobilePromoBlock mobilePromo();

    @Name("Фильтры по типам в 3pane")
    @FindByCss(".js-mail-Toolbar-Filter-item")
    ElementsCollection<MailElement> quickFiltersList();

    @Name("Плашка NPS «Оцени нас»")
    @FindByCss(".promo-nps")
    NPSBlock npsBlock();

    @Name("Промо удаления фильтров отписок")
    @FindByCss(".popup2_theme_mail-promo")
    MailElement deleteUnsubscribeFilterPopup();

    @Name("Строка поиска под списком писем")
    @FindByCss(".js-search-input")
    MailElement searchInput();

    @Name("Кнопка «Найти» под списком писем")
    @FindByCss(".js-search-input-button")
    MailElement searchBtn();

    @Name("Надпись «Вы просмотрели все письма»")
    @FindByCss(".b-mail-pager__navigation-label")
    MailElement allMessagesLabel();

    @Name("Попап с аттачами")
    @FindByCss(".ns-view-attachments-widget-popup")
    AttachmentsWidget attachmentsPopup();

    @Name("Попап сохранения файла на диск")
    @FindByCss(".ns-view-disk-widget-save-popup")
    MedalMoreInfoPopup saveToDiskPopup();

    @Name("Реклама в промо на Done")
    @FindByCss("#js-direct-done")
    MailElement directDone();

    @Name("Ссылка на страницу директа в полоске рекламы")
    @FindByCss("#js-messages-direct a[href='https://direct.yandex.ru/?partner']")
    MailElement directLineLink();

    @Name("Реклама над списком писем")
    @FindByCss("#js-messages-direct")
    MailElement directLine();

    @Name("Новая рекламная полоска над списком писем")
    @FindByCss(".mail-DirectLine")
    MailElement newDirectLine();

    @Name("Реклама в левой колонке")
    @FindByCss("#js-main-rtb")
    MailElement directLeft();

    @Name("Реклама над списком писем под блокировщиком")
    @FindByCss(".js-mail-layout-content > div:nth-child(2) > div > div > div > div:nth-child(2) > div > div")
    MailElement directLineCryprox();

    @Name("Реклама в левой колонке под блокировщиком")
    @FindByCss(".ns-view-left-box > div:nth-child(8) > div > div > div > div:nth-child(2)")
    MailElement directLeftCryprox();

    @Name("Реклама в левой колонке под блокировщиком на странице контактов")
    @FindByCss(".ns-view-left-box > div:nth-child(2) > div > div > div > div:nth-child(2)")
    MailElement directLeftCryproxContacts();

    @Name("Информация о блоке рекламы в левой колонке под блокировщиком")
    @FindByCss(".ns-view-left-box .ad-debug-info")
    AdInfoBlock directLeftInfoCryprox();

    @Name("Информация о блоке рекламы над просмотром письма под блокировщиком")
    @FindByCss(".js-mail-layout-content .ad-debug-info")
    AdInfoBlock directLineInfoCryprox();

    @Name("Информация о блоке рекламы над списком писем")
    @FindByCss(".mail-DirectLineContainer .ad-debug-info")
    AdInfoBlock directLineInfo();

    @Name("Информация о блоке рекламы в левой колонке")
    @FindByCss(".b-banner .ad-debug-info")
    AdInfoBlock directLeftInfo();

    @Name("Выпадушка «Вложить в другую папку»")
    @FindByCss(".b-folders_dropdown")
    ElementsCollection<MailElement> selectFolderDropDown();

    @Name("Промо Я.Плюс")
    @FindByCss(".mail-yaplus__tooltip")
    MailElement yaPlusPromo();

    @Name("Кнопка «Написать»")
    @FindByCss(".qa-LeftColumn-ComposeButton")
    MailElement composeButton();

    @Name("Обновить")
    @FindByCss(".qa-LeftColumn-SyncButton")
    MailElement checkMailButton();

    @Name("Имя текущего пользователя")
    @FindByCss(".mail-User-Name")
    MailElement userName();

    @Name("Блок кнопок отправки письма")
    @FindByCss(".mail-Compose-Field-Actions")
    MailElement msgActionButtons();

    @Name("Выпадушка сервисов")
    @FindByCss("[class*=ServicesList__root]")
    ServicesListDropdownMenuBlock servicesPopup();

    @Name("Ссылки в меню сервисы")
    @FindByCss("[class*=ServicesList__item]")
    ElementsCollection<MailElement> services();

    @Name("Список элементов видимого выпадающего списка")
    @FindByCss("._nb-select-item")
    ElementsCollection<MailElement> selectItem();

    @Name("Таб «Входящие»")
    @FindByCss(".qa-LeftColumn-Folder > a[href='#tabs/relevant']")
    MailElement inboxTab();

    @Name("Таб «Рассылки»")
    @FindByCss(".qa-LeftColumn-Folder > a[href='#tabs/news']")
    MailElement newsTab();

    @Name("Таб «Социальные сети»")
    @FindByCss(".qa-LeftColumn-Folder > a[href='#tabs/social']")
    MailElement socialTab();

    @Name("Таб «С Вложениями»")
    @FindByCss(".qa-LeftColumn-Folder > a[href='#attachments']")
    MailElement attachmentsTab();

    @Name("Таб «Главное»")
    @FindByCss(".qa-LeftColumn-Folder > a[href*='#label/']")
    MailElement priorityTab();

    @Name("Выбрать все письма")
    @FindByCss("[class*=MultipleEmailsSelected-m__action]")
    MailElement selectAllMessagesInFolder();

    @Name("Саджест городов в погодной теме")
    @FindByCss(".mail-Region-Autocompleter-List-Item")
    ElementsCollection<MailElement> suggestCitiesList();

    @Name("Прыщ непрочитанности у таба")
    @FindByCss(".qa-LeftColumn-UnreadControl")
    MailElement markReadTabIcon();

    @Name("Кнопка «Да, пометить прочитанным» в попапе пометки прочитанными писем в папке")
    @FindByCss(".js-apply-mark-read")
    MailElement markReadAccept();

    @Name("Плашка «Выбрать все письма»")
    @FindByCss(".qa-StatuslineRoot")
    MailElement selectAllMessagesPopup();

    @Name("Таб входящие «только новые»")
    @FindByCss("[href='#tabs/relevant?extra_cond=only_new']")
    MailElement inboxTabOnlyNew();

    @Name("Промо подключенного домена")
    @FindByCss(".BeautifulEmailConnectedPromo")
    MailElement domainPromo();

    @Name("Заголовок в попапе отключения домена")
    @FindByCss(".Modal__title--2C2rW")
    MailElement domainDisablePopupHeader();

    @Name("Кнопка «Отменить» в попапе отключения домена")
    @FindByCss(".Modal-Content .Button2_view_default")
    MailElement domainDisablePopupCancelButton();

    @Name("Промка в ЛК")
    @FindByCss(".ns-view-mail-pro-left-column-widget")
    MailElement promo360LC();

    @Name("Свернутый сайдбар виджетов")
    @FindByCss(".PSSidebarContainer")
    WidgetsSidebarCollapsed widgetsSidebarCollapsed();

    @Name("Развернутый сайдбар виджетов")
    @FindByCss(".PSSidebarContainer")
    WidgetsSidebarExpanded widgetsSidebarExpanded();

    @Name("Промо попап опт-ина ")
    @FindByCss("[class*='PromoOptInSubscriptions__modalWrapper--']")
    OptInPromo optInPromo();

    @Name("Промо-тултип опт-ина")
    @FindByCss(".regular-promo-tooltip")
    PromoTooltip promoTooltip();

    @Name("Полоска опт-ина")
    @FindByCss(".ns-view-opt-in-subs-view")
    OptInLine optInLine();

    @Name("Новый попап всех настроек")
    @FindByCss("[class*='NewSettings__popup--']")
    SettingsPopupBlock mainSettingsPopupNew();

    @Name("Пункты меню в выпадушке «Напомнить позже»")
    @FindByCss("[class*='ReplyLaterMenu__root--'] li")
    ElementsCollection<MailElement> replyLaterDropDown();

    @Name("Календарь для отложенной отправки")
    @FindByCss("[class*='DateTimePicker__root']")
    CalendarBlock calendar();

    @Name("Левая кнопка подтверждения действия в статуслайне")
    @FindByCss(".mail-Statusline .button2_theme_clear")
    CalendarBlock leftSubmitActionBtn();

    @Name("Правая кнопка подтверждения действия в статуслайне")
    @FindByCss(".mail-Statusline .button2_theme_action")
    CalendarBlock rightSubmitActionBtn();
}
