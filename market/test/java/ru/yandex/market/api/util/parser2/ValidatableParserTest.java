package ru.yandex.market.api.util.parser2;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.util.parser2.postprocess.ParsedValuePostProcessor;
import ru.yandex.market.api.util.parser2.validation.GreaterEqualsValidator;
import ru.yandex.market.api.util.parser2.validation.ParsedValueValidator;
import ru.yandex.market.api.util.parser2.validation.RangeValueValidator;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ValidatableParserTest {

    @Test
    public void shouldCallPostProcesor() throws Exception {

        ParsedValuePostProcessor<Integer, Object> processor = mock(ParsedValuePostProcessor.class);

        Parser2<Integer> parser = ParserTestUtils.integerParser()
            .parameterName("test")
            .addPostProcessor(processor)
            .build();

        HttpServletRequest request = MockRequestBuilder.start().param("test", "1").build();

        Result<Integer, ValidationError> result = parser.get(request);

        assertTrue(result.isOk());
        verify(processor, times(1)).process(anyInt(), anyObject());
    }

    @Test
    public void shouldReturnCompositeError() throws Exception {

        Parser2<Integer> parser = ParserTestUtils.integerParser()
            .parameterName("test")
            .required()
            .addValidator(new RangeValueValidator<>(0, 1))
            .build();

        HttpServletRequest request = MockRequestBuilder.start().build();

        Result<Integer, ValidationError> result = parser.get(request);

        assertThat(result.hasError(), is(true));
        assertThat(result.getError(), isA(ValidationError.class));
    }

    @Test
    public void shouldValidateByEachFromComposite() throws Exception {
        ParsedValueValidator<Integer, Object> validator1 = mock(ParsedValueValidator.class);
        ParsedValueValidator<Integer, Object> validator2 = mock(ParsedValueValidator.class);

        Parser2<Integer> parser = ParserTestUtils.integerParser()
            .parameterName("test")
            .addValidator(validator1)
            .addValidator(validator2)
            .build();

        HttpServletRequest request = MockRequestBuilder.start().param("test", "1").build();

        Result<Integer, ValidationError> result = parser.get(request);

        assertThat(result.isOk(), is(true));
        verify(validator1, times(1)).validate(Maybe.just(1), null);
        verify(validator2, times(1)).validate(Maybe.just(1), null);
    }

    @Test
    public void shouldExpandGetValidationErrorsMessage() throws Exception {
        Parser2<Integer> parser = ParserTestUtils.integerParser()
            .parameterName("test")
            .addValidator(new GreaterEqualsValidator<>(1))
            .addValidator(new GreaterEqualsValidator<>(2))
            .build();

        HttpServletRequest request = MockRequestBuilder.start().param("test", "0").build();

        Result<Integer, ValidationError> result = parser.get(request);
        assertEquals(new Integer(0), result.getValue());
        assertThat(
            result.getError().getMessage(),
            allOf(
                containsString("Parameter does not fit range constraint (actual value = 0, min value = 1)"),
                containsString("Parameter does not fit range constraint (actual value = 0, min value = 2)")
            )
        );
    }
}
