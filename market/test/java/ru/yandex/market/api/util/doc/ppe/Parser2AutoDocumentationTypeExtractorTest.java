package ru.yandex.market.api.util.doc.ppe;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.doc.ParamInfo;
import ru.yandex.market.api.doc.ParamType;
import ru.yandex.market.api.doc.ParserGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.doc.ppe.parsers2.Parser2ParamsExtractor;
import ru.yandex.market.api.util.parser2.Parser2;
import ru.yandex.market.api.util.parser2.ParserWrapper2;
import ru.yandex.market.api.util.parser2.validation.Validators;

import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.Parsers2;

public class Parser2AutoDocumentationTypeExtractorTest extends UnitTestBase {
    private static class SampleParser extends ParserWrapper2<Integer> {
        public SampleParser() {
            super(sampleParser);
        }
    }

    private static final Parser2<Integer> sampleParser = Parsers2.integerParser()
        .addValidator(Validators.required())
        .parameterName("p")
        .build();

    private Parser2ParamsExtractor extractor = new Parser2ParamsExtractor();

    @Test
    public void sampleClass() throws Exception {
        assertType(extractor.extract(new SampleParser()).getGroups().get(0));
    }

    @Test
    public void sampleInstance() throws Exception {
        assertType(extractor.extract(sampleParser));
    }

    private void assertType(ParserGroup group) {
        ParamInfo info = group.extractParam();
        Assert.assertNotNull(info);
        Object actual = info.getType();
        Assert.assertNotNull(actual);
        Assert.assertEquals(ParamType.INTEGER, actual);
    }
}
