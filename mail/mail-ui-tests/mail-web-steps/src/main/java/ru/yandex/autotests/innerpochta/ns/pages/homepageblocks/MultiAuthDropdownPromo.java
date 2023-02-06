package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 04.08.15.
 */
public interface MultiAuthDropdownPromo extends MailElement {

    @Name("Добавить пользователя")
    @FindByCss(".js-user-dropdown-promo-click")
    MailElement addUserButton();
}

