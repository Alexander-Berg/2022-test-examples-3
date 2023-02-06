package ru.yandex.market.ff.tms.util;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextRequestIdAppenderTest {

    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("1572167040200", 123L, "1572167040200/123"),

            Arguments.of("1572167040200/3010f7943c83f19d6d4e81293c09307e", 123L,
                "1572167040200/3010f7943c83f19d6d4e81293c09307e/123"),

            Arguments.of("1572167040200/3010f7943c83f19d6d4e81293c09307e/111", 123L, "" +
                "1572167040200/3010f7943c83f19d6d4e81293c09307e/123"),

            Arguments.of("1572167040200/3010f7943c83f19d6d4e81293c09307e/111/2", 123L, "" +
                "1572167040200/3010f7943c83f19d6d4e81293c09307e/111/123")

        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void replace(String currentRequestId, Long newShopRequestId, String expectedNewRequestId) {
        String newRequestId =
            ContextRequestIdAppender.appendOrReplaceLastShopRequestId(newShopRequestId, currentRequestId);
        assertEquals(expectedNewRequestId, newRequestId);
    }
}
