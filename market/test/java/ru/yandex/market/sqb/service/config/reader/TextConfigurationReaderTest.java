package ru.yandex.market.sqb.service.config.reader;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.CONTENT;

/**
 * Unit-тесты для {@link TextConfigurationReader}.
 *
 * @author Vladislav Bauer
 */
class TextConfigurationReaderTest {

    @Test
    void testPositive() {
        final TextConfigurationReader reader = new TextConfigurationReader(CONTENT);
        final String content = reader.get();

        assertThat(content, equalTo(CONTENT));
        assertThat(reader.getContent(), equalTo(CONTENT));
    }

}
