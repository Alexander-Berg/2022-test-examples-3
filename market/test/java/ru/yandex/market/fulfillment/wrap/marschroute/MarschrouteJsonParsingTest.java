package ru.yandex.market.fulfillment.wrap.marschroute;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.core.ParsingTest;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.client.MarschrouteClientConfiguration;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarschrouteClientConfiguration.class)
public abstract class MarschrouteJsonParsingTest<T> extends ParsingTest<T> {

    @Autowired
    private ObjectMapper objectMapper;

    protected MarschrouteJsonParsingTest(Class<T> type, String fileName) {
        super(null, type, fileName);
    }

    @Override
    protected ObjectMapper getMapper() {
        return objectMapper;
    }
}
