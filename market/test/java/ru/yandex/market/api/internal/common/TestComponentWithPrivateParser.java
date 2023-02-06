package ru.yandex.market.api.internal.common;

import org.springframework.stereotype.Component;
import ru.yandex.market.api.util.parser.ParserWrapper;
import ru.yandex.market.api.util.parser.Parsers;

@Component
public class TestComponentWithPrivateParser {

    private static class TestPrivateParser extends ParserWrapper<Integer> {
        public TestPrivateParser() {
            super(Parsers.integerParser()
                .parameterName("test")
                .build());
        }
    }

    private static class TestPrivateParserWithoutDefaultCtor extends ParserWrapper<Integer> implements TestComponentGetter {
        private final TestComponent component;

        public TestComponent getTestComponent() {
            return component;
        }

        public TestPrivateParserWithoutDefaultCtor(TestComponent component) {
            super(Parsers.integerParser()
                .parameterName("test")
                .build());
            this.component = component;

        }
    }

    public interface TestComponentGetter {
        TestComponent getTestComponent();
    }

    @Component
    public static class TestComponent {
        public String foo() {
            return "bar";
        }
    }
}

