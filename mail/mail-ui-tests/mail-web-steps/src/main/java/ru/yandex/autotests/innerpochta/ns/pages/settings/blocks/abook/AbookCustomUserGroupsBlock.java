package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.abook;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface AbookCustomUserGroupsBlock extends MailElement {

    @Name("Счетчик группы контактов")
    @FindByCss(".setup-abook-group__count")
    MailElement groupCounter();

    @Name("Имя группы")
    @FindByCss(".setup-abook-group__title")
    MailElement groupName();
}
