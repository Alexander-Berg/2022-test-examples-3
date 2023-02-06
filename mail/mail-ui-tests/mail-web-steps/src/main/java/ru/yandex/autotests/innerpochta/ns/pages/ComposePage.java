package ru.yandex.autotests.innerpochta.ns.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposeFieldsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposeFooterSendBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposeHeadBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposePageAddressBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposePageFileAttachmentBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposePageSaveToDraftBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposePageSuggestBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.MessageTextareaBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.SaveAsDraftOrTemplateDropdownMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.CalendarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.ComposeToolbarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.ForwardedMsgAttachBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown.ChangeAliasDropdownBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown.ComposeYabbleDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown.MessageWaitForAnswerDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown.SignatureDropdownBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown.TemplatesDropdownBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.AddDiskAttachPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.AddLinkPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.AddSignaturePopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.CaptchaPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.ConfirmChangesPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.HtmlFormattingOffPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.MailNotifyPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.SaveAfterSendPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.SendTimePopup;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ComposePage extends MailPage {

    @Name("Блоки саджеста адреса")
    @FindByCss(".mail-Suggest[style*='display: block;'] > .mail-Suggest-Item")
    ElementsCollection<ComposePageSuggestBlock> suggestList();

    @Name("Попап подтверждения сохранения письма в черновики")
    @FindByCss(".nb-popup[style*='display: block']")
    ComposePageSaveToDraftBlock composePageSaveToDraftBlock();

    @Name("Аттачи письма")
    @FindByCss(".b-file")
    ElementsCollection<ComposePageFileAttachmentBlock> fileAttachment();

    @Name("Всплывающее меню для выбора адреса из адресной книги")
    @FindByCss(".mail-AbookPopup")
    ComposePageAddressBlock abookPopup();

    @Name("Крестик закрытия попапа «Добавить получаетелей»")
    @FindByCss("._nb-popup-close")
    MailElement closeAbookPopupButton();

    @Name("Шапка композной страницы (кнопки “Отправить“, “Закрыть“, “Свернуть“, " +
        "поле “от кого“ с выбором отправителя и алиасов.)")
    @FindByCss(".ns-view-compose-head")
    ComposeHeadBlock composeHead();

    @Name("Блок под шапкой с полями ввода: “Кому“, “Тема“ и кнопками: “Копия“, “Скрытая копия“.")
    @FindByCss(".ns-view-compose-fields-wrapper")
    ComposeFieldsBlock composeFieldsBlock();

    @Name("Показать все контакты")
    @FindByCss(".mail-Suggest-Item-Abook_Button")
    MailElement addAddressFromAbook();

    @Name("Подвал композной страницы (кнопка, сообщение о сохранении в черновики)")
    @FindByCss(".ns-view-compose-footer")
    ComposeFooterSendBlock footerSendBlock();

    @Name("Выпадающее меню выбора сохранения. как Черновик/Шаблон")
    @FindByCss(".js-template-popup")
    SaveAsDraftOrTemplateDropdownMenuBlock saveAsDraftOrTemplateDropdownMenu();

    @Name("Блок ввода текста, визивиг, переводчик")
    @FindByCss(".js-compose-message")
    MessageTextareaBlock textareaBlock();

    @Name("Нотификация о некорректном адресе")
    @FindByCss(".b-notification_error_required:not([class*='hidden']) .b-notification__i")
    MailElement notificationAddressRequired();

    @Name("Выпадающее меню с выбором и созданием нового шаблона")
    @FindByCss(".mail-Compose-TemplateList-Popup")
    TemplatesDropdownBlock templatesDropdownMenu();

    @Name("Ярлык для выбора множественной подписи")
    @FindByCss(".mail-SignatureChooser_transition")
    MailElement signatureChooser();

    @Name("Выпадающее меню с подписями")
    @FindByCss(".ns-view-compose-signature-list-popup")
    SignatureDropdownBlock signaturesDropdownMenu();

    @Name("Попап добавления новой множественной подписи")
    @FindByCss(".ui-dialog")
    AddSignaturePopup addSignaturePopup();

    @Name("Выпадающее меню смены алиаса")
    @FindByCss(".nb-select-dropdown .ui-autocomplete[style*='display: block;']")
    ChangeAliasDropdownBlock changeAliasDropdown();

    //=======

    @Name("Попап при отмене форматирования письма")
    @FindByCss("._nb-popup-outer")
    HtmlFormattingOffPopup htmlFormattingOffPopup();

    @Name("Попап с временем отправки письма")
    @FindByCss(".ns-view-compose-send-button-complex-popup")
    SendTimePopup sendTimePopup();

    @Name("Попап с галочкой “Напоминать всегда“")
    @FindByCss(".js-compose-noreply-notify-popup ._nb-popup-content")
    MailNotifyPopup mailNotifyPopup();

    @Name("Календарь для отложенной отправки")
    @FindByCss(".b-mail-calendar__table")
    CalendarBlock calendar();

    @Name("Попап - У вас отключена настройка «Сохранять письма в папке \"Отправленные\"»")
    @FindByCss(".b-popup__box__content")
    SaveAfterSendPopup saveAfterSendPopup();

    @Name("Попап “Сохранить сделанные изменения“")
    @FindByCss(".ui-dialog-fixed .nb-popup:not(._nb-is-hidden)")
    ConfirmChangesPopup confirmChangesPopup();

    @Name("Попап с капчой")
    @FindByCss(".b-popup__box__content")
    CaptchaPopup captchaPopup();

    @Name("Тулбар в поле композа: Включить/выключить оформление, визивиг")
    @FindByCss(".cke_toolbox")
    ComposeToolbarBlock composeToolbarBlock();

    @Name("Выпадушка с временем через которое напомнить")
    @FindByCss(".ui-menu[style*='display: block']")
    MessageWaitForAnswerDropdown messageWaitForAnswerDropdown();

    @Name("Блок аттачей - пересылаемых сообщений")
    @FindByCss(".ns-view-compose-forwarded-messages li")
    ElementsCollection<ForwardedMsgAttachBlock> forwardedMsgAttachBlock();

    @Name("Ссылка на абук - “Остальные контакты“")
    @FindByCss(".mail-Suggest .mail-Suggest-Item-Abook_Button")
    MailElement otherContacts();

    @Name("Попап с алертом о невозможности отправить СМС-уведомление")
    @FindByCss(".nb-popup[style*='display: block']")
    MailElement smsDisabledAlert();

    @Name("Переводчик")
    @FindByCss(".cke_button__translate")
    MailElement translateBtn();

    @Name("Дропдаун со списком языков")
    @FindByCss(".mail-Translate-Langs > .js-lang")
    ElementsCollection<MailElement> changeLangList();

    @Name("Дропдаун со смайликами")
    @FindByCss(".mail-Compose-Emoticons_item")
    ElementsCollection<MailElement> smilesList();

    @Name("Попап добавления ссылки")
    @FindByCss(".linkModal__inner")
    AddLinkPopup addLinkPopup();

    @Name("Попап добавления аттача с диска")
    @FindByCss(".browseDisk")
    AddDiskAttachPopup addDiskAttachPopup();

    @Name("Попап “Вы не забыли приложить файл?“")
    @FindByCss(".ns-view-compose-forgotten-attach")
    MailElement forgotAttachPopup();

    @Name("Элемент селекта выбора группы")
    @FindByCss("._nb-select-item")
    ElementsCollection<MailElement> selectGroupItem();

    @Name("Выпадушка из яблла в композе")
    @FindByCss(".mail-Bubble-Dropdown")
    ComposeYabbleDropdown composeYabbleDropdown();

    @Name("Попап напоминания о письме")
    @FindByCss(".js-noreply-first-visit-popup")
    MailElement composeNotifyPopup();

    @Name("Блок с вложениями")
    @FindByCss(".js-compose-attachments-container")
    MailElement attachmentsBlock();

    @Name("Добавить картинку по ссылке")
    @FindByCss(".cke_button__addimage")
    MailElement addImage();

    @Name("Попап добавления картинки в подпись")
    @FindByCss(".mail-Compose-AddImage-Popup")
    MailElement addImagePopup();

    @Name("Кнопка «Добавить» в попапе добавления картинки")
    @FindByCss(".mail-Compose-AddImage-Popup-Action_add")
    MailElement addImageButton();

    @Name("Поле для ссылки на изображение")
    @FindByCss("._nb-input-controller")
    MailElement linkToImage();

}
