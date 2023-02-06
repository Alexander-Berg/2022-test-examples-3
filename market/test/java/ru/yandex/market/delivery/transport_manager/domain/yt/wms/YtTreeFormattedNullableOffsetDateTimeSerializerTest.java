package ru.yandex.market.delivery.transport_manager.domain.yt.wms;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yson.YsonConsumer;

class YtTreeFormattedNullableOffsetDateTimeSerializerTest {

    public static final OffsetDateTime DATE_TIME =
        OffsetDateTime.of(2022, 4, 3, 20, 1, 4, 0, ZoneOffset.UTC);
    public static final String DATE_TIME_STR = "2022-04-03 20:01:04";
    private YtTreeFormattedNullableOffsetDateTimeSerializer dateTimeSerializer;

    @BeforeEach
    void setUp() {
        dateTimeSerializer = new YtTreeFormattedNullableOffsetDateTimeSerializer();
    }

    @Test
    void serialize() {
        YsonConsumer ysonConsumer = Mockito.mock(YsonConsumer.class);
        dateTimeSerializer.serialize(DATE_TIME, ysonConsumer);
        Mockito.verify(ysonConsumer).onString(Mockito.eq(DATE_TIME_STR));
    }

    @ParameterizedTest
    @MethodSource("dateRepresentations")
    void deserialize(String dateTimeStr) {
        YTreeNode node = Mockito.mock(YTreeNode.class);
        Mockito.when(node.stringValue()).thenReturn(dateTimeStr);
        Assertions.assertThat(dateTimeSerializer.deserialize(node)).isEqualTo(DATE_TIME);
    }

    @Test
    void deserializeNull() {
        YTreeNode node = Mockito.mock(YTreeNode.class);
        Mockito.when(node.stringValue()).thenReturn(null);
        Assertions.assertThat(dateTimeSerializer.deserialize(node)).isNull();
    }

    static Stream<Arguments> dateRepresentations() {
        return Stream.of(
            Arguments.of("2022-04-03T20:01:04"),
            Arguments.of("2022-04-03 20:01:04")
        );
    }
}
