package ru.yandex.market.api.partner.controllers.feed.model;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.util.DateTimes;

/**
 * Тесты для модели {@FeedIndexLogRecordDTO}.
 */
class FeedIndexLogRecordDTOTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("parseYmlDateTimeData")
    void testParseYmlDateTime(final String timeString) {
        var offsetDateTime = FeedIndexLogRecordDTO.parseYmlDateTime(DateTimes.MOSCOW_TIME_ZONE, timeString);
        Assertions.assertNotNull(offsetDateTime);
    }

    private static Stream<Arguments> parseYmlDateTimeData() {
        return List.of(
                "20200924 08:29:53",
                "20200924 08:29",
                "2020-09-24 08:29:53.123",
                "2020-09-24 08:29:53",
                "2020-09-24 08:29",
                "2020-09-24T08:29:53"
        ).stream().map(Arguments::of);
    }

}
