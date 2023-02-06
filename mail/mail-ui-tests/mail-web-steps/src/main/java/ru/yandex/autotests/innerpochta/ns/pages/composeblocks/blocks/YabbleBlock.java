package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface YabbleBlock extends MailElement {
    @Name("Кнопка удаления яббла")
    @FindByCss(".ComposeYabble-RemoveIconWrapper")
    MailElement yabbleDeleteBtn();

    @Name("Текст яббла")
    @FindByCss(".ComposeYabble-Text")
    MailElement yabbleText();
}
