package ru.yandex.direct.grid.processing.service.showcondition.keywords;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.grid.core.entity.showcondition.repository.GridKeywordsParser;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdAddKeywordsOperatorsMode;
import ru.yandex.direct.grid.processing.service.showcondition.converter.KeywordOperatorsConverter;

import static org.assertj.core.api.Assertions.assertThat;

public class KeywordOperatorsConverterTest {

    private KeywordOperatorsConverter keywordOperatorsConverter;
    private GridKeywordsParser gridKeywordsParser;

    @Test
    public void testFormAndNumber() {
        keywordOperatorsConverter = new KeywordOperatorsConverter();
        gridKeywordsParser = new GridKeywordsParser();
        var keyword = gridKeywordsParser.parseKeyword("asd dasda das dad").getKeyword();
        var result1 = keywordOperatorsConverter.addOperators(keyword, List.of(GdAddKeywordsOperatorsMode.FORM, GdAddKeywordsOperatorsMode.NUMBER));
        var result2 = keywordOperatorsConverter.addOperators(keyword, List.of(GdAddKeywordsOperatorsMode.NUMBER, GdAddKeywordsOperatorsMode.FORM));

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.toString()).isEqualTo("\"!asd !dasda !das !dad\"");
    }

    @Test
    public void testOrder() {
        keywordOperatorsConverter = new KeywordOperatorsConverter();
        gridKeywordsParser = new GridKeywordsParser();
        var keyword = gridKeywordsParser.parseKeyword("asd dasda das dad").getKeyword();
        var result1 = keywordOperatorsConverter.addOperators(keyword, List.of(GdAddKeywordsOperatorsMode.ORDER));
        assertThat(result1.toString()).isEqualTo("[asd dasda das dad]");
    }
}
