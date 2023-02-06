package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface DropDownLegoUserBlock extends MailElement {

    @Name("Каунтер новых писем")
    @FindByCss(".user-account .counterLabel")
    MailElement messagesCount();
}
