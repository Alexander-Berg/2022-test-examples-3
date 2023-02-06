package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.group;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface GroupsBlock extends MailElement {

    @Name("Блок конкретной группы")
    @FindByCss(".js-abook-group")
    ElementsCollection<EveryGroupBlock> groups();

    @Name("Кнопка «Создать группу»")
    @FindByCss(".js-abook-add-new-group")
    MailElement createGroupButton();

    @Name("Показать все контакты")
    @FindByCss(".js-abook-groups-all")
    MailElement showAllContactsLink();

    @Name("Кнопка «Настройки»")
    @FindByCss(".js-abook-group-settings")
    MailElement settingsButton();

    @Name("Общие контакты")
    @FindByCss(".js-abook-groups-shared")
    MailElement sharedContacts();

    @Name("Личные контакты")
    @FindByCss(".js-abook-groups-all")
    MailElement personalContacts();

    @Name("Активная плашка личных контактов")
    @FindByCss(".js-abook-groups-all.mail-NestedList-Item_current")
    MailElement activePersonalContacts();

    @Name("Счетчик личных контактов")
    @FindByCss(".js-abook-groups-all .mail-NestedList-Item-Info")
    MailElement personalContactsCounter();
}



