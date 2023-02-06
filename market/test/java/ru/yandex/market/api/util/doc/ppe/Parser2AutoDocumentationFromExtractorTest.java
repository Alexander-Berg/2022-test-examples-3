package ru.yandex.market.api.util.doc.ppe;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.common.From;
import ru.yandex.market.api.doc.NameFrom;
import ru.yandex.market.api.doc.ParamInfo;
import ru.yandex.market.api.doc.ParserGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.doc.ppe.parsers2.Parser2ParamsExtractor;
import ru.yandex.market.api.util.parser2.ParserWrapper2;
import ru.yandex.market.api.util.parser2.validation.Validators;

import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.Parsers2;

public class Parser2AutoDocumentationFromExtractorTest extends UnitTestBase {
    private static class SampleParser extends ParserWrapper2<Integer> {
        public SampleParser() {
            super(Parsers2.integerParser()
                .addValidator(Validators.required())
                .header("X-Header")
                .parameterName("queryStringParameterName")
                .pathVariable("pathVariableName")
                .build());
        }
    }

    private Parser2ParamsExtractor extractor = new Parser2ParamsExtractor();

    @Test
    public void multipleSources() throws Exception {
        ParserGroup group = extractor.extract(new SampleParser());
        Assert.assertEquals(1, group.getParamSize());
        ParamInfo paramInfo = group.extractParam();
        Assert.assertEquals("X-Header", parameterName(From.HEADER, paramInfo));
        Assert.assertEquals("queryStringParameterName", parameterName(From.QUERY_STRING, paramInfo));
        Assert.assertEquals("pathVariableName", parameterName(From.PATH, paramInfo));
    }

    private String parameterName(From expected, ParamInfo info) {
        Optional<NameFrom> from1 = info.getNameFromTuples().stream()
            .filter(x -> {
                Object from = x.getFrom();
                if (from == null) {
                    return false;
                }
                return expected.equals(from);
            }).findFirst();
        Assert.assertTrue(from1.isPresent());
        return (String) from1.get().getName();
    }
}
