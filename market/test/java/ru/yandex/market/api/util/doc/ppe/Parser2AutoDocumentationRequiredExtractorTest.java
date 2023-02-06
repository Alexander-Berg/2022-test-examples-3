package ru.yandex.market.api.util.doc.ppe;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.doc.ParamInfo;
import ru.yandex.market.api.doc.ParserGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.doc.ppe.parsers2.Parser2ParamsExtractor;
import ru.yandex.market.api.util.parser2.Parser2;
import ru.yandex.market.api.util.parser2.ParserWrapper2;
import ru.yandex.market.api.util.parser2.validation.Validators;

import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.Parsers2;

public class Parser2AutoDocumentationRequiredExtractorTest extends UnitTestBase {

    private static class NotRequiredParser extends ParserWrapper2<Integer> {
        public NotRequiredParser() {
            super(notRequired);
        }
    }

    private static class RequiredParser extends ParserWrapper2<Integer> {
        public RequiredParser() {
            super(required);
        }
    }

    private static final Parser2<Integer> required = Parsers2.integerParser()
        .addValidator(Validators.required())
        .parameterName("p")
        .build();
    private static final Parser2<Integer> notRequired = Parsers2.integerParser()
        .defaultValue(0)
        .parameterName("p")
        .build();
    private Parser2ParamsExtractor extractor = new Parser2ParamsExtractor();

    @Test
    public void notRequiredClass() throws Exception {
        assertRequired(false, extractor.extract(new NotRequiredParser()).getGroups().get(0));
    }

    @Test
    public void notRequiredInstance() throws Exception {
        assertRequired(false, extractor.extract(notRequired));
    }

    @Test
    public void requiredClass() throws Exception {
        assertRequired(true, extractor.extract(new RequiredParser()).getGroups().get(0));
    }

    @Test
    public void requiredInstance() throws Exception {
        assertRequired(true, extractor.extract(required));
    }

    private void assertRequired(boolean expected, ParserGroup group) {
        ParamInfo info = group.extractParam();
        Assert.assertNotNull(info);
        Object actual = info.isRequired();
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }
}
