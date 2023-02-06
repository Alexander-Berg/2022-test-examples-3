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

public class UserCredentialsAuthValidatorTvmImplTest {
    private TvmWrapper tvm;
    private UserCredentialsAuthValidatorTvmImpl validator;

    @Before
    public void init() {
        tvm = Mockito.mock(TvmWrapper.class);
        validator = new UserCredentialsAuthValidatorTvmImpl(tvm);
    }

    @Test
    public void testNoUserTicketValidation() {
        assertThat(validator.validate(null, null, null).isOk()).isTrue();
        assertThat(validator.validate("", "", null).isOk()).isTrue();
        assertThat(validator.validate("pid", null, null)).satisfies(r -> {
            assertThat(r.isOk()).isFalse();
            assertThat(r.getDescription()).isEqualTo("Missing user ticket");
        });
        assertThat(validator.validate(null, "login", null)).satisfies(r -> {
            assertThat(r.isOk()).isFalse();
            assertThat(r.getDescription()).isEqualTo("Missing user ticket");
        });
    }

    @Test
    public void testMalformedPassportIdUserTicketValidation() {
        assertThat(validator.validate("asd", null, "ticket")).satisfies(r -> {
            assertThat(r.isOk()).isFalse();
            assertThat(r.getDescription()).isEqualTo("Invalid passport id");
        });
    }

    @Test
    public void testTicketValidation() {
        when(tvm.checkUserTicket(any(), any())).thenReturn(UserTicketCheck.invalid(INVALID_TICKET, "bad ticket"));
        assertThat(validator.validate("123", null, "ticket")).satisfies(r -> {
            assertThat(r.isOk()).isFalse();
            assertThat(r.getDescription()).isEqualTo("bad ticket");
        });

        when(tvm.checkUserTicket(any(), any())).thenReturn(UserTicketCheck.valid(null));
        assertThat(validator.validate("123", null, "ticket").isOk()).isTrue();
    }
}
