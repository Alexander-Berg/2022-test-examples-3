package ru.yandex.market.api.util.doc.ppe;

import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.doc.ParamInfo;
import ru.yandex.market.api.doc.ParserGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.doc.ppe.parsers2.Parser2ParamsExtractor;
import ru.yandex.market.api.util.parser2.ParserWrapper2;
import ru.yandex.market.api.util.parser2.resolver.typed.enums.MultiEnum2;

import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.Parsers2;

public class Parser2AutoDocumentationEnumValuesExtractorTest extends UnitTestBase {
    public enum TestEnumOffer implements EnumInterface {
        A,
        B
    }

    public enum TestEnumShop implements EnumInterface {
        C,
        D,
        E
    }

    public interface EnumInterface {

    }

    private static class MultiEnumListParser extends ParserWrapper2<Collection<EnumInterface>> {
        public MultiEnumListParser() {
            super(Parsers2.multiEnumListParser(new MultiEnum2<EnumInterface>()
                .add(TestEnumOffer.class)
                .add("SHOP", TestEnumShop.class))
                .parameterName("p")
                .build());
        }
    }

    private static class EnumParser extends ParserWrapper2<TestEnumOffer> {

        public EnumParser() {
            super(Parsers2.enumParser(TestEnumOffer.class)
                .parameterName("p")
                .build());
        }
    }

    private Parser2ParamsExtractor extractor = new Parser2ParamsExtractor();

    @Test
    public void simpleEnumValues() throws Exception {
        ParserGroup group = extractor.extract(new EnumParser());
        Assert.assertEquals(1, group.getParamSize());
        ParamInfo infoItem = group.extractParam();
        Map<String, Object> enumValues = infoItem.getEnumValues();
        Assert.assertEquals(2, enumValues.size());
        Assert.assertEquals(TestEnumOffer.A, enumValues.get("A"));
        Assert.assertEquals(TestEnumOffer.B, enumValues.get("B"));
    }

    @Test
    public void multiEnumListValues() throws Exception {
        ParserGroup group = extractor.extract(new MultiEnumListParser());
        Assert.assertEquals(1, group.getParamSize());
        ParamInfo infoItem = group.extractParam();
        Map<String, Object> enumValues = infoItem.getEnumValues();
        Assert.assertEquals(5, enumValues.size());
        Assert.assertEquals(TestEnumOffer.A, enumValues.get("A"));
        Assert.assertEquals(TestEnumOffer.B, enumValues.get("B"));
        Assert.assertEquals(TestEnumShop.C, enumValues.get("SHOP_C"));
        Assert.assertEquals(TestEnumShop.D, enumValues.get("SHOP_D"));
        Assert.assertEquals(TestEnumShop.E, enumValues.get("SHOP_E"));
    }
}
