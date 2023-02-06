package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface GridEvent extends MailElement {

    @Name("Название")
    @FindByCss(".qa-GridEvent-Name")
    MailElement eventName();

    @Name("Время начала")
    @FindByCss(".qa-GridEvent-TimeStart")
    MailElement eventTimeStart();
}
