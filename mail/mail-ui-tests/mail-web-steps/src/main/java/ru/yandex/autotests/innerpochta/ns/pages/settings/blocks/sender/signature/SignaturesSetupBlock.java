package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SignaturesSetupBlock extends MailElement {

    /* Поле ввода */

    @Name("Выбрать шрифт")
    @FindByCss(".cke_button__menuselection ")
    ElementsCollection<MailElement> fontSelect();

    @Name("Список")
    @FindByCss(".cke_button__menulist")
    MailElement listSelect();

    @Name("Выравнивание")
    @FindByCss(".cke_button__menualignment")
    MailElement alignmentSelect();

    @Name("Цвет текста")
    @FindByCss(".cke_button__mailtextcolor")
    MailElement colorSelect();

    @Name("Цвет фона")
    @FindByCss(".cke_button__mailbgcolor")
    MailElement bgColorSelect();

    @Name("Шрифт")
    @FindByCss(".cke_button__mailfont")
    MailElement fontTextSelect();

    @Name("Размер шрифта")
    @FindByCss(".cke_button__mailfontsize")
    MailElement fontSizeSelect();

    @Name("Добавление картинки по ссылке")
    @FindByCss(".cke_button__addimage")
    MailElement addImage();

    @Name("Поля ввода подписи")
    @FindByCss(".cke_editable")
    ElementsCollection<MailElement> input();

    @Name("Язык")
    @FindByCss(".js-setup-signature-edit-lang")
    MailElement editLang();

    @Name("Селект алиасов при добавлении новой подписи")
    @FindByCss(".js-bind-address")
    MailElement aliasesCheckBox();

    @Name("Селект алиасов при добавлении новой подписи")
    @FindByCss(".js-radio-select.ui-autocomplete-input")
    MailElement aliasesList();

    /* Кнопка */
    @Name("Кнопка «Добавить подпись» в настройках отправителя")
    @FindByCss(".nb-button.js-signature-add")
    MailElement addBtn();

    @Name("Редактирование существующей подписи")
    @FindByCss(".js-signature-edit")
    MailElement editSignature();

    @Name("Сохранение подписи")
    @FindByCss(".js-signature-save")
    MailElement saveSignature();

    /* Существующие подписи */
    @Name("Список существующих подписей юзера")
    @FindByCss(".js-setup-signature.b-form-element_signature-inactive")
    ElementsCollection<SignatureToolbarBlock> signaturesList();

    @Name("Редактирование существующей подписи (после клика на тулбар)")
    @FindByCss("div.js-setup-signature:not(.b-form-element_signature-inactive)")
    EditSignatureBlock editSignatureBlock();

    @Name("Активное поле ввода")
    @FindByCss(".cke_editor_signature_new.cke_focus")
    MailElement activeInput();
}
