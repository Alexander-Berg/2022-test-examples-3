package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;

import ru.yandex.market.api.domain.v2.SearchProcessingOptions;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ural Yulmukhametov <a href="mailto:ural@yandex-team.ru"></a>
 * @date 04.07.2019
 */
public class SearchProcessingOptionsJsonParserTest extends UnitTestBase {

    @Test
    public void shouldParse() {
        SearchProcessingOptionsJsonParser parser = new SearchProcessingOptionsJsonParser();
        SearchProcessingOptions options = parser.parse(ResourceHelpers.getResource("search-processing-options.json"));

        assertEquals("красное платjье ASOS", options.getText());
        assertEquals("красное платье ASOS", options.getActualText());
        assertEquals("красное пла[ть]е ASOS", options.getHighlightedText());
        assertTrue(options.isCheckSpelled());

        assertTrue(options.getAdult());
        assertTrue(options.getRestrictionAge18());
    }
}
