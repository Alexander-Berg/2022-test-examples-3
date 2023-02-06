package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SampleLabelBlock extends MailElement {

    @Name("Крестик Закрыть")
    @FindByCss(".js-compose-label-unlabel")
    MailElement removeLabel();
}
