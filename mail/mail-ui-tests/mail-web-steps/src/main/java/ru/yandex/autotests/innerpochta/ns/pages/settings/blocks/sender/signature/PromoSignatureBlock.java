package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 17.02.14.
 */
public interface PromoSignatureBlock extends MailElement {

    @Name("Кнопка «Добавить подпись»")
    @FindByCss(".js-signature-new-select")
    MailElement newSignatureButton();
}
