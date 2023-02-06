package ru.yandex.market.api.util.parser2.typed.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.controller.annotations.RestrictedToClientType;
import ru.yandex.market.api.controller.annotations.VersionedEnum;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.parser2.Parser2;
import ru.yandex.market.api.util.parser2.ParserTestUtils;
import ru.yandex.market.api.util.parser2.resolver.typed.enums.MultiEnum2;

import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.PARAM_NAME;
import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.Parsers2;
import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.createRequest;
import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.setClient;
import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.setVersion;

public class MultiEnum2MultipleValuesParserTest extends UnitTestBase {

    public enum TestEnumOffer implements EnumInterface {
        A,
        B
    }

    public enum TestEnumShop implements EnumInterface {
        @VersionedEnum("2.0.0-*")
        C,
        @RestrictedToClientType({Client.Type.INTERNAL, Client.Type.EXTERNAL})
        D,
        @RestrictedToClientType({Client.Type.MOBILE})
        E,
        @VersionedEnum(value = "1.0.0-2.0.0", mandatory = "2.0.0-*")
        F
    }

    public interface EnumInterface {

    }

    private final Parser2<Collection<EnumInterface>> parser = Parsers2.multiEnumListParser(
        new MultiEnum2<EnumInterface>()
            .add(TestEnumOffer.class)
            .add("SHOP", TestEnumShop.class)
                .addAlias("EXTERNAL_ALIAS", Arrays.asList(TestEnumOffer.A, TestEnumShop.C))
    ).parameterName(PARAM_NAME)
        .build();

    private static String toString(EnumInterface value) {
        if (value instanceof TestEnumOffer) {
            return ((TestEnumOffer) value).name().toUpperCase();
        }
        return "SHOP_" + ((TestEnumShop) value).name().toUpperCase();
    }

    @Test
    public void dependsOnVersion() {
        setVersion(Version.V1_0_0);
        assertNotAllowed("SHOP_C", "A", "B", "D", "E", "ALL", "SHOP_ALL", "EXTERNAL_ALIAS");

        assertParsed("SHOP_D", TestEnumShop.D);

        setVersion(Version.V2_0_0);
        assertParsed("SHOP_C", TestEnumShop.C, TestEnumShop.F);
        assertParsed("SHOP_D", TestEnumShop.D, TestEnumShop.F);
    }

    @Test
    public void unknownValue_waitAllAllowedValues() {
        setVersion(Version.V2_0_0);
        assertNotAllowed("trash", "ALL", "A", "B", "SHOP_C", "SHOP_D", "SHOP_E", "SHOP_ALL", "ALL", "EXTERNAL_ALIAS");
    }

    @Test
    public void unknownValue_waitAllAllowedValuesWithRespectToVersion() {
        setVersion(Version.V1_0_0);
        assertNotAllowed("trash", "ALL", "A", "B", "SHOP_D", "SHOP_E", "SHOP_ALL", "ALL", "EXTERNAL_ALIAS");
    }

    @Test
    public void unknownValue_waitAllAllowedValuesWithRespectToClientType() {
        setVersion(Version.V1_0_0);
        setClient(Client.Type.MOBILE);
        assertNotAllowed("trash", "ALL", "A", "B", "SHOP_E", "SHOP_ALL", "ALL", "EXTERNAL_ALIAS");
    }

    @Test
    public void ignoreCase() {
        assertParsed("a", TestEnumOffer.A);
        assertParsed("shOp_d", TestEnumShop.D);
    }

    @Test
    public void multipleValues() {
        assertParsed("A,B", TestEnumOffer.A, TestEnumOffer.B);
    }

    @Test
    public void multipleValuesWithPrefixes() {
        assertParsed("A,SHOP_D,B", TestEnumShop.D, TestEnumOffer.B, TestEnumOffer.A);
    }

    @Test
    public void restrictedToClientType() {
        setVersion(Version.V2_0_0);
        setClient(Client.Type.MOBILE);
        assertNotAllowed(
            "SHOP_D",
            MultiEnum2MultipleValuesParserTest::toString,
            TestEnumOffer.A,
            TestEnumOffer.B,
            TestEnumShop.C,
            TestEnumShop.E);
        assertParsed("SHOP_C", TestEnumShop.C, TestEnumShop.F);
        assertParsed("SHOP_E", TestEnumShop.E, TestEnumShop.F);
    }

    @Test
    public void mandatory() {
        setVersion(Version.V1_0_0);
        assertParsed("A", TestEnumOffer.A);
        assertParsed("SHOP_F", TestEnumShop.F);
        setVersion(Version.V2_0_0);
        assertParsed("SHOP_C", TestEnumShop.C, TestEnumShop.F);
    }

    @Before
    public void setup() {
        setVersion(Version.V1_0_0);
        Parsers2 = Parsers2;
    }

    @Test
    public void singleValue() {

        assertParsed("A", TestEnumOffer.A);
    }

    @After
    public void tearDown() {
        ContextHolder.reset();
    }

    @Test
    public void withPrefix() {
        assertParsed("SHOP_D", TestEnumShop.D);
    }

    private void assertNotAllowed(String value, Function<EnumInterface, String> fn, EnumInterface... allowed) {
        Result<Collection<EnumInterface>, ValidationError> result = parser.get(createRequest(value));
        ParserTestUtils.assertNotAllowedValues(result, fn, allowed);
    }

    private void assertNotAllowed(String value, String... allowed) {
        Result<Collection<EnumInterface>, ValidationError> result = parser.get(createRequest(value));
        ParserTestUtils.assertNotAllowedValues(result, allowed);
    }

    private <T> void assertParsed(String value, EnumInterface... expected) {
        Result<Collection<EnumInterface>, ValidationError> result = parser.get(createRequest(value));
        ParserTestUtils.assertParsed(expected, result);
    }
}
