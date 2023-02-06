package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface AdInfoBlock extends MailElement {

    @Name("ID рекламного блока")
    @FindByCss("span[title='ID блока']")
    AdInfoBlock blockId();

    @Name("Количество отрисовок блока left-col-rtb-1")
    @FindByCss("span[title='Количество отрисовок блока left-col-rtb-1']")
    AdInfoBlock blockRefreshCount();
}