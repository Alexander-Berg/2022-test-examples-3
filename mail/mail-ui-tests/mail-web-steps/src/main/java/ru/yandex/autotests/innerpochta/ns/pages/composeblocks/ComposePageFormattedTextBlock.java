package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ComposePageFormattedTextBlock extends MailElement {

    @Name("Жирный текст")
    @FindByCss("strong")
    MailElement boldText();

    @Name("Курсивный текст")
    @FindByCss("em")
    MailElement italicText();
}


