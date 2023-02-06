package ru.yandex.market.core.util;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;

@ExtendWith(MockitoExtension.class)
class CheckouterUtilsTest {

    private static final int BATCH_SIZE = 5;
    private static final long LAST_EVENT_ID = 0;

    @Mock
    private CheckouterAPI checkouterAPIMock;
    private RetryTemplate retryTemplate;
    @Mock
    private CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApiMock;

    @BeforeEach
    void setup() {
        Mockito.when(checkouterAPIMock.orderHistoryEvents())
                .thenReturn(checkouterOrderHistoryEventsApiMock);
        retryTemplate = new RetryTemplate();
    }

    public static Stream<Arguments> testArgs() {
        return Stream.of(
                Arguments.of(
                        "Получение событий",
                        List.of(1L, 2L, 3L, 4L, 5L),
                        List.of()
                ),
                Arguments.of(
                        "Получение только архивных событий",
                        List.of(6L, 7L, 8L, 9L, 10L),
                        List.of(1L, 2L, 3L, 4L, 5L)
                ),
                Arguments.of(
                        "Получение архивных событий #1",
                        List.of(1L, 3L, 5L, 6L, 7L),
                        List.of(2L, 4L)
                ),
                Arguments.of(
                        "Получение архивных событий #2",
                        List.of(1L, 2L, 9L, 10L, 11L),
                        List.of(3L, 4L, 5L, 6L, 7L)
                ),
                Arguments.of(
                        "Получение пропусков #1",
                        List.of(1L, 3L, 9L, 10L, 11L),
                        List.of(2L, 4L, 5L, 6L, 7L)
                ),
                Arguments.of(
                        "Получение пропусков #2",
                        List.of(1L, 4L, 5L, 10L, 11L),
                        List.of(2L, 3L, 6L, 7L, 8L)
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testArgs")
    void testGetEvents(
            String description,
            List<Long> eventIds,
            List<Long> archivedEventIds
    ) {
        var events = makeEvent(eventIds);
        var archivedEvents = makeEvent(archivedEventIds);
        mockGetEvents(events, archivedEvents);

        var actual = CheckouterUtils
                .getOrderHistoryEvents(checkouterAPIMock, retryTemplate, LAST_EVENT_ID, BATCH_SIZE);
        assertIdEquals(actual);
    }

    private void assertIdEquals(Collection<OrderHistoryEvent> actual) {
        Assertions.assertArrayEquals(
                new long[]{1L, 2L, 3L, 4L, 5L},
                actual.stream().mapToLong(OrderHistoryEvent::getId).toArray()
        );
    }

    private void mockGetEvents(OrderHistoryEvents events, OrderHistoryEvents archivedEvents) {
        mockGetEvents(events, false);
        mockGetEvents(archivedEvents, true);

        Mockito.doReturn(archivedEvents.getContent().size())
                .when(checkouterOrderHistoryEventsApiMock)
                .getOrderHistoryEventsCount(
                        Mockito.argThat(arg ->
                                arg.isArchived() &&
                                        arg.getFirstEventId() == LAST_EVENT_ID &&
                                        arg.getLastEventId() == events.getContent().stream()
                                                .mapToLong(OrderHistoryEvent::getId)
                                                .max()
                                                .getAsLong()
                        )
                );
    }

    private void mockGetEvents(OrderHistoryEvents events, boolean archived) {
        Mockito.doReturn(events)
                .when(checkouterOrderHistoryEventsApiMock)
                .getHistoryEvents(
                        Mockito.argThat(clientInfo ->
                                clientInfo.getClientRole().equals(ClientRole.SYSTEM) &&
                                        clientInfo.getClientId() == null
                        ),
                        Mockito.argThat(request -> request.isArchived() == archived)
                );
    }

    private static OrderHistoryEvents makeEvent(List<Long> ids) {
        return new OrderHistoryEvents(ids.stream()
                .map(id -> {
                    var event = new OrderHistoryEvent();
                    event.setId(id);
                    return event;
                }).collect(Collectors.toList())
        );
    }
}
