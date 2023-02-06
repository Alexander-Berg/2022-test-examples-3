package ru.yandex.autotests.innerpochta.ns.pages.folderblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by kurau on 20.03.14.
 */
public interface CustomLabelBlock extends MailElement {

    @Name("Счетчик пользовательской метки")
    @FindByCss(".mail-LabelList-Item_count")
    MailElement labelCounter();

    @Name("Имя пользовательской метки")
    @FindByCss(".qa-LeftColumn-TagName")
    MailElement labelName();
}
