package ru.yandex.market.ff.service;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enrichment.OldMovementSupplyRequestItemsEnricher;
import ru.yandex.market.ff.enrichment.OldMovementWithdrawRequestItemsEnricher;
import ru.yandex.market.ff.enrichment.RequestItemsEnricher;
import ru.yandex.market.ff.enrichment.RequestItemsEnricherProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestItemsEntricherProviderTest extends IntegrationTest {

    @Autowired
    private List<RequestItemsEnricher> enrichers;

    private RequestItemsEnricherProvider provider;

    private static Stream<Arguments> supportedParams() {
        return Stream.of(
                Arguments.of(RequestType.MOVEMENT_SUPPLY, OldMovementSupplyRequestItemsEnricher.class),
                Arguments.of(RequestType.MOVEMENT_WITHDRAW, OldMovementWithdrawRequestItemsEnricher.class)
        );
    }

    @BeforeEach
    void init() {
        provider = new RequestItemsEnricherProvider(enrichers);
    }

    @ParameterizedTest
    @MethodSource("supportedParams")
    void providerParameterizedTest(RequestType requestType,
                                   Class<?> expectedProvidedClass) {
        RequestItemsEnricher providedEnricher = provider.provide(requestType);

        assertEquals(expectedProvidedClass, providedEnricher.getClass());
    }
}
