package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines
 */
public interface DropDownUserBlock extends MailElement {

    @Name("Имя юзера")
    @FindByCss(".user-account__name-text")
    MailElement name();

    @Name("Нотификация")
    @FindByCss(".dropdown-user-notification")
    MailElement notification();
}
