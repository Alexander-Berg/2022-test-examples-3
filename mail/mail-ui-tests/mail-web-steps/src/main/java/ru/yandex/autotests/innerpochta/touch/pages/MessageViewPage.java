package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.CopyYabblePopup;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.Popup;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.SaveToDisk;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.Viewer;
import ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks.AttachmentsBlock;
import ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks.GroupOperationsToolbarTablet;
import ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks.Header;
import ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks.QuickReply;
import ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks.Toolbar;
import ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks.Translator;
import ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks.MsgInThread;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */

public interface MessageViewPage extends MailPage {

    @Name("Хедер в просмотре писем")
    @FindByCss(".topBar_thread")
    Header header();

    @Name("Вьюер")
    @FindByCss(".lightbox")
    Viewer viewer();

    @Name("Тулбар груповых операций с письмами на планшетах")
    @FindByCss(".ns-view-right-column-box .selectionOperations")
    GroupOperationsToolbarTablet groupOperationsToolbarTablet();

    @Name("Тулбар в просмотре письма")
    @FindByCss(".messageHead")
    Toolbar toolbar();

    @Name("Квикреплай в просмотре письма")
    @FindByCss(".quickReplySection")
    QuickReply quickReply();

    @Name("Блок с аттачами в просмотре письма")
    @FindByCss(".messageBody .messageAttachments")
    AttachmentsBlock attachmentsBlock();

    @Name("Попап сохранения на диск")
    @FindByCss(".is-active.is-fullsize.popup")
    SaveToDisk saveToDisk();

    @Name("Переводчик")
    @FindByCss("[class*='translator__translator--']")
    Translator translator();

    @Name("Письма в просмотре треда")
    @FindByCss(".message-box")
    ElementsCollection<MsgInThread> msgInThread();

    @Name("Статуслайн с ошибкой")
    @FindByCss("[class*='NotificationToast__error--'] [class*='NotificationToast__toast-content--']")
    MailElement statusLineError();

    @Name("Статуслайн с информацией о дейтствии/событии")
    @FindByCss("[class*='NotificationToast__info--'] [class*='NotificationToast__toast-content--']")
    MailElement statusLineInfo();

    @Name("Правая колонка")
    @FindByCss(".is-active.overlay")
    MailElement rightColumnBox();

    @Name("Заголовок всего треда")
    @FindByCss(".messageDescription")
    MailElement threadHeader();

    @Name("Кнопки в выпадающем меню")
    @FindByCss(".popup-item")
    ElementsCollection<MailElement> btnsList();

    @Name("Ябблы отправителей и получателей")
    @FindByCss(".yabble_threadMessage")
    ElementsCollection<MailElement> yabbles();

    @Name("Крестик для снятия метки")
    @FindByCss(".label-remove")
    MailElement unmarkLabelBtn();

    @Name("Показать цитату")
    @FindByCss(".quote-switcher")
    MailElement showQuoteLink();

    @Name("Аттачи в просмотре письма")
    @FindByCss(".js-attachments .js-attachment-wrapper")
    ElementsCollection<MailElement> attachments();

    @Name("Адрес в письме")
    @FindByCss(".ymaps-link")
    MailElement ymapsLink();

    @Name("Кнопка «Ещё» внизу письма")
    @FindByCss(".messageBody-toolbar .messageHead-toolbarItem-more")
    MailElement moreBtnLow();

    @Name("Кнопка включения отображения картинок и ссылок в спаме")
    @FindByCss(".messageSpamWarning-button")
    MailElement turnOnInSpam();

    @Name("Лоадер во время загрузки сообщения(ий) в просмотр письма")
    @FindByCss(".thread-loadMore .ico_loader")
    MailElement msgLoaderInView();

    @Name("Ссылка в письме")
    @FindByCss(".goto-anchor")
    MailElement linkInMessage();

    @Name("Картинка в письме")
    @FindByCss(".messageBody-content img")
    MailElement ImgInMessage();

    @Name("Счётчик треда")
    @FindByCss(".messageDescription-threadCounter")
    MailElement threadCounter();

    @Name("Адрес электорнной почты в письме")
    @FindByCss(".goto-anchor")
    MailElement mailAddressInMail();

    @Name("Прыщик непрочитанности на письме в просмотре")
    @FindByCss(".messageHead-unreadFlag-inner")
    MailElement recentToggler();

    @Name("Кнопка «Ещё»")
    @FindByCss(".messageHead-toolbar .Tappable-inactive.messageHead-toolbarItem.messageHead-toolbarItem-more")
    MailElement moreBtn();

    @Name("Попап копирования ябблов")
    @FindByCss(".popup-yabble")
    CopyYabblePopup yabblePopup();

    @Name("Плашка классификации")
    @FindByCss(".lNbI0CXh-Ew9-ys17s02d._3cghfRHH0-iQS06lzuEvSu")
    MailElement classDialog();

    @Name("Кнопка Да на плашке классификации")
    @FindByCss("._18n7Y_sSO2KPpYIjyfdpQj")
    MailElement classDlgYesBtn();

    @Name("Кнопка Нет на плашке классификации")
    @FindByCss(".exY2XxzKqaQGzKwXMiaos:not(._18n7Y_sSO2KPpYIjyfdpQj)")
    MailElement classDlgNoBtn();

    @Name("Детали письма")
    @FindByCss(".messageBody .messageDetails")
    MailElement msgDetails();

    @Name("Попапы")
    @FindByCss(".is-active.popup")
    Popup popup();

    @Name("Аватарка отправителя в тулбаре письма")
    @FindByCss(".messageHead-avatar")
    MailElement avatarToolbar();

    @Name("Паранжа под попапом")
    @FindByCss(".popup-overlay")
    MailElement overlay();

    @Name("Паранжа при отправке сообщения")
    @FindByCss(".quickReplyOverlay")
    MailElement quickReplyOverlay();

    @Name("Тело письма")
    @FindByCss(".messageBody-content")
    MailElement msgBody();

    @Name("Поля в деталях письма")
    @FindByCss(".messageDetails-line")
    ElementsCollection<MailElement> msgDetailsFields();

    @Name("Скрытая картинка в спамном письме")
    @FindByCss(".ya-hidden-link img")
    MailElement hiddenImg();

    @Name("Черновик в просмотре")
    @FindByCss(".message-box .is-draft:not(.is-collapsed)")
    MailElement draft();

    @Name("Адрес/имя контакта")
    @FindByCss("[class*='yabble__yabble-text--']")
    MailElement yabbleText();

    @Name("Попап «Скрыть переводчик»")
    @FindByCss(".message-translatorPopup")
    MailElement translatorPopup();

    @Name("Кнопка «Да» в попапе «Скрыть переводчик»")
    @FindByCss("[class*='TranslatorPopup__translator-popup-button--']")
    MailElement confirmTranslatorBtn();

    @Name("Крестик в попапе «Скрыть переводчик»")
    @FindByCss("[class*='Close__substrate--']")
    MailElement closeTranslatorPopupBtn();

    @Name("Список языков перевода")
    @FindByCss("[class*='SelectionMenu__langList-item--']")
    ElementsCollection<MailElement> choiceLangList();

    @Name("Крестик в списке языков перевода")
    @FindByCss("[class*='SelectionMenu__selectionMenu-closeButton--']")
    MailElement closeChoiceLangListBtn();
}
