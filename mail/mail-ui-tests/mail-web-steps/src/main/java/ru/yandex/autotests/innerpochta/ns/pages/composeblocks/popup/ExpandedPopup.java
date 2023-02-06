package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposeLabelsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposePageFormattedTextBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.AttachBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.CalendarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.ComposeToolbarBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.TranslateHeaderBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author eremin-n-s
 */
public interface ExpandedPopup extends MailElement {
    @Name("Кнопка «Свернуть» композ")
    @FindByCss(".qa-ControlButton_button_collapse")
    MailElement hideBtn();

    @Name("Кнопка «Закрыть» композ")
    @FindByCss(".qa-ControlButton_button_close")
    MailElement closeBtn();

    @Name("Кнопка «Уменьшить окно»")
    @FindByCss(".qa-ControlButton_button_maximize_small")
    MailElement minimizeBtn();

    @Name("Инпут темы письма")
    @FindByCss(".ComposeSubject-TextField")
    MailElement sbjInput();

    @Name("Тема письма в заголовке окна")
    @FindByCss(".ComposePopup-Head .composeHeader-Title")
    MailElement popupTitle();

    @Name("«Сохранено в...» в заголовке композа")
    @FindByCss(".composeHeader-SavedAt")
    MailElement savedAt();

    @Name("Кнопка «Отправить» письмо")
    @FindByCss(".qa-Compose-SendButton")
    MailElement sendBtn();

    @Name("Текст опции отложенной отправки на кнопке «Отправить»")
    @FindByCss(".qa-Compose-SendButton .ComposeSendButton-SecondaryText")
    MailElement sendBtnDelayTxt();

    @Name("Кнопка «Развернуть» шапку")
    @FindByCss(".ComposeRecipientsExpander")
    MailElement expandCollapseBtn();

    @Name("Поле «Кому»")
    @FindByCss(".ComposeRecipients-MultipleAddressField:first-child .composeYabbles")
    MailElement popupTo();

    @Name("Надпись «Кому»")
    @FindByCss(".ComposeRecipients-MultipleAddressField:first-child " +
            ".compose-LabelRow-Label")
    MailElement popupToLabel();

    @Name("Инпут «Кому»")
    @FindByCss(".ComposeRecipients-MultipleAddressField:first-child " +
            ".compose-LabelRow-Content")
    MailElement popupToInput();

    @Name("Поле «Копия»")
    @FindByCss(".ComposeRecipients-AdditionalAddresses .ComposeRecipients-MultipleAddressField:first-child " +
            ".composeYabbles")
    MailElement popupCc();

    @Name("Поле «Скрытая копия»")
    @FindByCss(".ComposeRecipients-AdditionalAddresses .ComposeRecipients-MultipleAddressField:nth-child(2) " +
            ".composeYabbles")
    MailElement popupBcc();

    @Name("Поле «От кого»")
    @FindByCss(".ComposeAddressFrom")
    MailElement popupFrom();

    @Name("Тема письма")
    @FindByCss(".ComposeSubject-Content")
    MailElement sbj();

    @Name("Инпут тела письма")
    @FindByCss(".cke_wysiwyg_div")
    ComposePageFormattedTextBlock bodyInput();

    @Name("Кнопка «Файлы с Диска»")
    @FindByCss(".qa-Compose-AttachDiskButton")
    MailElement diskAttachBtn();

    @Name("Кнопка «Файлы из Почты»")
    @FindByCss(".qa-Compose-AttachMailButton")
    MailElement mailAttachBtn();

    @Name("Инпут локального аттача")
    @FindByCss(".qa-Compose-FileInput2")
    MailElement localAttachInput();

    @Name("Залипающая панель аттачей")
    @FindByCss(".qa-Compose-Attachments")
    AttachBlock attachPanel();

    @Name("Тулбар в поле композа(сkeditor)")
    @FindByCss(".cke_toolbox")
    ComposeToolbarBlock toolbarBlock();

    @Name("Кнопка «Шаблоны»")
    @FindByCss(".qa-Compose-TemplatesButton")
    MailElement templatesBtn();

    @Name("«Прыщ» несохранённых изменений у шаблона")
    @FindByCss(".qa-Compose-ControlPanelButton-notification")
    MailElement templatesNotif();

    @Name("Показать цитату")
    @FindByCss(".mail-Compose-Quote-Toggler js-expand-quote")
    MailElement showQuote();

    @Name("Кнопка «Включить оповещения»")
    @FindByCss(".qa-Compose-NotificationsButton")
    MailElement notifyBtn();

    @Name("Попап оповещений")
    @FindByCss(".ComposeNotificationsOptions")
    MailNotifyPopup notifyPopup();

    @Name("Кнопка «Переводчик»")
    @FindByCss(".qa-Compose-TranslatorButton")
    MailElement translateBtn();

    @Name("Попап «Шаблоны»")
    @FindByCss(".ComposeTemplatesOptions")
    TemplatePopup templatePopup();

    @Name("Блок с изменение языка перевода")
    @FindByCss(".ComposeReact-TranslateLanguagesPanel")
    TranslateHeaderBlock translateHeader();

    @Name("Поле перевода письма")
    @FindByCss(".ComposeReact-TranslationPanel")
    MailElement translateText();

    @Name("Дропдаун со списком языков")
    @FindByCss(".ComposeReact-ChooseLanguage .ComposeReact-ChooseLanguage-Languages .ComposeReact-ChooseLanguage-Item")
    ElementsCollection<MailElement> changeLangList();

    @Name("Кнопка «Отложить отправку»")
    @FindByCss(".qa-Compose-DelayedSendingButton")
    MailElement delaySendBtn();

    @Name("Попап «Отложить отправку»")
    @FindByCss(".ComposeDelayedSendingButton-Popup")
    SendTimePopup delaySendPopup();

    @Name("Календарь для отложенной отправки")
    @FindByCss(".ComposeDateTimePicker")
    CalendarBlock calendar();

    @Name("Добавить изображение ссылкой")
    @FindByCss(".CKInlineImageMenu-Item:nth-child(2)")
    MailElement addImageLink();

    @Name("Попап добавления изображения ссылкой")
    @FindByCss(".CKEnterUrlForm")
    AddImageLinkPopup addImagePopup();

    @Name("Варианты выравнивания текста")
    @FindByCss(".jst-editor-group-buttons")
    ElementsCollection<MailElement> mailTextAlignment();

    @Name("Варианты цвета текста")
    @FindByCss(".jst-editor-color-item")
    ElementsCollection<MailElement> mailTextColorArray();

    @Name("Варианты типа шрифта")
    @FindByCss(".jst-editor-font-family")
    ElementsCollection<MailElement> mailFontArray();

    @Name("Варианты размера шрифта")
    @FindByCss(".jst-editor-font-size")
    ElementsCollection<MailElement> mailFontSizeArray();

    @Name("Кнопка «Удалить черновик»")
    @FindByCss(".ComposeDeleteDraftButton")
    MailElement composeDeleteBtn();

    @Name("Кнопка «Добавить eml»")
    @FindByCss(".ComposeForwardPanel .ComposeNotification-Action")
    MailElement composeAddEmlBtn();

    @Name("Кнопка «Троеточие»")
    @FindByCss(".qa-Compose-MoreButton")
    MailElement composeMoreBtn();

    @Name("Попап с опциями композа")
    @FindByCss(".qa-Compose-MoreOptions")
    ComposeMoreOptionsPopup composeMoreOptionsPopup();

    @Name("Попап Smart Suggest")
    @FindByCss(".cke_autocomplete_opened")
    MailElement smartSuggestPopup();

    @Name("Варианты Smart Suggest")
    @FindByCss(".cke_autocomplete_option")
    ElementsCollection<MailElement> smartSuggestOptions();

    @Name("Поле «Метки»")
    @FindByCss(".ComposeLabels")
    ComposeLabelsBlock labels();

}
