package ru.yandex.market.logistic.gateway.controller;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

class TvmConfigurationTest extends AbstractIntegrationTest {

    @Autowired
    private TvmTicketChecker tvmTicketChecker;

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] {0}")
    @MethodSource
    void testUrls(String url, boolean needCheck) {
        assertEquals(tvmTicketChecker.isRequestedUrlAcceptable(url), needCheck);
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> testUrls() {
        return Stream.of(
            Arguments.of("/partner", true),
            Arguments.of("/internal/support", true),
            Arguments.of("/su/pport", true),
            Arguments.of("/admin/mdb/ping", true),
            Arguments.of("/qu/eue/statistic", true)
        );
    }
}
