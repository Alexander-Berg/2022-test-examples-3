package ru.yandex.market.api.util.doc.ppe;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.doc.ParamInfo;
import ru.yandex.market.api.doc.ParserGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.doc.ppe.parsers2.Parser2ParamsExtractor;
import ru.yandex.market.api.util.parser2.ParserWrapper2;

import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.Parsers2;

public class Parser2AutoDocumentationDefaultValueExtractorTest extends UnitTestBase {
    private static class ParserWithRangeConstraint extends ParserWrapper2<Integer> {
        public ParserWithRangeConstraint() {
            super(Parsers2.integerParser()
                .defaultValue(123)
                .parameterName("p")
                .build());
        }
    }

    private Parser2ParamsExtractor extractor = new Parser2ParamsExtractor();

    @Test
    public void defaultValue() throws Exception {
        ParserGroup group = extractor.extract(new ParserWithRangeConstraint());
        Assert.assertEquals(1, group.getParamSize());
        ParamInfo infoItem = group.extractParam();
        String defaultValue = infoItem.getDefaultValue();
        Assert.assertTrue("123".compareTo(defaultValue) == 0);
    }
}
