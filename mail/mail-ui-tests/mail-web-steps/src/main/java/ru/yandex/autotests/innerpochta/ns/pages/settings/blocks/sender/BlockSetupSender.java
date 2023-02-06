package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.right.BlockAliases;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.right.BlockAvatar;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature.SignaturesSetupBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupSender extends MailElement {

    @Name("Поле ввода «От кого»")
    @FindByCss("[name = 'from_name']")
    MailElement fromName();

    @Name("Блок «Ваши Подписи»")
    @FindByCss(".b-form-layout__line.b-form-element_signature__box.js-setup-signatures")
    SignaturesSetupBlock signatures();

    @Name("Радио «Расположение подписи при ответе»")
    @FindByCss("[name = 'signature_top']")
    ElementsCollection<MailElement> signPlace();

    @Name("Блок «Отправлять письма с адреса»")
    @FindByCss(".b-setup__col:nth-of-type(2)>div>.b-form-layout__block_settings")
    BlockAliases blockAliases();

    @Name("Блок «Мой портрет»")
    @FindByCss(".ns-view-setup-avatar-setup")
    BlockAvatar blockAvatar();

    @Name("Кнопка «Сохранить изменения»")
    @FindByCss(".b-mail-button_setup-saver")
    MailElement saveButton();
}
