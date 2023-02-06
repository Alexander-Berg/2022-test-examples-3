package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.group;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface EveryGroupBlock extends MailElement {

    @Name("Счетчик группы")
    @FindByCss(".mail-LabelList-Item_count")
    MailElement groupCounter();

    @Name("Имя группы")
    @FindByCss(".js-abook-group-name")
    MailElement groupName();

    @Name("Иконка - написать группе")
    @FindByCss(".js-abook-write-group")
    MailElement writeGroupBtn();
}




