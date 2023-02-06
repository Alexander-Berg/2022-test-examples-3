package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SignatureToolbarBlock extends MailElement {

    @Name("Кнопка «Редактировать» на плашке")
    @FindByCss(".b-form-element__signature__toolbar__item_edit")
    MailElement edit();

    @Name("Кнопка «Удалить» на плашке")
    @FindByCss(".b-form-element__signature__toolbar__item_delete")
    MailElement delete();

    @Name("Текст подписи на плашке")
    @FindByCss(".js-setup-signature-text")
    MailElement textSignature();

    @Name("Язык подписи на плашке")
    @FindByCss(".b-form-element_signature_info .b-form-element_signature_info__lang_box")
    MailElement langText();
}
