package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface MobilePromoBlock extends MailElement {

    @Name("Поле для ввода номера телефона")
    @FindByCss("[name = 'welcome-wizard-mobile-app-phone-number']")
    MailElement phoneInput();

    @Name("Кнопка Получить ссылку")
    @FindByCss(".js-buttons-send")
    MailElement getLinkBtn();

    @Name("Сообщение об успешной отправке")
    @FindByCss(".mail-PromoMobileApp-SuccessMessage_text")
    MailElement successMessage();

    @Name("Заголовок промки")
    @FindByCss(".mail-PromoMobileApp-Title")
    MailElement title();
}
