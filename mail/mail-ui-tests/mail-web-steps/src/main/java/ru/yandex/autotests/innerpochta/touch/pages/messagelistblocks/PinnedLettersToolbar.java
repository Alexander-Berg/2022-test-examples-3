package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface PinnedLettersToolbar extends MailElement {

    @Name("Счетчик закрепленных писем")
    @FindByCss(".messagesHead-pins .shrinker__right")
    MailElement counter();
}
