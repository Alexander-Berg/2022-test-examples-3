package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.AttachmentsWidget;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.QuoteBlock;

public interface AttachedMessageBlock extends MailElement {

    @Name("Виджет с аттачами")
    @FindByCss(".qa-MessageViewer-Attachments-wrapper")
    AttachmentsWidget attachments();

    @Name("Цитаты")
    @FindByCss(".qa-MessageViewer-RootQuote-Wrapper")
    ElementsCollection<QuoteBlock> quotes();

}
