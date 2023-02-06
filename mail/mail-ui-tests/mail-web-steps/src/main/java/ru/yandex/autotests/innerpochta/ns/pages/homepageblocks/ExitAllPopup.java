package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 04.08.15.
 */
public interface ExitAllPopup extends MailElement{

    @Name("Линк на выход")
    @FindByCss("[data-click-action='common.clck']")
    MailElement confirmLink();
}
