package ru.yandex.travel.tvm;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableBiMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.passport.tvmauth.Unittest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.passport.tvmauth.TicketStatus.EXPIRED;
import static ru.yandex.passport.tvmauth.TicketStatus.OK;

public class TvmWrapperImplTest {
    public static final String[] NO_SCOPES = new String[0];

    private Tvm2 tvm;
    private TvmWrapperImpl wrapper;

    @Before
    public void init() {
        tvm = Mockito.mock(Tvm2.class);
        wrapper = new TvmWrapperImpl(tvm, ImmutableBiMap.copyOf(Map.of("A", 1, "B", 2)), -1);
    }

    @Test
    public void checkServiceTicket_ok() {
        when(tvm.checkServiceTicket("ok_ticket")).thenReturn(Unittest.createServiceTicket(OK, 1));
        assertThat(wrapper.checkServiceTicket("ok_ticket", List.of("A"))).satisfies(check -> {
            assertThat(check.isOk()).isTrue();
            assertThat(check.getStatus()).isEqualTo(ServiceTicketCheckStatus.OK);
        });
    }

    @Test
    public void checkServiceTicket_missingTicket() {
        assertThat(wrapper.checkServiceTicket(null, List.of("A"))).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(ServiceTicketCheckStatus.MISSING_TICKET);
        });
        assertThat(wrapper.checkServiceTicket("", List.of("A"))).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(ServiceTicketCheckStatus.MISSING_TICKET);
        });
    }

    @Test
    public void checkServiceTicket_unexpectedError() {
        when(tvm.checkServiceTicket(any())).thenThrow(new RuntimeException("something went wrong"));
        assertThat(wrapper.checkServiceTicket("ticket", List.of("A"))).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(ServiceTicketCheckStatus.UNEXPECTED_ERROR);
        });
    }

    @Test
    public void checkServiceTicket_invalidTicket() {
        when(tvm.checkServiceTicket(any())).thenReturn(Unittest.createServiceTicket(EXPIRED, 1));
        assertThat(wrapper.checkServiceTicket("ticket", List.of("A"))).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(ServiceTicketCheckStatus.INVALID_TICKET);
        });
    }

    @Test
    public void checkServiceTicket_notAllowedSource() {
        when(tvm.checkServiceTicket(any())).thenReturn(Unittest.createServiceTicket(OK, 2));
        assertThat(wrapper.checkServiceTicket("ticket", List.of("A"))).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(ServiceTicketCheckStatus.NOT_ALLOWED_SOURCE);
        });
    }

    @Test
    public void checkUserTicket_ok() {
        when(tvm.checkUserTicket("ok_ticket")).thenReturn(Unittest.createUserTicket(OK, 1, NO_SCOPES, new long[]{1L}));
        assertThat(wrapper.checkUserTicket("ok_ticket", 1L)).satisfies(check -> {
            assertThat(check.isOk()).isTrue();
            assertThat(check.getStatus()).isEqualTo(UserTicketCheckStatus.OK);
            assertThat(check.getDefaultUid()).isEqualTo(1L);
        });
        assertThat(wrapper.checkUserTicket("ok_ticket", null)).satisfies(check -> {
            assertThat(check.isOk()).isTrue();
            assertThat(check.getStatus()).isEqualTo(UserTicketCheckStatus.OK);
            assertThat(check.getDefaultUid()).isEqualTo(1L);
        });
    }

    @Test
    public void checkUserTicket_missingTicket() {
        assertThat(wrapper.checkUserTicket(null, 1L)).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(UserTicketCheckStatus.MISSING_TICKET);
        });
        assertThat(wrapper.checkUserTicket("", 1L)).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(UserTicketCheckStatus.MISSING_TICKET);
        });
    }

    @Test
    public void checkUserTicket_invalidTicket() {
        when(tvm.checkUserTicket(any())).thenReturn(Unittest.createUserTicket(EXPIRED, 1, NO_SCOPES, new long[]{1L}));
        assertThat(wrapper.checkUserTicket("ticket", 1L)).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(UserTicketCheckStatus.INVALID_TICKET);
        });
    }

    @Test
    public void checkUserTicket_notAllowedSource() {
        when(tvm.checkUserTicket(any())).thenReturn(Unittest.createUserTicket(OK, 2, NO_SCOPES, new long[]{2L}));
        assertThat(wrapper.checkUserTicket("ticket", 1L)).satisfies(check -> {
            assertThat(check.isOk()).isFalse();
            assertThat(check.getStatus()).isEqualTo(UserTicketCheckStatus.PASSPORT_ID_MISMATCH);
        });
    }
}
