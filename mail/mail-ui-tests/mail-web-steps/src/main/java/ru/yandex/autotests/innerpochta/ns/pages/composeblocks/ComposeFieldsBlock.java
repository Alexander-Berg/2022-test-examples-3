package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.LabelsBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines
 */
public interface ComposeFieldsBlock extends MailElement {

    @Name("Поле ввода «Кому»")
    @FindByCss(".js-compose-field[name='to']")
    MailElement fieldTo();

    @Name("Имя ябла в «Кому»")
    @FindByCss(".js-compose-field[name='to'] .mail-Bubble-Block_text")
    MailElement yabbleNameTo();

    @Name("Яббл «Кому»")
    @FindByCss(".js-compose-field[name='to'] .js-bubble")
    MailElement yabbleTo();

    @Name("Поле ввода для саджеста")
    @FindByCss(".js-compose-field[name='to']")
    MailElement suggestInputTo();

    @Name("Список ябблов в поле «Кому»")
    @FindByCss(".js-compose-field[name='to'] .js-bubble")
    ElementsCollection<MailElement> yabbleToList();

    @Name("Список адресов в поле Кому")
    @FindByCss(".js-compose-field[name='to'] .mail-Bubble-Block_text")
    ElementsCollection<MailElement> addressesList();

    @Name("Поле ввода «Копия»")
    @FindByCss(".js-compose-field[name='cc']")
    MailElement fieldCc();

    @Name("Яббл «Копия»")
    @FindByCss(".js-compose-field[name='cc'] .js-bubble")
    MailElement yabbleCc();

    @Name("Имя ябла в «Копия»")
    @FindByCss(".js-compose-field[name='cc'] .mail-Bubble-Block_text")
    MailElement yabbleNameCC();

    @Name("Поле ввода «Скрытая копия»")
    @FindByCss(".js-compose-field[name='bcc']")
    MailElement fieldBcc();

    @Name("Яббл «Скрытая копия»")
    @FindByCss(".js-compose-field[name='bcc'] .js-bubble")
    MailElement yabbleBcc();

    @Name("Ссылка «копия»")
    @FindByCss(".ns-view-compose-field-to-extras-cc")
    MailElement addCcLink();

    @Name("Ссылка «скрытая копия»")
    @FindByCss(".ns-view-compose-field-to-extras-bcc")
    MailElement addBccLink();

    @Name("Ссылка «копия в SMS» рядом с yabble")
    @FindByCss(".js-sms-open-link")
    MailElement addSmsLink();

    @Name("Ссылка «копия в SMS» рядом с CC и BCC")
    @FindByCss(".ns-view-compose-field-to-extras-phone")
    MailElement smsLink();

    @Name("Неактивная ссылка «копия в SMS» рядом с CC и BCC")
    @FindByCss(".ns-view-compose-field-to-extras-phone.is-disabled-clickable")
    MailElement disabledSmsLink();

    @Name("Поле ввода темы")
    @FindByCss(".ns-view-compose-field-subject .mail-Compose-Field-Input-Controller")
    MailElement fieldSubject();

    @Name("Линк «Шаблон» в поле ввода темы")
    @FindByCss(".js-show-template-list")
    MailElement templateDropDown();

    @Name("Поле с проставленными пользовательскими метками")
    @FindByCss(".ns-view-compose-labels")
    LabelsBlock labelsBlock();

    @Name("Алерт под полем Кому")
    @FindByCss(".ns-view-compose-field-to-error")
    MailElement fieldToAlert();

    @Name("Поле ввода номера телефона для копии в СМС")
    @FindByCss(".js-phone-bubble .mail-Bubble-Block_text")
    MailElement phoneField();

    @Name("Инпут номера телефона для копии в СМС")
    @FindByCss(".js-compose-field[name='phone']")
    MailElement phoneInput();

    @Name("Яббл для копии в СМС")
    @FindByCss(".js-compose-field[name='phone'] .js-bubble")
    MailElement phoneYabble();

    @Name("Поле подсказок о номере телефона")
    @FindByCss(".ns-view-compose-field-phone-notices-wrapper")
    MailElement phoneFieldNotifier();

    @Name("Кнопка помощи в поле нотифаек")
    @FindByCss(".ns-view-compose-field-notices-phone a")
    MailElement helpPhoneLink();

    @Name("Алерт в поле SMS")
    @FindByCss(".mail-Compose-Field-Input-Error")
    MailElement smsAlert();

    @Name("Ошибка в поле SMS")
    @FindByCss(".ns-view-compose-field-phone-error")
    MailElement smsError();

    @Name("Контакты в списке популярных контактов")
    @FindByCss(".ns-view-compose-popular-contacts .js-contact")
    ElementsCollection<MailElement> popularContacts();

    @Name("Полоска популярных контактов")
    @FindByCss(".mail-Compose-Contacts")
    MailElement popularContactsBlock();

    @Name("Крестик, скрывающий полоску популярных контактов")
    @FindByCss(".svgicon-mail--Close")
    MailElement popularContactsCancelBtn();

}
