package ru.yandex.travel.orders;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ErrorException;
import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.orders.proto.TUserInfo;

import static ru.yandex.travel.orders.services.OrderOwnerValidator.validateIfLoggedIn;

public class OrderOwnerValidatorTest {
    @Test
    public void testValidateLoginOk() {
        validateIfLoggedIn(owner("login1"), credentials("login1"));
        validateIfLoggedIn(owner(""), credentials(""));
        validateIfLoggedIn(owner(""), credentials(null));
    }

    @Test
    public void testValidateLoginBroken() {
        // null values aren't supported in proto DTOs
        Assertions.assertThatThrownBy(() -> validateIfLoggedIn(owner(null), credentials(null)))
                .isExactlyInstanceOf(NullPointerException.class);

        Assertions.assertThatThrownBy(() -> validateIfLoggedIn(owner("a"), credentials("b")))
                .isExactlyInstanceOf(ErrorException.class)
                .hasMessageContaining("login mismatch");

        Assertions.assertThatThrownBy(() -> validateIfLoggedIn(owner(""), credentials("b")))
                .isExactlyInstanceOf(ErrorException.class)
                .hasMessageContaining("login mismatch");

        Assertions.assertThatThrownBy(() -> validateIfLoggedIn(owner("a"), credentials("")))
                .isExactlyInstanceOf(ErrorException.class)
                .hasMessageContaining("login mismatch");

        Assertions.assertThatThrownBy(() -> validateIfLoggedIn(owner("a"), credentials(null)))
                .isExactlyInstanceOf(ErrorException.class)
                .hasMessageContaining("login mismatch");
    }

    private TUserInfo owner(String login) {
        return TUserInfo.newBuilder()
                .setLogin(login)
                .setPassportId("passport1")
                .build();
    }

    private UserCredentials credentials(String login) {
        return new UserCredentials(null, null, "passport1", login, null, "127.0.0.1", false, false);
    }
}
