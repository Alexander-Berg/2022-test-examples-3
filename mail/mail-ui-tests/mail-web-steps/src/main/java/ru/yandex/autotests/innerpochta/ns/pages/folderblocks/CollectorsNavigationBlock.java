package ru.yandex.autotests.innerpochta.ns.pages.folderblocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface CollectorsNavigationBlock extends MailElement {

    @Name("Ссылки на почтовые ящики для сборщика")
    @FindByCss(".qa-LeftColumn-TagName")
    ElementsCollection<MailElement> collectorsList();
}
