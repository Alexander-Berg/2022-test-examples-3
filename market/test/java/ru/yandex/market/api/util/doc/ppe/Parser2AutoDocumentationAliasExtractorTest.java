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

public class Parser2AutoDocumentationAliasExtractorTest extends UnitTestBase {
    public enum TestEnumOffer implements EnumInterface {
        A,
        B
    }

    public enum TestEnumShop implements EnumInterface {
        C,
        D,
        E
    }

    public enum TestEnumModel implements EnumInterface {
        F,
        G,
        H
    }

    public interface EnumInterface {

    }

    private static class MultiEnumParser extends ParserWrapper2<Collection<EnumInterface>> {
        public MultiEnumParser() {
            super(Parsers2.multiEnumListParser(new MultiEnum2<EnumInterface>()
                .add(TestEnumOffer.class)
                .add("SHOP", TestEnumShop.class)
                .add("MODEL", TestEnumModel.class))
                .parameterName("p")
                .build());
        }
    }

    private static class ParserWithSimpleAlias extends ParserWrapper2<Integer> {
        public ParserWithSimpleAlias() {
            super(Parsers2.integerParser()
                .alias("HOHOHO", 123)
                .parameterName("p")
                .build());
        }
    }

    private Parser2ParamsExtractor extractor = new Parser2ParamsExtractor();

    @Test
    public void multiEnumAliases() throws Exception {
        ParserGroup group = extractor.extract(new MultiEnumParser());
        Assert.assertEquals(1, group.getParamSize());
        ParamInfo infoItem = group.extractParam();
        Map<String, String> aliases = infoItem.getAliases();
        Assert.assertEquals(3, aliases.size());
        Assert.assertEquals("Все значения", aliases.get("ALL"));
        Assert.assertEquals("C, D, E", aliases.get("SHOP_ALL"));
        Assert.assertEquals("F, G, H", aliases.get("MODEL_ALL"));
    }

    @Test
    public void simpleAlias() throws Exception {
        ParserGroup group = extractor.extract(new ParserWithSimpleAlias());
        Assert.assertEquals(1, group.getParamSize());
        ParamInfo infoItem = group.extractParam();
        Map<String, String> aliases = infoItem.getAliases();
        Assert.assertEquals(1, aliases.size());
        Assert.assertEquals("123", aliases.get("HOHOHO"));
    }
}
