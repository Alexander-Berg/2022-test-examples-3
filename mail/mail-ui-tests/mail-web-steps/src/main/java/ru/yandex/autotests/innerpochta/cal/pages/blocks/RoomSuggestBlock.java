package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface RoomSuggestBlock extends MailElement {

    @Name("Доступность")
    @FindByCss("[class*=TouchResource__dueDate]")
    MailElement availability();
}
