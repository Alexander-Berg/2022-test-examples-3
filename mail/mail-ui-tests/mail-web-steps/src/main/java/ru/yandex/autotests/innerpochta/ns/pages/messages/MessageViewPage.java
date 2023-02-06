package ru.yandex.autotests.innerpochta.ns.pages.messages;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.popup.AddContactPopup;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.popup.ContactPopup;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.AttachedMessageBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageToolbarContentBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.ToolbarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.AttachmentsWidget;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.LabelMessageDropdownMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.MoveMessageDropdownMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.MessageTextBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.MessageTranslateNotificationBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.MessageViewHeadBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.MessageMiscFieldBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.MessageSubjectBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.ReplyNotificationBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.ToolbarMoreDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.dropdown.MessageThreadCommonToolbar;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.popup.ContactBlockPopup;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.popup.MedalMoreInfoPopup;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.quickreply.QuickReplyBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.rightpannel.MessageViewSidebar;

public interface MessageViewPage extends MailPage {

    @Name("Тулбар над письмом. Кнопки для работы с письмами")
    @FindByCss(".ns-view-toolbar-box")
    ToolbarBlock toolbar();

    @Name("Тема письма в просмотре письма в списке писем с метками важное и прочитанное")
    @FindByCss(".js-mail-Message-Toolbar")
    MessageSubjectBlock messageSubject();

    @Name("Тема письма в просмотре письма на отдельной странице")
    @FindByCss(".qa-MessageViewer-Title")
    MailElement messageSubjectInFullView();

    @Name("Метки письма")
    @FindByCss(".qa-ComposeLabel")
    ElementsCollection<MailElement> messageLabel();

    @Name("Блок с информацией от кого и кому")
    @FindByCss(".qa-MessageViewer-Header-wrapper")
    MessageViewHeadBlock messageHead();

    @Name("Плашка перевода письма")
    @FindByCss(".qa-MessageViewer-TranslateMessageBar")
    MessageTranslateNotificationBlock translateNotification();

    @Name("Крестик закрытия письма")
    @FindByCss(".js-message-close-button")
    MailElement closeMsgBtn();

    @Name("Кнопка сворачивания/разворачивания правой колонки в широком письме")
    @FindByCss(".qa-MessageViewer-FloatingColumn-Button")
    MailElement messageViewExpandSideBar();

    @Name("Правая колонка в полноэкранном просмотре письма")
    @FindByCss(".qa-MessageViewer-RightColumn:not([class*='MessageViewerLayout__compact--'])")
    MessageViewSidebar messageViewSideBar();

    @Name("Полный блок с текстом и цитированием")
    @FindByCss(".js-message-body")
    MessageTextBlock messageTextBlock();

    @Name("Виджет с аттачами")
    @FindByCss(".qa-MessageViewer-Attachments-wrapper")
    AttachmentsWidget attachments();

    @Name("Тултип аттача")
    @FindByCss(".Tooltip-Content")
    MailElement attachmentToolTip();

    @Name("Кнопка «Быстрый ответ всем участникам переписки»")
    @FindByCss(".ns-view-thread-quick-reply-placeholder")
    MailElement qrInCompactViewBtn();

    @Name("Кнопка «Ответить» в QR")
    @FindByCss(".js-quick-reply-placeholder-single-reply")
    MailElement qrInFullViewReplyBtn();

    @Name("Кнопка «Быстрый ответ всем участникам переписки»")
    @FindByCss(".qa-QuickReplyPlaceholder")
    MailElement quickReplyPlaceholder();

    @Name("Развёрнутый QR в просмотре треда из инбокса")
    @FindByCss(".qa-QuickReply")
    QuickReplyBlock quickReply();

    @Name("Кнопка включить скрытые ссылки и картинки")
    @FindByCss(".qa-MessageViewer-SpamMessageBar .qa-MessageViewer-Widget-Button")
    MailElement showHiddenPicturesButton();

    @Name("Плашка «Почему в спаме»")
    @FindByCss(".qa-MessageViewer-SpamReasonBar")
    MailElement spamReasonBar();

    @Name("Крестик в плашке «Почему в спаме»")
    @FindByCss(".qa-MessageViewer-SpamReasonBar .qa-MessageViewer-Widget-Close")
    MailElement spamReasonBarCross();

    @Name("Просмотрщик изображения")
    @FindByCss(".b-image-viewer__container")
    MailElement imageViewer();

    @Name("Кнопка «Скачать» в просмотрщике изображения")
    @FindByCss(".b-image-viewer__download")
    MailElement imageDownload();

    @Name("Лоадер изображения в просмотрщике")
    @FindByCss(".b-image-viewer__loader")
    MailElement imageViewerLoader();

    @Name("Плашка спама «Ссылки и картинки отключены»")
    @FindByCss(".qa-MessageViewer-SpamMessageBar")
    MailElement spamLinksNotification();

    @Name("Плашка спама «Письмо может быть опасно»")
    @FindByCss(".qa-MessageViewer-Widget")
    MailElement dangerNotification();

    @Name("Скрытая картинка")
    @FindByCss("[data-spam-item-id]")
    MailElement hiddenPicture();

    @Name("Отрисованная картинка")
    @FindByCss(".ns-view-message-body img:not([data-spam-item-id])")
    ElementsCollection<MailElement> shownPictures();

    @Name("Тулбар в шапке под блоком с информацией об отправителе и получателе")
    @FindByCss(".qa-MessageViewer-Toolbar-Container")
    MessageToolbarContentBlock contentToolbarBlock();

    @Name("Дропдаун с дополнительными действиями: создать правило, свойства письма")
    @FindByCss(".ns-view-message-toolbar-popup")
    MessageMiscFieldBlock miscField();

    @Name("Выпадушка с метками")
    @FindByCss(".mail-FilterListPopup")
    LabelMessageDropdownMenuBlock labelsDropdownMenu();

    @Name("Выпадушка - «Переложить в папку»")
    @FindByCss(".ns-view-folders-actions")
    MoveMessageDropdownMenuBlock moveMessageDropdownMenu();

    @Name("Выпадушка из Yabble")
    @FindByCss(".mail-ContactMenu")
    ContactBlockPopup contactBlockPopup();

    @Name("Попап существующего контакта")
    @FindByCss(".js-abook-person-popup")
    ContactPopup mailCard();

    @Name("Попап добавления нового контакта")
    @FindByCss(".js-abook-person-popup")
    AddContactPopup editMailCard();

    @Name("Кнопка «развернуть n непрочитанных»")
    @FindByCss(".js-message-thread-pager-expandUnread")
    MailElement expandUnreadBtn();

    @Name("Раскрытые сообщения в треде")
    @FindByCss(".ns-view-message-thread-item-wrap.is-opened")
    ElementsCollection<MailElement> expandMsgInThread();

    @Name("Выпадушка кнопки «Еще» в тулбаре")
    @FindByCss(".ui-dialog .js-main-toolbar-more-menu")
    ToolbarMoreDropdown moreBlock();

    @Name("Сообщение «Письмо успешно отправлено»")
    @FindByCss(".mail-StatuslineProgress_MessageSent")
    MailElement doneQrMessage();

    @Name("Пустое окно просмотра в 3pane")
    @FindByCss(".mail-Message-Empty-Header")
    MailElement emptyMsgView3pane();

    @Name("Якорь для скролла к аттачам")
    @FindByCss(".js-attachments-scroll")
    MailElement attachScrollLink();

    @Name("Развернутая тема письма")
    @FindByCss(".with-subject-hovered")
    MailElement quickMessageViewSubjectFull();

    @Name("Сообщения в треде")
    @FindByCss(".js-message-toggle")
    ElementsCollection<MailElement> msgInThread();

    @Name("Общий тулбар треда")
    @FindByCss(".ns-view-message-thread-toolbar-popup")
    MessageThreadCommonToolbar commonToolbar();

    @Name("Кнопка открыть список писем")
    @FindByCss(".js-message-thread-pager-loadMore")
    MailElement loadMore();

    @Name("Ссылка «Вы уже ответили на это письмо. Посмотреть»")
    @FindByCss(".js-go-to-replied-message")
    MailElement goToReplyLink();

    @Name("Попап информации о надежности отправителя")
    @FindByCss("[class*=DKIMPopup__popup]")
    MedalMoreInfoPopup medalPopup();

    @Name("Попап сохранения файла на диск")
    @FindByCss(".ns-view-disk-widget-save-popup")
    MedalMoreInfoPopup saveToDiskPopup();

    @Name("Стрелка «предыдущее сообщение» при просмотре в списке писем")
    @FindByCss(".js-open-prev-thread")
    MailElement prevMessageCompact();

    @Name("Стрелка «следующее сообщение» при просмотре в списке писем")
    @FindByCss(".js-open-next-thread")
    MailElement nextMessageCompact();

    @Name("Все аватарки на странице")
    @FindByCss(":not(a)>.mail-User-Avatar")
    ElementsCollection<MailElement> allAvatars();

    @Name("Все аватарки на странице просмотра письма")
    @FindByCss(".mail-Avatar")
    ElementsCollection<MailElement> allAvatarsMessageView();

    @Name("Попап вложенного письма")
    @FindByCss("[class*=EMLViewer__root] .Modal-Content")
    AttachedMessageBlock attachedMessagePopup();

    @Name("Попап «Больше не спрашивать» о ...")
    @FindByCss(".b-popup:not(.g-hidden)")
    ReplyNotificationBlock replyNotification();

    @Name("Нотификация об ответе не всем участника")
    @FindByCss(".ns-view-compose-field-to-notice-replyall")
    MailElement notificationAboutReply();

    @Name("Просмотр письма")
    @FindByCss(".qa-MessageViewer")
    MailElement messageViewer();

    @Name("Кнопка «Удалить напоминание» на плашке")
    @FindByCss(".qa-MessageViewer-Widget-Button")
    MailElement deleteReminderBtn();

    @Name("Пункты меню в выпадушке «Напомнить позже»")
    @FindByCss("[class*='ReplyLaterMenu__root--'] li")
    ElementsCollection<MailElement> replyLaterDropDown();

    @Name("Промо «Напомнить позже»")
    @FindByCss("[class*='PromoReplyLater__root--']")
    MailElement replyLaterPromo();

    @Name("Крестик на промо «Напомнить позже»")
    @FindByCss("[class*='PromoReplyLater__root--'] button")
    MailElement replyLaterPromoClose();
}
