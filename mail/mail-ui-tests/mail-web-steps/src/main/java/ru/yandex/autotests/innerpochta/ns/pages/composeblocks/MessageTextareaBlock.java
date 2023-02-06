package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.TranslateHeaderBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 14.05.14.
 */
public interface MessageTextareaBlock extends MailElement {

    @Name("Блок с изменение языка перевода")
    @FindByCss(".cke_translate_header")
    TranslateHeaderBlock translateHeader();

    @Name("Поле перевода письма")
    @FindByCss(".cke_translate_content")
    MailElement translateText();

    @Name("Кнопка “Редактировать“")
    @FindByCss(".ns-view-compose-button-translate-apply .nb-button")
    MailElement editTranslateBtn();

    @Name("Кнопка “Отменить“")
    @FindByCss(".ns-view-compose-button-translate-close .nb-button")
    MailElement cancelTranslateBtn();

    @Name("Поле ввода текста письма с выключенным форматированием")
    @FindByCss(".cke_source")
    MailElement textAreaWithoutFormatting();

    @Name("Поле ввода текста с включенным форматированием")
    @FindByCss(".cke_wysiwyg_div")
    ComposePageFormattedTextBlock formattedText();

    @Name("Строки текста в композе")
    @FindByCss(".cke_wysiwyg_div div")
    ElementsCollection<ComposePageFormattedTextBlock> formattedTextLines();

    @Name("Первая строка подписи")
    @FindByCss(".cke_wysiwyg_div div:nth-of-type(3)")
    MailElement signatureFirstLine();

    @Name("Показать цитату")
    @FindByCss(".mail-Compose-Quote-Toggler")
    MailElement showQuote();

    @Name("Показать цитату в переводчике")
    @FindByCss(".cke_translate_content .mail-Compose-Quote-Toggler")
    MailElement showQuoteTranslate();
}
