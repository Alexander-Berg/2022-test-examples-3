package ru.yandex.market.api.internal.suggest;


import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.domain.v2.suggest.Completion;
import ru.yandex.market.api.domain.v2.suggest.HighlightRange;
import ru.yandex.market.api.domain.v2.suggest.PageSuggestion;
import ru.yandex.market.api.domain.v2.suggest.Suggestions;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * Created by tesseract on 26.01.17.
 */
public class WordSuggestParserTest extends ContainerTestBase {
    @Inject
    private MarketUrls marketUrls;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ContextHolder.update(ctx -> ctx.setUrlSchema(UrlSchema.HTTPS));
    }

    /**
     * Проверяем правильность обработки ответа от сервиса саджестов
     */
    @Test
    public void checkGreenParser() {
        Suggestions suggestions = new WordSuggestParser(marketUrls::touch, false)
            .parse(ResourceHelpers.getResource("canon.json"));

        Assert.assertNotNull("Должны получить поисковые подсказки", suggestions);

        List<Completion> completions = suggestions.getCompletions();
        Assert.assertNotNull("Должны получить список окончаний фразы", completions);
        Assert.assertEquals("Кол-во окончаний фраз должно соответствовать кол-ву из файла", 4, completions.size());

        // Формат ответа для тача
        Completion completion = completions.get(0);
        Assert.assertEquals("eos", completion.getCompletion());
        Assert.assertEquals("canon eos", completion.getValue());

        // Формат ответа для мобильного приложения
        completion = completions.get(1);
        Assert.assertEquals("pixma", completion.getCompletion());
        Assert.assertEquals("canon pixma", completion.getValue());

        List<PageSuggestion> pages = suggestions.getPages();
        Assert.assertNotNull("Должны получить список страниц", pages);
        Assert.assertEquals("Кол-во страниц должно соответствовать кол-ву из файла", 5, pages.size());

        PageSuggestion page = pages.get(0);
        Assert.assertEquals("https://m.market.yandex.ru/product/10710502?hid=91148&suggest_text=Canon%20EOS%201200D%20Kit&suggest=1&suggest_type=model", page.getUrl());
        Assert.assertEquals("canon eos 1200d kit", page.getValue());

        Assert.assertEquals("canon", suggestions.getInput().getValue());
        Assert.assertNull("Должны получить null т.к. для canon нет страницы приземления", suggestions.getInput().getUrl());
    }

    /**
     * Проверяем правильность получения inputUrl
     */
    @Test
    public void checkInput() {
        Suggestions suggestions = new WordSuggestParser(marketUrls::touch, false)
            .parse(ResourceHelpers.getResource("canonEos1200dKit.json"));

        Assert.assertNotNull("Должны получить поисковые подсказки", suggestions);

        PageSuggestion input = suggestions.getInput();
        Assert.assertEquals("canon eos 1200d kit", input.getValue());
        Assert.assertEquals("https://m.market.yandex.ru/product/10710502?hid=91148&suggest_text=Canon%20EOS%201200D%20Kit&suggest=1&suggest_type=model", input.getUrl());
    }

    @Test
    public void checkBlueParse() {
        Suggestions suggestions = new WordSuggestParser(marketUrls::blueTouch, false)
            .parse(ResourceHelpers.getResource("canon-blue.json"));

        HighlightRange expectedRange = new HighlightRange();
        expectedRange.setBegin(0);
        expectedRange.setEnd(5);
        HighlightRange actualRange = suggestions.getPages().get(0).getHighlights().get(0);

        Assert.assertNotNull("Должны получить поисковые подсказки", suggestions);
        Assert.assertEquals("https://m.pokupki.market.yandex.ru/product/10710502?hid=91148&suggest_text=Canon%20EOS%201200D%20Kit&suggest=1&suggest_type=model", suggestions.getPages().get(0).getUrl());
        Assert.assertEquals(suggestions.getPages().get(0).getHighlights().size(), 1);
        Assert.assertEquals(expectedRange.getBegin(), actualRange.getBegin());
        Assert.assertEquals(expectedRange.getEnd(), actualRange.getEnd());
    }

    @Test
    public void checkBlueParseWithLogo() {
        //TODO выпилить update contextа, когды выпилим флаг
        ContextHolder.update(ctx -> {
            ctx.setGenericParams(
                    new GenericParamsBuilder()
                            .setWithSuggestLogos(true)
                    .build()
            );
        });

        Suggestions suggestions = new WordSuggestParser(marketUrls::blueTouch, false)
            .parse(ResourceHelpers.getResource("with-logo-blue.json"));


        Assert.assertNotNull("Должны получить поисковые подсказки", suggestions);
        Assert.assertEquals("https://avatars.mds.yandex.net/get-mpic/195452/img_id696209164757851690/orig", suggestions.getPages().get(0).getLogo());
        Assert.assertEquals("vendor", suggestions.getPages().get(0).getType());
    }

    @Test
    public void checkParseNullData() {
        Suggestions suggestions = new WordSuggestParser(marketUrls::blueTouch, false)
            .parse(ResourceHelpers.getResource("with-null-data.json"));

        Assert.assertNotNull("Должны получить поисковые подсказки", suggestions);
    }

    @Test
    public void checkBlueParseWithLogoIgnore() {
        Suggestions suggestions = new WordSuggestParser(marketUrls::blueTouch, false)
                .parse(ResourceHelpers.getResource("with-logo-blue-ignore.json"));

        Assert.assertNotNull("Должны получить поисковые подсказки", suggestions);
        Assert.assertNull("https://avatars.mds.yandex.net/get-mpic/195452/img_id696209164757851690/orig", suggestions.getPages().get(0).getLogo());
        Assert.assertEquals("vendor", suggestions.getPages().get(0).getType());
    }

}
