package ru.yandex.autotests.innerpochta.touch.pages.searchblocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by puffyfloof.
 */
public interface SearchOptions extends MailElement{

    @Name("Дропдаун выбора папки")
    @FindByCss(".searchDropdown-fid .select")
    Select byFolder();

    @Name("Дропдаун выбора метки")
    @FindByCss(".searchDropdown-lid .select")
    Select byLabel();

    @Name("Дропдаун выбора скоупа")
    @FindByCss(".searchDropdown-scope .select")
    Select byScope();

    @Name("Дропдаун выбора типа")
    @FindByCss(".searchDropdown-type .select")
    Select byType();

    @Name("Чекбокс «С вложениями»")
    @FindByCss(".searchFlags-checkbox")
    MailElement withAttach();

    @Name("Даты в таймлайне")
    @FindByCss(".searchMessagerPager-item")
    ElementsCollection<MailElement> timelineItems();
}
