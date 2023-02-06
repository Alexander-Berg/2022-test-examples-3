package ru.yandex.market.sqb.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbRenderingException;
import ru.yandex.market.sqb.model.common.NameValueModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.sqb.test.TestUtils.checkConstructor;
import static ru.yandex.market.sqb.util.SqbRenderingUtils.DEFAULT_PREFIX;
import static ru.yandex.market.sqb.util.SqbRenderingUtils.DEFAULT_SUFFIX;
import static ru.yandex.market.sqb.util.SqbRenderingUtils.hasTemplateParam;

/**
 * Unit-тесты для {@link SqbRenderingUtils}.
 *
 * @author Vladislav Bauer
 */
class SqbRenderingUtilsTest {

    @Test
    void testConstructorContract() {
        checkConstructor(SqbRenderingUtils.class);
    }

    @Test
    void testTemplateParam() {
        final String paramName = "TEST";

        assertThat(hasTemplateParam(null, null), equalTo(false));
        assertThat(hasTemplateParam(StringUtils.EMPTY, StringUtils.EMPTY), equalTo(false));
        assertThat(hasTemplateParam(DEFAULT_PREFIX + DEFAULT_SUFFIX, StringUtils.EMPTY), equalTo(false));
        assertThat(hasTemplateParam(DEFAULT_PREFIX + paramName + DEFAULT_SUFFIX, paramName), equalTo(true));
    }

    @Test
    void testSubstitutePositive() {
        final String expected = "Hello, World";
        final String template = "Hello, ${NAME}";
        final Map<String, String> parameters = Collections.singletonMap("NAME", "World");

        checkSubstitute(expected, template, parameters);
    }

    @Test
    void testSubstituteRecursionPositive() {
        final String expected = "Hi, Guys";
        final String template = "Hi, ${name1}";
        final Map<String, String> parameters = ImmutableMap.<String, String>builder()
                .put("name1", "${name2}")
                .put("name2", "Guys")
                .build();

        checkSubstitute(expected, template, parameters);
    }

    @Test
    void testSubstituteRecursionNegative() {
        final String template = "Hello, ${NAME}";
        final Map<String, String> parameters = Collections.singletonMap("NAME", "${NAME}");
        Assertions.assertThrows(SqbRenderingException.class, () -> SqbRenderingUtils.substitute(template, parameters));
    }

    @Test
    void testToMap() {
        checkToMap("prefix_");
        checkToMap(StringUtils.EMPTY);
        checkToMap(null);
    }


    private void checkSubstitute(final String expected, final String template, final Map<String, String> parameters) {
        final String result = SqbRenderingUtils.substitute(template, parameters);

        assertThat(result, equalTo(expected));
    }

    private void checkToMap(final String prefix) {
        final int count = 10;
        final String keyPrefix = "KEY";
        final String valuePrefix = "value";

        final Collection<NameValueModel<String>> models = IntStream.rangeClosed(1, count)
                .mapToObj(index -> new NameValueModel<>(keyPrefix + index, valuePrefix + index))
                .collect(Collectors.toList());

        final Map<String, String> parameters = SqbRenderingUtils.toMap(models, prefix);
        for (int index = 1; index <= count; index++) {
            final String value = parameters.get(StringUtils.trimToEmpty(prefix) + keyPrefix + index);
            assertThat(value, equalTo(valuePrefix + index));
        }

        assertThat(parameters.size(), equalTo(count));
    }

}
