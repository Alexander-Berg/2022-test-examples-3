package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.touch.pages.compose.AttachFilesPopup;
import ru.yandex.autotests.innerpochta.touch.pages.compose.Attachments;
import ru.yandex.autotests.innerpochta.touch.pages.compose.DelayedSendingPopup;
import ru.yandex.autotests.innerpochta.touch.pages.compose.DiskAttachmentsPage;
import ru.yandex.autotests.innerpochta.touch.pages.compose.Header;
import ru.yandex.autotests.innerpochta.touch.pages.compose.LabelsPopup;
import ru.yandex.autotests.innerpochta.touch.pages.compose.RemindersPopup;
import ru.yandex.autotests.innerpochta.touch.pages.compose.SignPopup;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */
public interface ComposeIframePage extends MailPage {

    String IFRAME_COMPOSE = "iframe[class=\"composeFrame\"]";

    @Name("Окно композа")
    @FindByCss("iframe[class=\"composeFrame\"]")
    MailElement iframe();

    @Name("Шапка")
    @FindByCss(".composeReact-Header")
    Header header();

    @Name("Блок с аттачами в композе")
    @FindByCss(".ComposeAttachments")
    Attachments attachments();

    @Name("Страница дисковых аттачей")
    @FindByCss(".browseDisk_mobile")
    DiskAttachmentsPage diskAttachmentsPage();

    @Name("Попап «Добавление файлов»")
    @FindByCss(".ComposeAttachFileButton-Popup")
    AttachFilesPopup attachFilesPopup();

    @Name("Поле «От кого»")
    @FindByCss(".ComposeAddressFrom .ComposeAddressFrom-Content")
    MailElement fieldFrom();

    @Name("Поле ввода «Кому»")
    @FindByCss(".ComposeRecipients-TopRow>.MultipleAddressesMobile .composeYabbles")
    MailElement inputTo();

    @Name("Поле ввода «Копия»")
    @FindByCss(".ComposeRecipients-AdditionalAddresses .MultipleAddressesMobile:nth-child(1) .composeYabbles")
    MailElement inputCc();

    @Name("Поле ввода «Скрытая копия»")
    @FindByCss(".ComposeRecipients-AdditionalAddresses .MultipleAddressesMobile:nth-child(2) .composeYabbles")
    MailElement inputBcc();

    @Name("Поле ввода темы письма")
    @FindByCss("input[name=\"subject\"]")
    MailElement inputSubject();

    @Name("Поле ввода тела письма")
    @FindByCss(".ComposeMbodySimpleEditor-Contents")
    MailElement inputBody();

    @Name("Саджест контактов")
    @FindByCss(".ComposeContactsSuggestMobile")
    MailElement composeSuggest();

    @Name("Контакты в саджестве")
    @FindByCss(".ContactsSuggestItemMobile")
    ElementsCollection<MailElement> composeSuggestItems();

    @Name("Яббл")
    @FindByCss(".ComposeYabble_editable")
    MailElement yabble();

    @Name("Яббл внешнего юзера в корпе")
    @FindByCss(".ComposeYabble_external")
    MailElement externalYabble();

    @Name("Ябблы")
    @FindByCss(".ComposeYabble_editable")
    ElementsCollection<MailElement> yabbles();

    @Name("Яббл «Ещё n»")
    @FindByCss(".js-yabble-more")
    MailElement yabbleMore();

    @Name("Крестик для удаления яббла")
    @FindByCss(".ComposeYabble-RemoveIconWrapper")
    MailElement deleteYabble();

    @Name("Саджест алиасов")
    @FindByCss(".ComposeYabbleMenu-Addresses")
    MailElement suggestAliases();

    @Name("Контакты в саджестве")
    @FindByCss(".ComposeYabbleMenu-Item_address")
    ElementsCollection<MailElement> composeAliasItems();

    @Name("Стрелка для разворачивания полей")
    @FindByCss(".ComposeRecipients-Expander")
    MailElement expandComposeFields();

    @Name("Стрелка для сворачивания полей")
    @FindByCss(".ComposeExpanderButton-CollapseIcon")
    MailElement hideComposeFields();

    @Name("Подпись")
    @FindByCss(".ComposeReact-SignatureContainer div")
    MailElement signature();

    @Name("Попап «Письмо не может быть отправлено»")
    @FindByCss(".ComposeConfirmPopup-Content")
    MailElement cantSendMailPopup();

    @Name("Кнопка ОК на попапе «Письмо не может быть отправлено»")
    @FindByCss(".ComposeConfirmPopup-Button_action")
    MailElement confirmBtn();

    @Name("Цитита")
    @FindByCss("[type='cite']")
    MailElement quote();

    @Name("Статуслайн с информацией о действии/событии")
    @FindByCss("[class*='NotificationToast__info--'] [class*='NotificationToast__toast-content--']")
    MailElement statusLineInfo();

    @Name("Адрес/имя контакта")
    @FindByCss(".ComposeYabble-Text")
    MailElement yabbleText();

    @Name("Редактируемый яббл")
    @FindByCss(".composeYabbles")
    MailElement editableYabble();

    @Name("Попап с чекбоксами напоминаний")
    @FindByCss(".ComposeNotificationsButton-Popup")
    RemindersPopup remindersPopup();

    @Name("Попап отложенной отправки")
    @FindByCss(".ComposeDelayedSendingButton-Popup")
    DelayedSendingPopup delayedSendingPopup();

    @Name("Попап меток")
    @FindByCss(".ComposeLabelButton-Popup")
    LabelsPopup labelsPopup();

    @Name("Поле «Метки»")
    @FindByCss(".ComposeLabels")
    MailElement labels();

    @Name("Поле «Метки»")
    @FindByCss(".ComposeLabel-Remove")
    MailElement deleteLabel();

    @Name("Кнопка открытия попапа с подписями")
    @FindByCss(".ComposeReact-SignatureMenuAnchor")
    MailElement signBtn();

    @Name("Попап подписей")
    @FindByCss(".SignaturesPopup")
    SignPopup signPopup();
}
