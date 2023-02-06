package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface MessageTextBlock extends MailElement {

    @Name("Текст письма")
    @FindByCss("p")
    MailElement text();

    @Name("Место в письме с текстом «{{ mailText }}»")
    @FindBy(value = ".//div[contains(text(), '{{ mailText }}')]")
    MailElement certainText(@Param("mailText") String mailText);

    @Name("Ссылка в тексте письма")
    @FindByCss("a")
    ElementsCollection<MailElement> messageHref();

    @Name("Подсвеченый адрес в тексте письма")
    @FindByCss(".qa-MessageViewer-AddressWidget-wrapper")
    MailElement highlightedAddress();

    @Name("Цитаты")
    @FindByCss(".qa-MessageViewer-RootQuote-Wrapper")
    ElementsCollection<QuoteBlock> quotes();

    @Name("Инлайн аттачи")
    @FindByCss("img")
    ElementsCollection<MailElement> inlineAttaches();
}
