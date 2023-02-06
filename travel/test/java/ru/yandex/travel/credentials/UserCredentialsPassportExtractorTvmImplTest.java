package ru.yandex.travel.credentials;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.tvm.TvmWrapper;
import ru.yandex.travel.tvm.UserTicketCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.tvm.UserTicketCheckStatus.INVALID_TICKET;

public class UserCredentialsPassportExtractorTvmImplTest {
    private TvmWrapper tvm;
    private UserCredentialsPassportExtractorTvmImpl validator;

    @Before
    public void init() {
        tvm = Mockito.mock(TvmWrapper.class);
        validator = new UserCredentialsPassportExtractorTvmImpl(tvm);
    }

    @Test
    public void extractPassportId() {
        when(tvm.checkUserTicket(any(), any())).thenReturn(UserTicketCheck.invalid(INVALID_TICKET, "bad ticket"));
        assertThat(validator.extractPassportId("", "ticket")).isNull();

        when(tvm.checkUserTicket(any(), any())).thenReturn(UserTicketCheck.valid(123L));
        assertThat(validator.extractPassportId(null, "ticket")).isEqualTo("123");

        when(tvm.checkUserTicket(any(), any())).thenReturn(UserTicketCheck.valid(123L));
        assertThat(validator.extractPassportId("234", "ticket")).isEqualTo("234");
    }
}
