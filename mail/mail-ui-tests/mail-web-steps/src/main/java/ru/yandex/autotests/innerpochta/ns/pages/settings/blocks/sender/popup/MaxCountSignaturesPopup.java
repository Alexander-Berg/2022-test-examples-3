package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MaxCountSignaturesPopup extends MailElement {

    @Name("Текст попапа при достижении максимального количкества подписей")
    @FindByCss(".b-popup__body")
    MailElement maxCountText();

    @Name("Кнопка «Закрыть» попап")
    @FindByCss("[data-dialog-action='dialog.cancel']")
    MailElement closePopup();
}
