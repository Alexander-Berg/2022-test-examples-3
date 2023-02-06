package ru.yandex.market.tpl.core.domain.clientreturn;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestClientReturnFactory;


import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.partner.DeliveryService.DEFAULT_DS_ID;

@RequiredArgsConstructor
class ClientReturnQueryServiceTest extends TplAbstractTest {
    public static final int EXPECTED_CLIENT_RETURN_NUMBER = 20;
    public static final int NOT_EXPECTED_DS_ID = 1234;
    public static final int EXPECTED_CLIENT_RETURNS_IN_WINDOW_CORNER = 3;
    private static final int EXPECTED_CLIENT_RETURNS_IN_WINDOW = 7;
    private final ClientReturnQueryService clientReturnQueryService;
    private final TestClientReturnFactory clientReturnFactory;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final Clock clock;

    @AfterEach
    void tearDown() {
        Mockito.reset(configurationProviderAdapter);
    }

    @Test
    void getAllForRoutingRequest() {
        //given
        LocalDateTime expectedDate = LocalDateTime.now(clock);
        var expectedClientReturns = IntStream.range(0, EXPECTED_CLIENT_RETURN_NUMBER)
                .mapToObj(i -> clientReturnFactory.buildAndSave(DEFAULT_DS_ID, expectedDate))
                .collect(Collectors.toList());

        IntStream.range(0, 10)
                .forEach(i -> clientReturnFactory.buildAndSave(NOT_EXPECTED_DS_ID, expectedDate));

        IntStream.range(0, 10)
                .forEach(i -> clientReturnFactory.buildAndSave(DEFAULT_DS_ID, expectedDate.minusDays(1)));


        //when
        var actualClientReturns = clientReturnQueryService.getAllForRoutingRequest(expectedDate.toLocalDate(),
                Set.of(DEFAULT_DS_ID));

        //then
        assertThat(actualClientReturns).containsExactlyElementsOf(expectedClientReturns);
    }


    @Test
    void getAllForRoutingRequest_withPastPeriod() {
        //given
        long PAST_WINDOW_SIZE = 5L;
        Mockito.when(configurationProviderAdapter.getValueAsLong(
                ConfigurationProperties.ROUTING_NOT_ARRIVED_CLIENT_RETURN_DAYS_IN_PAST)).thenReturn(Optional.of(PAST_WINDOW_SIZE));

        LocalDateTime expectedDate = LocalDateTime.now(clock);

        var expectedClientReturns = new ArrayList<ClientReturn>();
        IntStream.range(0, EXPECTED_CLIENT_RETURN_NUMBER)
                .mapToObj(i -> clientReturnFactory.buildAndSave(DEFAULT_DS_ID, expectedDate))
                .collect(Collectors.toCollection(() -> expectedClientReturns));
        IntStream.range(0, EXPECTED_CLIENT_RETURNS_IN_WINDOW_CORNER)
                .mapToObj(i -> clientReturnFactory.buildAndSave(DEFAULT_DS_ID, expectedDate.minusDays(PAST_WINDOW_SIZE)))
                .collect(Collectors.toCollection(() -> expectedClientReturns));
        IntStream.range(0, EXPECTED_CLIENT_RETURNS_IN_WINDOW)
                .mapToObj(i -> clientReturnFactory.buildAndSave(DEFAULT_DS_ID, expectedDate.minusDays(3)))
                .collect(Collectors.toCollection(() -> expectedClientReturns));

        //this ClientReturns not in collect window period or not appropriate DsId
        IntStream.range(0, 10)
                .forEach(i -> clientReturnFactory.buildAndSave(NOT_EXPECTED_DS_ID, expectedDate));
        IntStream.range(0, 10)
                .forEach(i -> clientReturnFactory.buildAndSave(DEFAULT_DS_ID, expectedDate.minusDays(PAST_WINDOW_SIZE + 1)));


        //when
        var actualClientReturns = clientReturnQueryService.getAllForRoutingRequest(expectedDate.toLocalDate(),
                Set.of(DEFAULT_DS_ID));

        //then
        assertThat(actualClientReturns).containsExactlyElementsOf(expectedClientReturns);
    }

    @Test
    void getWithItems() {
        LocalDateTime expectedDate = LocalDateTime.now(clock);

        ClientReturn clientReturn = clientReturnFactory.buildAndSave(DEFAULT_DS_ID, expectedDate);

        assertThat(clientReturnQueryService.getClientReturnWithItems(clientReturn.getExternalReturnId()))
                .isPresent();
    }
}
