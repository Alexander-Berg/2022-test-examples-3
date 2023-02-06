package ru.yandex.market.api.internal.common;

import org.springframework.stereotype.Component;
import ru.yandex.market.api.util.parser.ParserWrapper;
import ru.yandex.market.api.util.parser.Parsers;

/**
 * Created by yntv on 11/25/16.
 */
@Component
class TestComponentWithPublicParser {
    private static class TestPublicParser extends ParserWrapper<Boolean> {
        public TestPublicParser() {
            super(Parsers.booleanParser()
                .parameterName("test")
                .build());
        }
    }
}
