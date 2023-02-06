package ru.yandex.autotests.innerpochta.ns.pages;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposeConfirmClose;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposeKukutz;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposePageAddressBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposePopupSuggestBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposeStackThumb;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.ComposeThumb;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.YabbleBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown.ComposePopupYabbleDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown.SignatureDropdownBlock;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.AddDiskAttachPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.CaptchaPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.ExpandedPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.LabelsPopup;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup.SmartSubjectPopup;

/**
 * @author marchart
 */
public interface ComposePopup extends MailPage {
    @Name("Окно нового композа")
    @FindByCss(".ComposePopup-Content")
    MailElement composePopup();

    @Name("Развернутый попап композа")
    @FindByCss(".ComposePopup:not(.ComposeManager-PopupCompose_hidden)")
    ExpandedPopup expandedPopup();

    @Name("Свёрнутый композ")
    @FindByCss(".ComposeStackThumb")
    ElementsCollection<ComposeThumb> composeThumb();

    @Name("Выпадушка стека свернутых композов")
    @FindByCss(".ComposeStackMenu-Items")
    MailElement composeStackDropout();

    @Name("Элементы стека свернутых композов")
    @FindByCss(".ComposeStackMenu-Item")
    ElementsCollection<ComposeStackThumb> composeStackThumb();

    @Name("Стек свернутых композов")
    @FindByCss(".ComposeStackMenu")
    MailElement composeStack();

    @Name("Надпись Черновики в стэке свернутых композов")
    @FindByCss(".ComposeStackMenu-Title")
    MailElement composeStackTitle();

    @Name("Контент стека свернутых композов")
    @FindByCss(".ComposeStackMenu-Content")
    MailElement composeStackContent();

    @Name("Кнопка перехода в черновки в стеке свернутых композов")
    @FindByCss(".ComposeStackMenu-DraftLink")
    MailElement composeStackToDrafts();

    @Name("Окно подтверждения выхода из композа")
    @FindByCss(".ComposeConfirmPopup-Content")
    ComposeConfirmClose confirmClosePopup();

    @Name("Блоки саджеста адреса")
    @FindByCss(".ComposeContactsList .ComposeContactsList-Item")
    ElementsCollection<ComposePopupSuggestBlock> suggestList();

    @Name("Блоки саджеста адреса ябла «От кого» ")
    @FindByCss(".ComposeYabbleMenu-Addresses .ComposeYabbleMenu-Item")
    ElementsCollection<ComposePopupSuggestBlock> fromSuggestList();

    @Name("Яббл «Кому»")
    @FindByCss(".ComposeRecipients-MultipleAddressField .ComposeYabble")
    YabbleBlock yabbleTo();

    @Name("Список ябблов «Кому»")
    @FindByCss(".tst-field-to .ComposeYabble")
    ElementsCollection<YabbleBlock> yabbleToList();

    @Name("Список имён в поле «Кому»")
    @FindByCss(".ComposeRecipients-MultipleAddressField .ComposeYabble-Text")
    ElementsCollection<MailElement> yabbleToNamesList();

    @Name("Список email'ов в поле «Кому»")
    @FindByCss(".ComposeRecipients-MultipleAddressField .yabble-compose")
    ElementsCollection<MailElement> yabbleToEmailList();

    @Name("Яббл «Копия»")
    @FindByCss(".ComposeRecipients-AdditionalAddresses > :nth-child(1) .ComposeYabble")
    YabbleBlock yabbleCc();

    @Name("Список ябблов «Копия»")
    @FindByCss(".ComposeRecipients-AdditionalAddresses > :nth-child(1) .ComposeYabble")
    ElementsCollection<YabbleBlock> yabbleCcList();

    @Name("Яббл «Скрытая копия»")
    @FindByCss(".ComposeRecipients-AdditionalAddresses > :nth-child(2) .ComposeYabble")
    YabbleBlock yabbleBcc();

    @Name("Список ябблов «Скрытая копия»")
    @FindByCss(".ComposeRecipients-AdditionalAddresses > :nth-child(2) .ComposeYabble")
    ElementsCollection<YabbleBlock> yabbleBccList();

    @Name("Яббл «От Кого»")
    @FindByCss(".ComposeAddressFrom-Field .ComposeYabble")
    YabbleBlock yabbleFrom();

    @Name("Яббл «Ещё»")
    @FindByCss(".js-yabble-more")
    YabbleBlock yabbleMore();

    @Name("Выпадушка из яблла в композе")
    @FindByCss(".ComposeYabbleMenu")
    ComposePopupYabbleDropdown yabbleDropdown();

    @Name("Done Screen ссылка на папку Входящие")
    @FindByCss(".ComposeDoneScreen-Link")
    MailElement doneScreenInboxLink();

    @Name("Done Screen после отправки письма")
    @FindByCss(".ComposeDoneScreen")
    MailElement doneScreen();

    @Name("Промо хоткеев на Done")
    @FindByCss(".ComposeHotkeysPromo-Description")
    MailElement hotKeysPromo();

    @Name("Попап добавления аттача с диска")
    @FindByCss(".browseDisk")
    AddDiskAttachPopup addDiskAttachPopup();

    @Name("Тултип композного элемента")
    @FindByCss("[class*=withControls__tooltip]")
    MailElement composeTooltip();

    @Name("Блок кукутца")
    @FindByCss(".ComposeRecipientsDiff")
    ComposeKukutz composeKukutz();

    @Name("Тултип промо")
    @FindByCss(".ComposeRecipientsDiffPromo")
    MailElement kukutzPromo();

    @Name("abook - «Показать все контакты»")
    @FindByCss(".ComposeContactsSuggestDesktop-Action")
    MailElement abookBtn();

    @Name("Попап abook")
    @FindByCss(".mail-AbookPopup")
    ComposePageAddressBlock abookPopup();

    @Name("Крестик в попапе абука")
    @FindByCss("._nb-popup-close")
    MailElement abookCloseBtn();

    @Name("Ярлык для выбора множественной подписи")
    @FindByCss(".qa-Compose-SignatureMenuAnchor")
    MailElement signatureChooser();

    @Name("Выпадающее меню с подписями")
    @FindByCss(".qa-Compose-SignaturesPopup")
    SignatureDropdownBlock signaturesPopup();

    @Name("Блок подписи")
    @FindByCss(".qa-Compose-SignatureContainer")
    MailElement signatureBlock();

    @Name("Первая строка подписи")
    @FindByCss(".ComposeReact-SignatureContainer div:nth-of-type(1)")
    MailElement signatureFirstLine();

    @Name("Показать цитату")
    @FindByCss(".js-expand-quote")
    MailElement showQuote();

    @Name("Показать цитату в переводчике")
    @FindByCss(".ComposeReact-TranslationPanel .js-expand-quote")
    MailElement showQuoteTranslate();

    @Name("Попап с капчей")
    @FindByCss(".ComposeConfirmPopup-Content")
    CaptchaPopup captchaPopup();

    @Name("Кнопка Бета")
    @FindByCss(".svgicon-mail--Compose-Beta-Off")
    MailElement beta();

    @Name("Попап «Укажите тему письма»")
    @FindByCss(".ComposeConfirmPopup-Content")
    SmartSubjectPopup smartSubjectPopup();

    @Name("Попап меток")
    @FindByCss(".ComposeLabelOptions")
    LabelsPopup labelsPopup();
}
