package ru.yandex.market.api.util.parser2.composite;

import javax.servlet.http.HttpServletRequest;

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
public class CompositeParserBuilderTest {
    private static final CompositeParserBuilder<Pack.Mutable, Pack.Entity> BUILDER_PARAM1 =
            new CompositeParserBuilder<>(Pack.Mutable::new, Pack.Mutable::build)
                    .addField(Pack.Mutable::setParam1,
                            ParserTestUtils.integerParser().parameterName(Pack.PARAM1).build());

    private static final CompositeParserBuilder<Pack.Mutable, Pack.Entity> BUILDER =
            new CompositeParserBuilder<>(Pack.Mutable::new, Pack.Mutable::build)
                    .addField(Pack.Mutable::setParam1,
                            ParserTestUtils.integerParser().parameterName(Pack.PARAM1).build())
                    .addField(Pack.Mutable::setParam2,
                            ParserTestUtils.integerParser().parameterName(Pack.PARAM2).build());

    private static final HttpServletRequest REQUEST = MockRequestBuilder.start()
        .param(Pack.PARAM1, "2")
        .param(Pack.PARAM2, "1")
        .build();

    @Test
    public void parserBuilderExtractorsImmutable() {
        Parser2<Pack.Entity> fullParser = BUILDER_PARAM1
                .addField(Pack.Mutable::setParam2, ParserTestUtils.integerParser().parameterName(Pack.PARAM2).build())
            .build();
        Result<Pack.Entity, ValidationError> fullResult = fullParser.get(REQUEST);

        ParserTestUtils.assertParsed(new Pack.Entity(2, 1), fullResult);

        Parser2<Pack.Entity> onlyF1Parser = BUILDER_PARAM1.build();
        Result<Pack.Entity, ValidationError> onlyF1Result = onlyF1Parser.get(REQUEST);

        //Парсер для поля f2 не должен был проставится - билдер содержит персистентные структуры
        ParserTestUtils.assertParsed(new Pack.Entity(2, 0), onlyF1Result);
    }

    @Test
    public void parserBuilderBuildImmutable() {
        Parser2<Pack.Entity> swapParser = BUILDER
            .builder(b -> new Pack.Entity(b.getParam2(), b.getParam1()))
            .build();
        Result<Pack.Entity, ValidationError> swapResult = swapParser.get(REQUEST);

        ParserTestUtils.assertParsed(new Pack.Entity(1, 2), swapResult);

        Parser2<Pack.Entity> normalParser = BUILDER.build();
        Result<Pack.Entity, ValidationError> normalResult = normalParser.get(REQUEST);

        //Изменение билдинга объекта не должно произойти - билдер содержит персистентные структуры
        ParserTestUtils.assertParsed(new Pack.Entity(2, 1), normalResult);
    }

    @Test
    public void parserBuilderValidatorsImmutable() {
        Parser2<Pack.Entity> validateParser = BUILDER_PARAM1
            .addValidator(new Pack.FirstLessSecondValidator())
            .build();
        Result<Pack.Entity, ValidationError> validateResult = validateParser.get(REQUEST);

        ParserTestUtils.assertError(Pack.TEMPLATE, validateResult);

        Parser2<Pack.Entity> hasNoValidateParser = BUILDER.build();
        Result<Pack.Entity, ValidationError> hasNoValidateResult = hasNoValidateParser.get(REQUEST);

        //Валидатор не должен был проставится - билдер содержит персистентные структуры
        ParserTestUtils.assertParsed(new Pack.Entity(2, 1), hasNoValidateResult);
    }


}
