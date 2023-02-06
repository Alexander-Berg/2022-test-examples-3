package ru.yandex.market.api.util.parser2.typed.enums;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.controller.annotations.RestrictedToClientType;
import ru.yandex.market.api.controller.annotations.VersionedEnum;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.parser2.Parser2;
import ru.yandex.market.api.util.parser2.ParserTestUtils;

import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.PARAM_NAME;
import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.Parsers2;
import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.createRequest;
import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.setClient;
import static ru.yandex.market.api.util.parser2.typed.enums.EnumParserTestUtils.setVersion;

public class EnumValuesParserTest extends UnitTestBase {

    public enum TestEnum {
        A,

        @VersionedEnum("2.0.0-*")
        B,

        @RestrictedToClientType(Client.Type.EXTERNAL)
        C,

        @RestrictedToClientType({Client.Type.MOBILE, Client.Type.EXTERNAL})
        D,

        E
    }

    private final Parser2<TestEnum> parser = Parsers2.enumParser(TestEnum.class, Arrays.asList(
            new TestEnum[]{TestEnum.A,
                    TestEnum.B,
                    TestEnum.C,
                    TestEnum.D}))
            .parameterName(PARAM_NAME)
            .build();

    @Test
    public void dependsOnVersion() {
        setVersion(Version.V2_0_0);
        assertParsed("A", TestEnum.A);
        assertParsed("B", TestEnum.B);
        setVersion(Version.V1_0_0);
        assertParsed("A", TestEnum.A);
        assertNotAllowed("B", TestEnum.A, TestEnum.C, TestEnum.D);
    }

    @Test
    public void ignoreCase() {
        assertParsed("a", TestEnum.A);
    }

    @Test
    public void restrictedToClient() {
        setVersion(Version.V2_0_0);
        setClient(Client.Type.EXTERNAL);
        assertParsed("A", TestEnum.A);
        assertParsed("C", TestEnum.C);
        assertParsed("D", TestEnum.D);
        setClient(Client.Type.MOBILE);
        assertParsed("D", TestEnum.D);
        assertNotAllowed("C", TestEnum.A, TestEnum.B);
        assertNotAllowed("E", TestEnum.A, TestEnum.B);
    }
    @Before
    public void setup() {
        setVersion(Version.V1_0_0);
    }

    @Test
    public void simple() {
        assertParsed("A", TestEnum.A);
    }

    @After
    public void tearDown() {
        ContextHolder.reset();
    }

    private void assertNotAllowed(String value, TestEnum... allowed) {
        ParserTestUtils.assertNotAllowedValues(parser.get(createRequest(value)), allowed);
    }

    private void assertParsed(String value, TestEnum expected) {
        ParserTestUtils.assertParsed(expected, parser.get(createRequest(value)));
    }
}
