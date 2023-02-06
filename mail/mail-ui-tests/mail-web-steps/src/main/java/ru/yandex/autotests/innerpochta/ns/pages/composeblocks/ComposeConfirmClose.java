package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface ComposeConfirmClose extends MailElement {

    @Name("Крестик закрытия попапа")
    @FindByCss(".ComposeConfirmPopup-Close")
    MailElement closeBtn();

    @Name("Кнопка «Сохранить» черновик")
    @FindByCss(".ComposeConfirmPopup-Button_action")
    MailElement saveBtn();

    @Name("Кнопка «Не сохранять» черновик")
    @FindByCss(".ComposeConfirmPopup-Button_cancel")
    MailElement cancelBtn();

    @Name("Кнопка «показать другую картинку» для каптчи")
    @FindByCss(".ComposeReactCaptcha-Link")
    MailElement refreshCaptchaBtn();

    @Name("Заголовок попапа")
    @FindByCss(".ComposeConfirmPopup-Title")
    MailElement title();

    @Name("Описание попапа")
    @FindByCss(".ComposeConfirmPopup-Description")
    MailElement description();
}
