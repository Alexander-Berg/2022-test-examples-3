package ru.yandex.market.api.util.parser2.composite;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.util.parser2.CompositeParserBuilder;
import ru.yandex.market.api.util.parser2.Parser2;
import ru.yandex.market.api.util.parser2.ParserTestUtils;

/**
 * @author dimkarp93
 */
public class CompositeParserTest {
    private static final Parser2<Pack.Entity> SIMPLE_F1_LESS_F2_PARSER =
            new CompositeParserBuilder<>(Pack.Mutable::new, Pack.Mutable::build)
                    .addField(Pack.Mutable::setParam1,
                            ParserTestUtils.integerParser().parameterName(Pack.PARAM1).build())
                    .addField(Pack.Mutable::setParam2,
                            ParserTestUtils.integerParser().parameterName(Pack.PARAM2).build())
        .addValidator(new Pack.FirstLessSecondValidator())
        .build();

    @Test
    public void simpleParserValidationSuccessCase() {
        String[][] params = new String[][] {
            {Pack.PARAM1, "1"},
            {Pack.PARAM2, "3"}
        };

        Result<Pack.Entity, ValidationError> result = SIMPLE_F1_LESS_F2_PARSER.get(makeRequest(params));
        ParserTestUtils.assertParsed(new Pack.Entity(1, 3), result);
    }

    @Test
    public void simpleParserValidationNotSuccessCase() {
        String[][] params = new String[][]{
            {Pack.PARAM1, "3"},
            {Pack.PARAM2, "1"}
        };

        Result<Pack.Entity, ValidationError> result = SIMPLE_F1_LESS_F2_PARSER.get(makeRequest(params));
        ParserTestUtils.assertError(Pack.TEMPLATE, result);
    }


    private HttpServletRequest makeRequest(@NotNull String[][] params) {
        MockRequestBuilder builder = MockRequestBuilder.start();
        Arrays.stream(params).forEach(p -> builder.param(p[0], p[1]));
        return builder.build();
    }

}
