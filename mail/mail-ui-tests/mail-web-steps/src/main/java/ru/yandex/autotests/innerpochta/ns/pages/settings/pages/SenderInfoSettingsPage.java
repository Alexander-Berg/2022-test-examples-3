package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.StatusLineBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.BlockSetupSender;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.popup.DeleteSignaturePopup;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.popup.MaxCountSignaturesPopup;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature.LanguagesDropDown;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature.PromoSignatureBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 01.06.12
 * <p> Time: 15:21
 */
public interface SenderInfoSettingsPage extends MailPage {

    @Name("Все настройки → Информация об отправителе")
    @FindByCss(".ns-view-setup-sender")
    BlockSetupSender blockSetupSender();

    @Name("Попап удаления подписи")
    @FindByCss(".b-popup:not(.g-hidden)")
    DeleteSignaturePopup deleteSignaturePopup();

    @Name("Попап при достижении лимита")
    @FindByCss(".b-popup:not(.g-hidden)")
    MaxCountSignaturesPopup maxCountSignaturesPopup();

    @Name("Выпадающее меню выбора языка для подписи")
    @FindByCss("body>.b-mail-dropdown__box .b-mail-dropdown__box__content:not([style=\"display: none;\"])")
    LanguagesDropDown languagesDropdown();

    @Name("Блок промо для подписи")
    @FindByCss(".promo-intruder-content.promo-intruder-content-rus")
    PromoSignatureBlock promoSignature();

    @Name("Строка уведомления")
    @FindByCss(".mail-Statusline")
    StatusLineBlock statusLineBlock();

    @Name("Стили текста подписи")
    @FindByCss(".jst-editor-group-buttons")
    ElementsCollection<MailElement> signatureTextFormatItems();

    @Name("Цвета текста подписи")
    @FindByCss(".colorMenu-Item")
    ElementsCollection<MailElement> signatureTextColorItems();

    @Name("Шрифты текста подписи")
    @FindByCss(".jst-editor-font-family")
    ElementsCollection<MailElement> signatureFontItems();

    @Name("Размер шрифта текста подписи")
    @FindByCss(".jst-editor-font-size")
    ElementsCollection<MailElement> signatureFontSizeItems();
}
