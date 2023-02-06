package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author kurau
 */
public interface EditSignatureBlock extends MailElement {

    @Name("Кнопка «Сохранить» подпись")
    @FindByCss(".js-signature-save")
    MailElement saveBtn();

    @Name("Иконка языка для изменения в подписи")
    @FindByCss(".js-setup-signature-edit-lang")
    MailElement editLang();
}
