package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.right;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 24.03.14.
 */
public interface UserAddressesBlock extends MailElement {

    @Name("Домен отправки письма")
    @FindByCss(".js-radio-select")
    MailElement domain();

    @Name("Адрес отправки письма")
    @FindByCss(".js-radio-input")
    MailElement address();
}
