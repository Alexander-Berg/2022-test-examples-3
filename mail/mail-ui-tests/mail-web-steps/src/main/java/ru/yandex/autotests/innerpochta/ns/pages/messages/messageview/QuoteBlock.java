package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface QuoteBlock extends MailElement {

    @Name("Ссылка разворачивания цитаты в тексте письма")
    @FindByCss(".qa-MessageViewer-QuoteControls-ExpanderIcon")
    ElementsCollection<MailElement> wrapQuoteLink();

    @Name("Ссылка «Ранее в переписке»")
    @FindByCss(".qa-MessageViewer-Quote-QuotesEarlier")
    MailElement showQuoteEarlier();

    @Name("Ссылка «Показать всю переписку»")
    @FindByCss(".qa-MessageViewer-RootQuote-ExpandAll span")
    MailElement showFullQuote();

    @Name("Авторы цитат")
    @FindByCss(".qa-MessageViewer-QuoteMeta-email")
    ElementsCollection<MailElement> quotesAuthors();

    @Name("Аватарки авторов цитат")
    @FindByCss(".qa-MessageViewer-QuoteControls-AvatarExpander .mail-Avatar")
    ElementsCollection<MailElement> quoteAuthorAvatar();

    @Name("Камыши цитат")
    @FindByCss(".qa-MessageViewer-QuoteControls-LineContainer")
    ElementsCollection<MailElement> quoteLine();

    @Name("Вложенная цитата")
    @FindByCss(".qa-MessageViewer-Quote-wrapper")
    ElementsCollection<MailElement> nestedQuote();
}
