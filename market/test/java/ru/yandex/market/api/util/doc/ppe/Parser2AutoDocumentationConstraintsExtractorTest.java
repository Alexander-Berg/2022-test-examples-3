package ru.yandex.market.api.util.doc.ppe;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.doc.ParamInfo;
import ru.yandex.market.api.doc.ParserGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.doc.ppe.parsers2.Parser2ParamsExtractor;
import ru.yandex.market.api.util.parser2.ParserWrapper2;
import ru.yandex.market.api.util.parser2.validation.Validators;

import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.Parsers2;

public class Parser2AutoDocumentationConstraintsExtractorTest extends UnitTestBase {
    private static class ParserWithRangeConstraint extends ParserWrapper2<Integer> {
        public ParserWithRangeConstraint() {
            super(Parsers2.integerParser()
                .defaultValue(0)
                .addValidator(Validators.range(1, 10))
                .parameterName("p")
                .build());
        }
    }

    private Parser2ParamsExtractor extractor = new Parser2ParamsExtractor();

    @Test
    public void rangeConstraint() throws Exception {
        ParserGroup res = extractor.extract(new ParserWithRangeConstraint());
        Assert.assertEquals(1, res.getParamSize());
        ParamInfo infoItem = res.extractParam();
        List<String> constraints = infoItem.getConstraints();
        Assert.assertEquals(1, constraints.size());
        String constraint = constraints.get(0);
        Assert.assertTrue(constraint.contains("1"));
        Assert.assertTrue(constraint.contains("10"));
    }
}
