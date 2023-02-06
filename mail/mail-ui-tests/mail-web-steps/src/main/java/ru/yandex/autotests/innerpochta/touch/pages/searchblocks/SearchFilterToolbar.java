package ru.yandex.autotests.innerpochta.touch.pages.searchblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by puffyfloof.
 */
public interface SearchFilterToolbar extends MailElement {

    @Name("Поиск по всей почте")
    @FindByCss(".searchSwitch-button:nth-of-type(1)")
    MailElement searchAllMail();

    @Name("Поиск по папке")
    @FindByCss(".searchSwitch-button:nth-of-type(2)")
    MailElement searchFolder();

    @Name("Кнопка расширенных фильтров")
    @FindByCss(".searchSwitch-filterToggle")
    MailElement toggle();
}
