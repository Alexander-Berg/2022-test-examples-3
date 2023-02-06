package ru.yandex.market.fmcg.bff.suggestion;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fmcg.bff.test.FmcgBffTest;
import ru.yandex.market.fmcg.bff.test.TestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuggestionIdExtractorTest extends FmcgBffTest {

    private SuggestionIdExtractor extractor = new SuggestionIdExtractor();

    @Test
    void getId_testOk() {
        String okJson = TestUtil.loadResourceAsString(
            "suggestion/SuggestionResponseOk.json");
        assertEquals("11916", extractor.getId(okJson));
    }

    @Test
    void getId_testEmpty() {
        String emptyJson = TestUtil.loadResourceAsString(
            "suggestion/SuggestionResponseEmpty.json");
        assertThrows(SuggestionExtractionException.class, () -> {
            extractor.getId(emptyJson);
        });
    }

}