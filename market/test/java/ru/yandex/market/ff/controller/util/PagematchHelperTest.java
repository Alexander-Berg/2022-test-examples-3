package ru.yandex.market.ff.controller.util;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

class PagematchHelperTest {

    private static final String MODULE_NAME = "test-module";

    private static Stream<Arguments> params() {
        return Stream.of(
                Arguments.of("/ping", expectedString("ping", "/ping")),
                Arguments.of("/path/{id}/path2", expectedString("path_*_path2", "/path/<>/path2")),
                Arguments.of("/path/{id}/path2/", expectedString("path_*_path2", "/path/<>/path2/"))
        );
    }

    @ParameterizedTest
    @MethodSource("params")
    void testMappingToPagematchConversion(String requestTemplate, String expectedString) {
        String actual = PagematchHelper.toPagematcherLine(requestTemplate, MODULE_NAME);

        assertThat(actual, equalTo(expectedString));
    }

    private static String expectedString(String id, String pattern) {
        return id + "\t" + pattern + "\t" + MODULE_NAME;
    }
}
