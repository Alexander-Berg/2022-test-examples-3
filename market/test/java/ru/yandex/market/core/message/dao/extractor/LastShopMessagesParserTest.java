package ru.yandex.market.core.message.dao.extractor;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.market.core.message.dao.extractor.LastShopsMessagesExtractor.LastShopMessagesParser;
import ru.yandex.market.core.message.model.LastShopMessage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link LastShopMessagesParser}.
 *
 * @author avetokhin 17/01/17.
 */
public class LastShopMessagesParserTest {

    @Test
    public void testParse() {
        assertThat(LastShopMessagesParser.parse("1,55;2,66;3,232"), equalTo(
                Stream.of(new LastShopMessage(55, 1), new LastShopMessage(66, 2), new LastShopMessage(232, 3))
                        .collect(Collectors.toSet())
        ));
    }

}
