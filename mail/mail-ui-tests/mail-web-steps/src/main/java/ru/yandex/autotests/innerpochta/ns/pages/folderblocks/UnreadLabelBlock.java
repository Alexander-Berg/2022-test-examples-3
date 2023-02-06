package ru.yandex.autotests.innerpochta.ns.pages.folderblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface UnreadLabelBlock extends MailElement {

    @Name("Счетчик пользовательской метки")
    @FindByCss(".nb-button-count")
    MailElement labelCounter();
}