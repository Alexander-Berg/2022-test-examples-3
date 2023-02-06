package ru.yandex.market.api.util.parser2;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ParserBuilderTest {

    private static final String TEST_PARAM = "test";

    private static final int DEFAULT_VALUE = 1;

    @Test
    public void shouldResolveStandardParam() throws Exception {
        Parser2<Integer> parser =
                ParserTestUtils.integerParser().parameterName(TEST_PARAM).defaultValue(DEFAULT_VALUE).build();

        HttpServletRequest request = MockRequestBuilder.start().param(TEST_PARAM, "123").build();

        ParserTestUtils.assertParsed(123, parser.get(request));
    }

    @Test
    public void shouldResolveNullAsDefault() throws Exception {
        Parser2<Integer> parser = ParserTestUtils.integerParser().parameterName(TEST_PARAM).defaultValue(DEFAULT_VALUE).build();

        HttpServletRequest request = MockRequestBuilder.start().build();

        ParserTestUtils.assertParsed(DEFAULT_VALUE, parser.get(request));
    }

    @Test
    public void shouldResolveEmptyStringAsDefault() throws Exception {
        Parser2<Integer> parser = ParserTestUtils.integerParser().parameterName(TEST_PARAM).defaultValue(DEFAULT_VALUE).build();

        HttpServletRequest request = MockRequestBuilder.start().param(TEST_PARAM, "").build();
        ParserTestUtils.assertParsed(DEFAULT_VALUE, parser.get(request));
    }
}
