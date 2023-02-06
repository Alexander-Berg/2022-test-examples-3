package ru.yandex.market.mbo.toolkit.shared.templates;

import org.assertj.core.api.ListAssert;
import org.junit.Test;
import ru.yandex.market.mbo.toolkit.shared.models.ValueType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 20.04.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class TemplateBlockParserTest {

    @Test
    public void extractParameterValues() {
        assertThatParsed("s145").containsExactly(val(ValueType.PARAM_VALUES, 145));
        assertThatParsed("[s145]").containsExactly("[", val(ValueType.PARAM_VALUES, 145), "]");
    }

    @Test
    public void preserveFunctions() {
        assertThatParsed("Math.cos(2)").containsExactly("Math.cos(2)");
        assertThatParsed("Math.cos(2)").containsExactly("Math.cos(2)");
    }

    @Test
    public void preserveUserFunction() {
        assertThatParsed("join(46)").containsExactly("join(46)");
        assertThatParsed("[join(46)]").containsExactly("[join(46)]");
    }

    @Test
    public void preserveUserFunctionWithValues() {
        assertThatParsed("[s17, contains(s14, o99, o41)]").containsExactly(
            "[",
            val(ValueType.PARAM_VALUES, 17),
            ", contains(",
            val(ValueType.PARAM_VALUES, 14),
            ",", // trims spaces
            val(ValueType.OPTION, 99),
            ",",
            val(ValueType.OPTION, 41),
            ")]"
        );
    }

    @Test
    public void dontInifiniteRecursionOnWrong() {
        assertThatParsed("o").containsExactly("o");
    }

    private static ListAssert<Object> assertThatParsed(String pattern) {
        CollectCallback callback = new CollectCallback();
        TemplateBlockParser.parsePattern(pattern, callback);
        return assertThat(callback.getNodes());
    }

    private static Value val(ValueType type, int value) {
        return new Value(type, value);
    }

    private static class CollectCallback implements TemplateBlockParser.Callback {
        private final List<Object> nodes = new ArrayList<>();

        @Override
        public void value(ValueType type, int value) {
            nodes.add(new Value(type, value));
        }

        @Override
        public void text(String text) {
            nodes.add(text);
        }

        public List<Object> getNodes() {
            return nodes;
        }
    }

    private static class Value {
        private final ValueType type;
        private final int value;

        private Value(ValueType type, int value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Value value1 = (Value) o;
            return value == value1.value &&
                type == value1.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }

        @Override
        public String toString() {
            return type + "=" + value;
        }
    }
}
