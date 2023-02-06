package ru.yandex.market.api.internal.common;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.MethodParameter;
import ru.yandex.market.api.common.From;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.parser2.Parser2;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */

@WithMocks
public class CustomParserStorage2Test extends UnitTestBase {

    private static class UseParser2Impl implements UseParser2 {

        private Class<? extends Parser2> parserClass;

        public UseParser2Impl(Class<? extends Parser2> parserClass) {
            this.parserClass = parserClass;
        }

        @Override
        public Class<? extends Parser2> value() {
            return parserClass;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return UseParser2.class;
        }
    }

    public static class IntegerParser implements Parser2<Integer> {
        @Override
        public Result<Integer, ValidationError> get(HttpServletRequest request) {
            return Result.newResult(1);
        }
    }

    @InjectMocks
    private CustomParserStorage2 storage;

    @Mock
    private ObjectCreator objectCreator;

    @Before
    public void setUp() throws Exception {
        when(objectCreator.instantiate(any())).thenCallRealMethod();
    }

    public void stub() {
    }

    @Test
    public void shouldReturnSimpleParser() throws Exception {
        Parser2 parser = storage.getParser(createMethodParameter(
            this.getClass().getMethod("stub"),
            "test",
            int.class,
            new Annotation[]{new UseParser2Impl(IntegerParser.class)}
            )
        );

        assertTrue(parser instanceof IntegerParser);
    }


    @Test
    public void shouldReturnFromCache() throws Exception {
        MethodParameter methodParameter = createMethodParameter(
            this.getClass().getMethod("stub"),
            "test",
            int.class,
            new Annotation[]{new UseParser2Impl(IntegerParser.class)}
        );

        storage.getParser(methodParameter);
        storage.getParser(methodParameter);

        verify(objectCreator, times(1)).instantiate(eq(IntegerParser.class));
    }

    /**
     * MethodParameter достаточно сложная структура, которая собирается спрингом, поэтому собираем mock объект
     *
     * @param method    ссылка на метод
     * @param paramName имя параметра
     * @return Mock-объект
     */
    private static MethodParameter createMethodParameter(Method method,
                                                         String paramName,
                                                         Class paramType,
                                                         Annotation[] annotations) {
        MethodParameter mock = mock(MethodParameter.class);

        when(mock.getMethod()).thenReturn(method);
        when(mock.getParameterName()).thenReturn(paramName);
        when(mock.getParameterAnnotations()).thenReturn(annotations);
        when(mock.getParameterType()).thenReturn(paramType);

        return mock;
    }
}
