package ru.yandex.calendar.tvm;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import lombok.SneakyThrows;
import lombok.val;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.tvm.exceptions.EmptyTvmTicketException;
import ru.yandex.calendar.tvm.exceptions.InvalidTvmTicketException;
import ru.yandex.calendar.tvm.exceptions.UnexpectedTvmServiceIdException;
import ru.yandex.calendar.tvm.exceptions.UnexpectedUidException;
import ru.yandex.passport.tvmauth.TicketStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.calendar.logic.event.ActionSource.INTERNAL_API;
import static ru.yandex.calendar.logic.event.ActionSource.WEB;
import static ru.yandex.calendar.tvm.TvmTicketType.SERVICE;
import static ru.yandex.calendar.tvm.TvmTicketType.USER;

@ContextConfiguration(classes = CalendarTvmTestConfiguration.class)
@ExtendWith({SpringExtension.class})
public class TvmManagerTest {
    static final int SRC = 3;
    static final int ANOTHER_SRC = 4;
    private static final String VALID_TICKET = "VALID_TICKET";
    private static final String INVALID_TICKET = "INVALID_TICKET";
    private static final String ANOTHER_VALID_TICKET = "VALID_TICKET_WITH_RESTRICTED_PERMISSIONS";
    private static final long VALID_USER = 1;
    private static final long INVALID_USER = 2;
    private static final TvmServiceResponse OK_SERVICE = new TvmServiceResponse(TicketStatus.OK, Optional.of(SRC));
    private static final TvmServiceResponse ANOTHER_SERVICE = new TvmServiceResponse(
            TicketStatus.OK,
            Optional.of(ANOTHER_SRC));
    private static final TvmUserResponse MALFORMED_USER = new TvmUserResponse(TicketStatus.MALFORMED, new long[0]);
    private static final TvmServiceResponse MALFORMED_SERVICE = new TvmServiceResponse(
            TicketStatus.MALFORMED,
            Optional.empty());
    private static final TvmUserResponse OK_USER = new TvmUserResponse(TicketStatus.OK, new long[]{VALID_USER});

    @Inject
    private TvmManager tvmManager;
    @Inject
    private TvmClient tvmClient;
    @Inject
    private TvmFirewall tvmFirewall;

    @BeforeEach
    public void setUp() {
        reset(tvmClient);
        when(tvmClient.checkServiceTicket(VALID_TICKET)).thenReturn(OK_SERVICE);
        when(tvmClient.checkServiceTicket(ANOTHER_VALID_TICKET)).thenReturn(ANOTHER_SERVICE);
        when(tvmClient.checkUserTicket(VALID_TICKET)).thenReturn(OK_USER);
        when(tvmClient.checkUserTicket(INVALID_TICKET)).thenReturn(MALFORMED_USER);
        when(tvmClient.checkServiceTicket(INVALID_TICKET)).thenReturn(MALFORMED_SERVICE);

        reset(tvmFirewall);
        when(tvmFirewall.isActionAllowedWithoutServiceTicket(any(ActionSource.class))).thenReturn(false);
        when(tvmFirewall.isActionAllowedWithoutServiceTicket(INTERNAL_API)).thenReturn(true);

        when(tvmFirewall.isActionAllowedWithoutUserTicket(anyInt())).thenReturn(false);
        when(tvmFirewall.isActionAllowedWithoutUserTicket(SRC)).thenReturn(true);

        when(tvmFirewall.isSourceAllowed(anyInt(), any(ActionSource.class))).thenReturn(false);
        when(tvmFirewall.isSourceAllowed(SRC, INTERNAL_API)).thenReturn(true);
        when(tvmFirewall.isSourceAllowed(ANOTHER_SRC, INTERNAL_API)).thenReturn(true);
        when(tvmFirewall.isSourceAllowed(SRC, WEB)).thenReturn(true);
    }

    private static Stream<Arguments> goodRequestsWithTVM() {
        return Stream.of(
                Arguments.of(Optional.of(VALID_TICKET), Optional.of(VALID_TICKET), Optional.of(VALID_USER), INTERNAL_API),
                Arguments.of(Optional.of(VALID_TICKET), Optional.empty(), Optional.of(VALID_USER), INTERNAL_API),
                Arguments.of(Optional.of(VALID_TICKET), Optional.empty(), Optional.empty(), INTERNAL_API),
                Arguments.of(Optional.of(VALID_TICKET), Optional.of(VALID_TICKET), Optional.of(VALID_USER), WEB),
                Arguments.of(Optional.empty(), Optional.of(VALID_TICKET), Optional.of(VALID_USER), INTERNAL_API)
        );
    }

    private static Stream<Arguments> requestsWithInvalidTVM() {
        return Stream.of(
                Arguments.of(Optional.of(VALID_TICKET), Optional.of(VALID_TICKET), Optional.of(INVALID_USER), INTERNAL_API, UnexpectedUidException.class),
                Arguments.of(Optional.of(VALID_TICKET), Optional.of(INVALID_TICKET), Optional.of(VALID_USER), INTERNAL_API, InvalidTvmTicketException.class),
                Arguments.of(Optional.of(ANOTHER_VALID_TICKET), Optional.empty(), Optional.of(VALID_USER), INTERNAL_API, EmptyTvmTicketException.class),
                Arguments.of(Optional.of(ANOTHER_VALID_TICKET), Optional.of(VALID_TICKET), Optional.of(VALID_USER), WEB, UnexpectedTvmServiceIdException.class),
                Arguments.of(Optional.of(INVALID_TICKET), Optional.of(VALID_TICKET), Optional.of(VALID_USER), INTERNAL_API, InvalidTvmTicketException.class),
                Arguments.of(Optional.empty(), Optional.of(VALID_TICKET), Optional.of(VALID_USER), WEB, EmptyTvmTicketException.class)
        );
    }

    @ParameterizedTest(name = "{index}. {4} (serviceTicket={0}, userTicket={1}, uid={2}, actionSource={3})")
    @DisplayName("TVM authorization bad cases")
    @MethodSource("requestsWithInvalidTVM")
    public void checkWithException(Optional<String> serviceTicket, Optional<String> userTicket,
                                   Optional<Long> uid, ActionSource actionSource,
                                   Class<? extends Exception> exceptionType) {
        assertThatThrownBy(() -> check(serviceTicket, userTicket, uid, actionSource))
                .isInstanceOf(exceptionType);
    }

    @ParameterizedTest(name = "{index}. serviceTicket={0}, userTicket={1}, uid={2}, actionSource={3}")
    @DisplayName("TVM authorization good cases")
    @MethodSource("goodRequestsWithTVM")
    @SneakyThrows
    public void check(Optional<String> serviceTicket, Optional<String> userTicket, Optional<Long> uid, ActionSource actionSource) {
        val headers = EntryStream.of(SERVICE, serviceTicket, USER, userTicket)
                .flatMapValues(Optional::stream)
                .mapKeys(TvmTicketType::getHeader)
                .append("X-Real-Ip", "::1")
                .toImmutableMap();

        tvmManager.checkTickets(uid, arg -> Optional.ofNullable(headers.get(arg)), actionSource);

        serviceTicket.ifPresentOrElse(
                ticket -> verify(tvmClient).checkServiceTicket(ticket),
                () -> verify(tvmClient, never()).checkServiceTicket(anyString()));

        if (serviceTicket.isEmpty() || userTicket.isEmpty() || uid.isEmpty()) {
            verify(tvmClient, never()).checkUserTicket(anyString());
        } else {
            verify(tvmClient).checkUserTicket(userTicket.get());
        }
    }
}
