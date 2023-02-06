package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockConnectedCollectors extends MailElement {

    @Name("Список из подключенных ящиков")
    @FindByCss(".b-pop__email")
    ElementsCollection<BlockConnectedCollector> collectors();
}
