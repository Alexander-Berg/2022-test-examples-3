package ru.yandex.market.api.util.parser2.typed.enums;

import java.util.Arrays;
import java.util.Collection;

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

public class MultiEnum2MultipleValuesAliasParserTest extends UnitTestBase {
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
        E
    }

    public interface EnumInterface {

    }

    private final Parser2<Collection<EnumInterface>> parser =
        Parsers2
            .multiEnumListParser(
                new MultiEnum2<EnumInterface>()
                    .add(TestEnumOffer.class)
                    .add("SHOP", TestEnumShop.class)
                    .addAlias(
                        "ABC",
                        Arrays.asList(
                            TestEnumOffer.A,
                            TestEnumOffer.B,
                            TestEnumShop.C
                        )
                    )
            )
            .parameterName(PARAM_NAME)
            .build();

    @Test
    public void allAlias() {
        assertParsed(
            Version.V1_0_0,
            Client.Type.MOBILE,
            "ALL",
            TestEnumOffer.A,
            TestEnumOffer.B,
            TestEnumShop.E
        );
    }

    @Test
    public void excludeAfterAll() {
        assertParsed(
            Version.V2_0_0,
            Client.Type.MOBILE,
            "ALL,-SHOP_D,-A",
            TestEnumOffer.B,
            TestEnumShop.C,
            TestEnumShop.E
        );
    }

    @Test
    public void excludeBeforeAll() {
        assertParsed(
            Version.V1_0_0,
            Client.Type.INTERNAL,
            "-SHOP_D,-A,ALL",
            TestEnumOffer.B
        );
    }

    @Test
    public void groupAlias() {
        assertParsed(
            Version.V2_0_0,
            Client.Type.INTERNAL,
            "SHOP_ALL",
            TestEnumShop.C,
            TestEnumShop.D
        );
    }

    @Test
    public void ignoreCaseAlias() {
        assertParsed(
            Version.V1_0_0,
            Client.Type.MOBILE,
            "aLl",
            TestEnumOffer.A,
            TestEnumOffer.B,
            TestEnumShop.E
        );
    }

    @Test
    public void enumAliasIgnoreVersionedEnum() {
        assertParsed(
            Version.V1_0_0,
            Client.Type.INTERNAL,
            "ABC",
            TestEnumOffer.A,
            TestEnumOffer.B
        );
    }

    @Test
    public void enumAliasParserVersionedEnum() {
        assertParsed(
            Version.V2_0_0,
            Client.Type.INTERNAL,
            "ABC",
            TestEnumOffer.A,
            TestEnumOffer.B,
            TestEnumShop.C
        );
    }

    @Test
    public void aliasIsInvalid() {
        Parser2<Collection<EnumInterface>> parser = Parsers2
                .multiEnumListParser(
                        new MultiEnum2<EnumInterface>()
                                .add(TestEnumOffer.class, Arrays.asList(TestEnumOffer.A))
                                .addAlias(
                                        "ABC",
                                        Arrays.asList(
                                                TestEnumOffer.A,
                                                TestEnumOffer.B,
                                                TestEnumShop.C
                                        )
                                )
                )
                .parameterName(PARAM_NAME)
                .build();
        setVersion(Version.V2_0_0);
        setClient(Client.Type.INTERNAL);
        ParserTestUtils.assertParsed(new EnumInterface[]{TestEnumOffer.A}, parser.get(createRequest("ABC")));
    }

    @Before
    public void setup() {
        setVersion(Version.V1_0_0);
    }

    @After
    public void tearDown() {
        ContextHolder.reset();
    }

    private <T> void assertParsed(Version version,
                                  Client.Type type,
                                  String value,
                                  EnumInterface... expected) {
        setVersion(version);
        setClient(type);
        Result<Collection<EnumInterface>, ValidationError> result = parser.get(createRequest(value));
        ParserTestUtils.assertParsed(expected, result);
    }
}
