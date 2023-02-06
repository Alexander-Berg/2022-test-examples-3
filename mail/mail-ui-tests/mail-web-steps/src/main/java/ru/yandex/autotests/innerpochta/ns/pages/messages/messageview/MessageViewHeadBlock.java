package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * author a-zoshchuk
 */
public interface MessageViewHeadBlock extends MailElement {

    @Name("От кого «Имя Фамилия»")
    @FindByCss(".qa-MessageViewer-SenderName")
    MailElement fromName();

    @Name("От кого «Адрес»")
    @FindByCss(".qa-MessageViewer-SenderEmail")
    MailElement fromAddress();

    @Name("Эспандер получателей в шапке письма")
    @FindByCss(".qa-MessageViewer-RecipientsTitle")
    MailElement recipientsCount();

    @Name("Список получателей в поле Кому")
    @FindByCss(".qa-MessageViewer-Recipients-to .qa-MessageViewer-Recipient")
    ElementsCollection<MailElement> contactsInTo();

    @Name("Список получателей в поле Копия")
    @FindByCss(".qa-MessageViewer-Recipients-cc .qa-MessageViewer-Recipient")
    ElementsCollection<MailElement> contactsInCC();

    @Name("Список получателей в поле Скрытая копия")
    @FindByCss(".qa-MessageViewer-Recipients-bcc .qa-MessageViewer-Recipient")
    ElementsCollection<MailElement> contactsInBCC();

    @Name("Время прихода письма")
    @FindByCss(".qa-MessageViewer-Header-date")
    MailElement messageDate();

    @Name("Стрелочка - развернуть информацию о получателях")
    @FindByCss(".qa-MessageViewer-RecipientsExpandIcon")
    MailElement showFieldToggler();

    @Name("Важность письма. Не активна.")
    @FindByCss(".qa-MessageViewer-Important:not([class*='qa-MessageViewer-Important-active'])")
    MailElement labelImportance();

    @Name("Важность письма. Подсвечивается красным")
    @FindByCss(".qa-MessageViewer-Important-active")
    MailElement activeLabelImportance();

    @Name("Прочитанность письма. Не активна.")
    @FindByCss(".qa-MessageViewer-New:not([class*='qa-MessageViewer-New-active'])")
    MailElement messageRead();

    @Name("Прочитанность письма. Подсвечивается желтым")
    @FindByCss(".qa-MessageViewer-New-active")
    MailElement messageUnread();

    @Name("Область сворачивания письма")
    @FindByCss(".qa-MessageViewer-Header-emptySpace")
    MailElement closeMessageField();

    @Name("Замочек")
    @FindByCss(".qa-MessageViewer-SenderDkim")
    MailElement medal();

    @Name("Аватарка отправителя")
    @FindByCss(".qa-MessageViewer-SenderAvatar")
    MailElement senderAvatar();

    @Name("Аватарки получателя")
    @FindByCss(".qa-MessageViewer-ContactBadge-Avatar")
    ElementsCollection<MailElement> receiversAvatarsList();

}
