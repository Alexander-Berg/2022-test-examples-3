package ru.yandex.travel.credentials;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserCredentialsValidatorTest {
    private UserCredentialsBuilder builder = new UserCredentialsBuilder();
    private UserCredentialsValidator validator = new UserCredentialsValidator(new UserCredentialsAuthValidatorStubImpl());

    @Test
    public void testBuildWithValidParams() {
        UserCredentials uc = builder.build("sid", "yaUid", "pid", "login", "ticket", "ip", false, false);
        assertThat(uc.getSessionKey()).isEqualTo("sid");
        assertThat(uc.getYandexUid()).isEqualTo("yaUid");
        assertThat(uc.getPassportId()).isEqualTo("pid");
        assertThat(uc.getLogin()).isEqualTo("login");
        assertThat(uc.getUserTicket()).isEqualTo("ticket");
        assertThat(uc.getUserIp()).isEqualTo("ip");
        validator.validate(uc);
    }

    @Test
    public void testBuildWithMinimalParams() {
        UserCredentials uc = builder.build("sid", "yaUid", null, null, null, null, false, false);
        assertThat(uc.getSessionKey()).isEqualTo("sid");
        assertThat(uc.getYandexUid()).isEqualTo("yaUid");
        assertThat(uc.getPassportId()).isNull();
        assertThat(uc.getLogin()).isNull();
        assertThat(uc.getUserTicket()).isNull();
        assertThat(uc.getUserIp()).isEqualTo("127.0.0.1");
        validator.validate(uc);
    }

    @Test
    public void testBuildWithoutYaUid() {
        UserCredentials uc = builder.build("sid", "", "pid", "login", "ticket", "ip", false, false);
        assertThatThrownBy(() -> validator.validate(uc))
                .isExactlyInstanceOf(BadUserCredentialsException.class)
                .hasMessage("Missing yandex uid");
    }

    @Test
    public void testBuildWithoutSidOrPid() {
        UserCredentials uc = builder.build("", "yaUid", "", "login", "ticket", "ip", false, false);
        assertThatThrownBy(() -> validator.validate(uc))
                .isExactlyInstanceOf(BadUserCredentialsException.class)
                .hasMessage("Either passport id or session key must be present");
    }
}
