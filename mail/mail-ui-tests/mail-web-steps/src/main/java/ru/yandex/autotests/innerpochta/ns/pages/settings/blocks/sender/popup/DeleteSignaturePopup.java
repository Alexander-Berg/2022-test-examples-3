package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 16.02.14.
 */
public interface DeleteSignaturePopup extends MailElement {

    @Name("Кнопка «Удалить» подпись")
    @FindByCss("[data-dialog-action='dialog.delete']")
    MailElement deleteButton();

    @Name("Линк «Отмена» удаления подписи")
    @FindByCss(".b-link.b-link_recessive")
    MailElement cancelLink();
}
